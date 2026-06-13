package com.sportspulse.india.features.venues.data

import com.sportspulse.india.core.data.api.DistanceMatrixService
import com.sportspulse.india.core.data.api.PlacesApiService
import com.sportspulse.india.core.data.db.dao.VenueDao
import com.sportspulse.india.core.data.mapper.EntityMapper.toDomain
import com.sportspulse.india.core.data.mapper.EntityMapper.toEntity
import com.sportspulse.india.core.data.mapper.PlacesMapper.enrichVenue
import com.sportspulse.india.core.data.mapper.PlacesMapper.inferSports
import com.sportspulse.india.core.data.mapper.PlacesMapper.toDomain
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import com.sportspulse.india.core.domain.repository.VenueRepository
import com.sportspulse.india.core.domain.usecase.HaversineDistanceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Full implementation of [VenueRepository].
 *
 * Data flow:
 *  1. Emit Room-cached venues immediately (if within 2-hour TTL).
 *  2. Fetch from Google Places Nearby Search using multiple keyword + type combinations.
 *  3. De-duplicate by placeId.
 *  4. Sort by Haversine distance first.
 *  5. Enrich top-25 results via Distance Matrix API (driving mode).
 *  6. Re-sort by actual travel time.
 *  7. Upsert to Room, emit updated list.
 */
