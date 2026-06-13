package com.sportspulse.india.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportspulse.india.core.domain.usecase.GetDashboardEventsUseCase
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
class DashboardViewModel @Inject constructor(
    private val getDashboardEventsUseCase: GetDashboardEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadEvents(forceRefresh = false)
    }

    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.SelectSportFilter -> {
                _uiState.update { it.copy(selectedSportFilter = intent.sport) }
                loadEvents(forceRefresh = false)
            }
            is DashboardIntent.ToggleLiveFilter -> {
                _uiState.update { it.copy(isLiveFilterActive = intent.isLive) }
                loadEvents(forceRefresh = false)
            }
            is DashboardIntent.RefreshEvents -> {
                _uiState.update { it.copy(isRefreshing = true) }
                loadEvents(forceRefresh = true)
            }
            is DashboardIntent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadEvents(forceRefresh: Boolean) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            getDashboardEventsUseCase(
                sportType = currentState.selectedSportFilter,
                onlyLive = currentState.isLiveFilterActive,
                forceRefresh = forceRefresh
            )
            .onStart {
                if (!forceRefresh) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
            }
            .catch { exception ->
                Timber.e(exception, "Failed to load dashboard events")
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false,
                        error = exception.message ?: "Failed to load events"
                    ) 
                }
            }
            .collect { result ->
                result.onSuccess { events ->
                    _uiState.update {
                        it.copy(
                            events = events,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Failed to load dashboard events (Result failure)")
                    // Keep existing events if we have them, just show error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = exception.message ?: "Failed to load events"
                        )
                    }
                }
            }
        }
    }
}
