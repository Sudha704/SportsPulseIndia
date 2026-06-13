package com.sportspulse.india.core.domain.usecase

import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import com.sportspulse.india.core.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for fetching nearby venues with optional sport/type filters.
 *
 * Delegates to [VenueRepository], which:
 *  1. Returns Room-cached results immediately (if within 2-hour TTL)
 *  2. Fetches fresh data from Google Places API
 *  3. Enriches with Distance Matrix travel times
 *  4. Emits updated list sorted by [Venue.travelTimeMinutes]
 */
class GetNearbyVenuesUseCase @Inject constructor(
    private val repository: VenueRepository
) {
    /**
     * @param location   User's current or manually entered location.
     * @param radiusKm   Search radius; clamped to [15.0, 25.0] km.
     * @param venueType  Optional filter for venue category.
     * @param sportType  Optional filter for sport type.
     */
    operator fun invoke(
        location: UserLocation,
        radiusKm: Double = 15.0,
        venueType: VenueType? = null,
        sportType: SportType? = null
    ): Flow<Result<List<Venue>>> {
        val clampedRadius = radiusKm.coerceIn(15.0, 25.0)
        return repository.getNearbyVenues(location, clampedRadius, venueType, sportType)
    }
}
