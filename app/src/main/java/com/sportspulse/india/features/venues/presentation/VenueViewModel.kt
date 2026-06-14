package com.sportspulse.india.features.venues.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportspulse.india.core.domain.usecase.GetNearbyVenuesUseCase
import com.sportspulse.india.core.domain.usecase.GetUserLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VenueViewModel @Inject constructor(
    private val getNearbyVenuesUseCase: GetNearbyVenuesUseCase,
    private val getUserLocationUseCase: GetUserLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VenueUiState())
    val uiState: StateFlow<VenueUiState> = _uiState.asStateFlow()

    init {
        observeLocation()
        // Location will be fetched by VenueScreen after permissions are granted
    }

    fun handleIntent(intent: VenueIntent) {
        when (intent) {
            is VenueIntent.FetchUserLocation -> {
                fetchLocation()
            }
            is VenueIntent.SetSearchRadius -> {
                _uiState.update { it.copy(searchRadiusKm = intent.radiusKm) }
                loadVenues()
            }
            is VenueIntent.SelectVenueType -> {
                _uiState.update { it.copy(selectedVenueType = intent.venueType) }
                loadVenues()
            }
            is VenueIntent.SelectSportType -> {
                _uiState.update { it.copy(selectedSportType = intent.sportType) }
                loadVenues()
            }
            is VenueIntent.ToggleViewMode -> {
                _uiState.update { it.copy(isMapMode = intent.isMapMode) }
            }
            is VenueIntent.RefreshVenues -> {
                loadVenues(forceRefresh = true)
            }
            is VenueIntent.DismissError -> {
                _uiState.update { it.copy(error = null, locationError = null) }
            }
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            getUserLocationUseCase.observeLocation()
                .collect { location ->
                    _uiState.update { it.copy(userLocation = location) }
                    // When location updates from DataStore, load venues
                    if (location.latitude != 0.0 && location.longitude != 0.0) {
                        loadVenues()
                    }
                }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocationLoading = true, locationError = null) }
            val result = getUserLocationUseCase.fetchFreshLocation()
            result.onSuccess {
                _uiState.update { it.copy(isLocationLoading = false) }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to fetch user location")
                _uiState.update { 
                    it.copy(
                        isLocationLoading = false,
                        locationError = "Location unavailable. Please check permissions."
                    )
                }
            }
        }
    }

    private fun loadVenues(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val loc = currentState.userLocation
            
            if (loc.latitude == 0.0 && loc.longitude == 0.0) {
                // Wait for location before loading
                return@launch
            }

            getNearbyVenuesUseCase(
                location = loc,
                radiusKm = currentState.searchRadiusKm,
                venueType = currentState.selectedVenueType,
                sportType = currentState.selectedSportType,
                forceRefresh = forceRefresh
            )
            .onStart {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            .catch { exception ->
                Timber.e(exception, "Failed to load venues")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to find nearby venues"
                    )
                }
            }
            .collect { result ->
                result.onSuccess { venues ->
                    _uiState.update {
                        it.copy(
                            venues = venues,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Failed to load venues (Result failure)")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to find nearby venues"
                        )
                    }
                }
            }
        }
    }
}
