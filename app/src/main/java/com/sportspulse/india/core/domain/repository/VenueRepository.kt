package com.sportspulse.india.core.domain.repository

import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import kotlinx.coroutines.flow.Flow

/**
 * Contract for discovering and caching nearby sports venues.
 *
 * Data source: Google Places API (Nearby Search + Place Details).
 * Cache: Room `venues` table with 2-hour TTL.
 */
interface VenueRepository {

    /**
     * Returns nearby venues within [radiusKm] of [location].
     *
     * Behaviour:
     *  1. Return cached venues immediately if within 2-hour TTL.
     *  2. Fetch from Places API in background, emit updated list.
     *  3. Sort by [Venue.travelTimeMinutes] ascending after Distance Matrix enrichment.
     *
     * @param location   User's current location.
     * @param radiusKm   Search radius in km (15–25, default 15).
     * @param venueType  Optional filter; null means all types.
     * @param sportType  Optional sport filter applied post-fetch.
     */
    fun getNearbyVenues(
        location: UserLocation,
        radiusKm: Double = 15.0,
        venueType: VenueType? = null,
        sportType: SportType? = null
    ): Flow<Result<List<Venue>>>

    /**
     * Fetches full place details (photos, phone, website, opening hours) for [placeId].
     */
    suspend fun getVenueDetails(placeId: String): Result<Venue>

    /**
     * Refreshes Distance Matrix travel times for a list of [venues] from [origin].
     * Updates the cached records in Room.
     */
    suspend fun enrichWithTravelTimes(
        origin: UserLocation,
        venues: List<Venue>
    ): Result<List<Venue>>

    /**
     * Removes cached venue entries older than 2 hours.
     */
    suspend fun pruneStaleVenues()

    /**
     * Persists a list of venues to Room.
     */
    suspend fun cacheVenues(venues: List<Venue>)
}