class VenueRepositoryImpl @Inject constructor(
    private val venueDao: VenueDao,
    private val placesApiService: PlacesApiService,
    private val distanceMatrixService: DistanceMatrixService,
    private val haversine: HaversineDistanceUseCase
) : VenueRepository {

    companion object {
        private const val MAX_RADIUS_METERS = 25_000   // 25 km
        private const val DM_BATCH_SIZE     = 25       // Distance Matrix max destinations
        private val CACHE_TTL_MS = TimeUnit.HOURS.toMillis(2)

        /** Places search combinations (type + keyword pairs). */
        private val SEARCH_QUERIES = listOf(
            Pair("sports_complex", null),
            Pair("stadium", null),
            Pair("establishment", "turf"),
            Pair("establishment", "badminton court"),
            Pair("establishment", "indoor sports"),
            Pair("establishment", "cricket net"),
            Pair("gym", null),
            Pair("establishment", "swimming pool")
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    override fun getNearbyVenues(
        location: UserLocation,
        radiusKm: Double,
        venueType: VenueType?,
        sportType: SportType?
    ): Flow<Result<List<Venue>>> = flow {
        val radiusMeters = (radiusKm * 1000).toInt().coerceAtMost(MAX_RADIUS_METERS)
        val latPad = radiusKm / 111.0    // ~1° lat = 111 km
        val lngPad = radiusKm / (111.0 * Math.cos(Math.toRadians(location.latitude)))

        // Step 1: Emit fresh cache (if within TTL)
        val cached = loadCachedVenuesInBounds(
            minLat = location.latitude - latPad,
            maxLat = location.latitude + latPad,
            minLng = location.longitude - lngPad,
            maxLng = location.longitude + lngPad
        )
        val freshCache = cached.filter { !it.isCacheStale }
        if (freshCache.isNotEmpty()) {
            emit(Result.success(applyFilters(freshCache, venueType, sportType)))
        }

        // Step 2: Check if network refresh needed
        val staleCache = cached.any { it.isCacheStale }
        if (!staleCache && freshCache.isNotEmpty()) {
            Timber.d("VenueRepo: cache fresh, ${freshCache.size} venues")
            return@flow
        }

        // Step 3: Parallel Places API fetches
        val fresh = fetchVenuesFromPlaces(location, radiusMeters)
        if (fresh.isEmpty()) {
            if (cached.isEmpty()) emit(Result.failure(Exception("No venues found in this area")))
            return@flow
        }

        // Step 4: Haversine pre-sort
        val preSorted = haversine.sortByDistance(
            items        = fresh,
            originLat    = location.latitude,
            originLon    = location.longitude,
            latSelector  = { it.lat },
            lonSelector  = { it.lng }
        )

        // Step 5: Enrich top-25 with Distance Matrix
        val enriched = enrichWithTravelTimes(location, preSorted.take(DM_BATCH_SIZE))
            .getOrDefault(preSorted.take(DM_BATCH_SIZE))

        // Combine enriched + remainder (already distance-sorted)
        val finalList = (enriched + preSorted.drop(DM_BATCH_SIZE))
            .sortedWith(
                compareBy<Venue> { if (it.travelTimeMinutes > 0) it.travelTimeMinutes else Int.MAX_VALUE }
                    .thenBy { it.distanceKm }
            )

        // Step 6: Cache and emit
        cacheVenues(finalList)
        emit(Result.success(applyFilters(finalList, venueType, sportType)))
    }.flowOn(Dispatchers.IO)

    override suspend fun getVenueDetails(placeId: String): Result<Venue> =
        withContext(Dispatchers.IO) {
            runCatching {
                val existing = venueDao.getVenueByPlaceId(placeId)?.toDomain()
                    ?: throw NoSuchElementException("Venue $placeId not in cache")

                val response = placesApiService.getPlaceDetails(
                    placeId = placeId,
                    fields  = "name,formatted_address,rating,user_ratings_total," +
                            "opening_hours,photos,formatted_phone_number,website,geometry"
                )
                val enriched = response.result?.enrichVenue(existing) ?: existing
                cacheVenues(listOf(enriched))
                enriched
            }
        }

    override suspend fun enrichWithTravelTimes(
        origin: UserLocation,
        venues: List<Venue>
    ): Result<List<Venue>> = withContext(Dispatchers.IO) {
        runCatching {
            if (venues.isEmpty()) return@runCatching venues

            val originStr = "${origin.latitude},${origin.longitude}"
            val batches   = venues.chunked(DM_BATCH_SIZE)

            val enrichedVenues = mutableListOf<Venue>()
            batches.forEach { batch ->
                val destinations = batch.joinToString("|") { "${it.lat},${it.lng}" }
                val response = distanceMatrixService.getDistanceMatrix(
                    origins      = originStr,
                    destinations = destinations,
                    mode         = "driving"
                )
                val elements = response.rows?.firstOrNull()?.elements ?: emptyList()
                batch.forEachIndexed { index, venue ->
                    val element = elements.getOrNull(index)
                    val travelSeconds = element?.duration?.value ?: -1
                    val travelMinutes = if (travelSeconds > 0)
                        ceil(travelSeconds / 60.0).toInt() else -1
                    enrichedVenues += venue.copy(travelTimeMinutes = travelMinutes)
                }
            }
            enrichedVenues
        }.onFailure { e ->
            Timber.e(e, "VenueRepo: Distance Matrix enrichment failed")
        }
    }

    override suspend fun pruneStaleVenues() {
        withContext(Dispatchers.IO) {
            val expiryMs = System.currentTimeMillis() - CACHE_TTL_MS
            venueDao.deleteStaleVenues(expiryMs)
        }
    }

    override suspend fun cacheVenues(venues: List<Venue>) {
        withContext(Dispatchers.IO) {
            venueDao.upsertAll(venues.map { it.toEntity() })
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Places API parallel fetches
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchVenuesFromPlaces(
        location: UserLocation,
        radiusMeters: Int
    ): List<Venue> = coroutineScope {
        val locationStr = "${location.latitude},${location.longitude}"

        val results = SEARCH_QUERIES.map { (type, keyword) ->
            async {
                runCatching {
                    placesApiService.nearbySearch(
                        location = locationStr,
                        radius   = radiusMeters,
                        type     = type,
                        keyword  = keyword
                    ).results?.map { placeResult ->
                        placeResult.toDomain(
                            userLat = location.latitude,
                            userLng = location.longitude
                        )
                    } ?: emptyList()
                }.onFailure { e ->
                    Timber.e(e, "VenueRepo: Places search failed type=$type keyword=$keyword")
                }.getOrDefault(emptyList())
            }
        }.awaitAll()

        // De-duplicate by placeId, keep the entry with more data
        results.flatten()
            .groupBy { it.placeId }
            .mapValues { (_, dupes) ->
                dupes.maxByOrNull { it.reviewCount + if (it.isOpenNow) 1 else 0 }!!
            }
            .values
            .toList()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadCachedVenuesInBounds(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): List<Venue> = venueDao.getVenuesInBounds(minLat, maxLat, minLng, maxLng)
        .map { it.toDomain() }

    private fun applyFilters(
        venues: List<Venue>,
        venueType: VenueType?,
        sportType: SportType?
    ): List<Venue> {
        var result = venues
        if (venueType != null) result = result.filter { it.type == venueType }
        if (sportType  != null) result = result.filter { sportType in it.availableSports }
        return result
    }
}
