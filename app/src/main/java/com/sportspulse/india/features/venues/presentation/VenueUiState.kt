package com.sportspulse.india.features.venues.presentation

import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType

data class VenueUiState(
    val venues: List<Venue> = emptyList(),
    val userLocation: UserLocation = UserLocation.DEFAULT,
    val isLoading: Boolean = false,
    val isLocationLoading: Boolean = false,
    val error: String? = null,
    val locationError: String? = null,
    val searchRadiusKm: Double = 10.0,
    val selectedVenueType: VenueType? = null,
    val selectedSportType: SportType? = null,
    val isMapMode: Boolean = false
)

sealed interface VenueIntent {
    object FetchUserLocation : VenueIntent
    data class SetSearchRadius(val radiusKm: Double) : VenueIntent
    data class SelectVenueType(val venueType: VenueType?) : VenueIntent
    data class SelectSportType(val sportType: SportType?) : VenueIntent
    data class ToggleViewMode(val isMapMode: Boolean) : VenueIntent
    object RefreshVenues : VenueIntent
    object DismissError : VenueIntent
}
