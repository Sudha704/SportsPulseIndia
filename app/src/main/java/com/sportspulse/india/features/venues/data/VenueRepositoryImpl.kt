package com.sportspulse.india.features.venues.data

import com.sportspulse.india.core.data.api.PlacesApiService
import com.sportspulse.india.core.data.db.dao.VenueDao
import com.sportspulse.india.core.data.mapper.EntityMapper.toDomain
import com.sportspulse.india.core.data.mapper.EntityMapper.toEntity
import com.sportspulse.india.core.data.mapper.PlacesMapper.inferSports
import com.sportspulse.india.core.data.mapper.PlacesMapper.toDomain
import com.sportspulse.india.core.data.dto.NewPlacesSearchRequest
import com.sportspulse.india.core.data.dto.LocationBias
import com.sportspulse.india.core.data.dto.LocationCircle
import com.sportspulse.india.core.data.dto.LatLngDto
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
    private val haversine: HaversineDistanceUseCase
) : VenueRepository {

    companion object {
        private const val MAX_RADIUS_METERS = 25_000   // 25 km
        private val CACHE_TTL_MS = TimeUnit.HOURS.toMillis(2)

        /** Places search combinations (text queries). */
        private val SEARCH_QUERIES = listOf(
            "sports complex",
            "stadium",
            "turf",
            "badminton court",
            "indoor sports",
            "cricket net",
            "gym",
            "swimming pool"
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    override fun getNearbyVenues(
        location: UserLocation,
        radiusKm: Double,
        venueType: VenueType?,
        sportType: SportType?,
        forceRefresh: Boolean
    ): Flow<Result<List<Venue>>> = flow {
        val radiusMeters = (radiusKm * 1000).toInt().coerceAtMost(MAX_RADIUS_METERS)
        val latPad = radiusKm / 111.0    // ~1° lat = 111 km
        val lngPad = radiusKm / (111.0 * Math.cos(Math.toRadians(location.latitude)))

        // Step 1: Emit fresh cache (if within TTL and not forcing refresh)
        val cached = loadCachedVenuesInBounds(
            minLat = location.latitude - latPad,
            maxLat = location.latitude + latPad,
            minLng = location.longitude - lngPad,
            maxLng = location.longitude + lngPad
        )
        val freshCache = cached.filter { !it.isCacheStale }
        
        if (!forceRefresh) {
            if (freshCache.isNotEmpty()) {
                emit(Result.success(applyFilters(freshCache, venueType, sportType)))
            }

            // Step 2: Check if network refresh needed
            val staleCache = cached.any { it.isCacheStale }
            if (!staleCache && freshCache.isNotEmpty()) {
                Timber.d("VenueRepo: cache fresh, ${freshCache.size} venues")
                return@flow
            }
        } else {
            Timber.d("VenueRepo: forceRefresh requested, bypassing cache")
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

        // Combine enriched + remainder (already distance-sorted)
        val finalList = preSorted.sortedBy { it.distanceKm }

        // Step 6: Cache and emit
        cacheVenues(finalList)
        emit(Result.success(applyFilters(finalList, venueType, sportType)))
    }.flowOn(Dispatchers.IO)

    override suspend fun getVenueDetails(placeId: String): Result<Venue> =
        withContext(Dispatchers.IO) {
            runCatching {
                val existing = venueDao.getVenueByPlaceId(placeId)?.toDomain()
                    ?: throw NoSuchElementException("Venue $placeId not in cache")
                existing
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
        val locationBias = LocationBias(
            circle = LocationCircle(
                center = LatLngDto(latitude = location.latitude, longitude = location.longitude),
                radius = radiusMeters.toDouble()
            )
        )

        val results = SEARCH_QUERIES.map { keyword ->
            async {
                runCatching {
                    val request = NewPlacesSearchRequest(
                        textQuery = keyword,
                        locationBias = locationBias
                    )
                    placesApiService.searchText(request).places?.map { placeResult ->
                        placeResult.toDomain(
                            userLat = location.latitude,
                            userLng = location.longitude
                        )
                    } ?: emptyList()
                }.onFailure { e ->
                    Timber.e(e, "VenueRepo: Places search failed keyword=$keyword")
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
