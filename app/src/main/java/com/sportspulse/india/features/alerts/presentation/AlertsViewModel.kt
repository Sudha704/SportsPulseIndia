package com.sportspulse.india.features.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportspulse.india.core.domain.repository.AlertRepository
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
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        observeAlerts()
    }

    fun handleIntent(intent: AlertsIntent) {
        when (intent) {
            is AlertsIntent.RemoveAlert -> {
                viewModelScope.launch {
                    runCatching {
                        alertRepository.deleteAlert(intent.eventId)
                    }.onFailure { exception ->
                        Timber.e(exception, "Failed to remove alert")
                        _uiState.update { it.copy(error = "Failed to remove alert") }
                    }
                }
            }
            is AlertsIntent.ToggleAlert -> {
                viewModelScope.launch {
                    runCatching {
                        alertRepository.toggleAlert(intent.eventId, intent.isEnabled)
                    }.onFailure { exception ->
                        Timber.e(exception, "Failed to toggle alert")
                        _uiState.update { it.copy(error = "Failed to update alert status") }
                    }
                }
            }
            is AlertsIntent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
            is AlertsIntent.CreateCustomAlert -> {
                viewModelScope.launch {
                    runCatching {
                        // Create a placeholder alert for the chosen sport category.
                        // eventStartTimeIst is set 30 days from now so the reminder stays
                        // in the list until an actual match is found for that sport.
                        val now = System.currentTimeMillis()
                        val futureTime = now + (30L * 24 * 60 * 60 * 1000) // 30 days ahead
                        val alert = com.sportspulse.india.core.domain.entity.MatchAlert(
                            id = java.util.UUID.randomUUID().toString(),
                            eventId = "custom_${intent.sport.name}_${now}",
                            eventTitle = "Next ${intent.sport.displayName} match",
                            sport = intent.sport,
                            eventStartTimeIst = futureTime,
                            reminderMinutesBefore = intent.reminderMinutesBefore,
                            isEnabled = true
                        )
                        alertRepository.upsertAlert(alert)
                    }.onFailure { exception ->
                        Timber.e(exception, "Failed to create custom alert")
                        _uiState.update { it.copy(error = "Failed to create alert") }
                    }
                }
            }
        }
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            alertRepository.getAllAlerts()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { exception ->
                    Timber.e(exception, "Failed to observe alerts")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to load your match alerts"
                        ) 
                    }
                }
                .collect { alerts ->
                    _uiState.update { 
                        it.copy(
                            alerts = alerts,
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
        }
    }
}
