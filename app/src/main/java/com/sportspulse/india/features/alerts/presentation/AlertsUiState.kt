package com.sportspulse.india.features.alerts.presentation

import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.SportType

data class AlertsUiState(
    val alerts: List<MatchAlert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface AlertsIntent {
    data class RemoveAlert(val eventId: String) : AlertsIntent
    data class ToggleAlert(val eventId: String, val isEnabled: Boolean) : AlertsIntent
    object DismissError : AlertsIntent
    /** Create a generic sport-category reminder for upcoming matches of that sport. */
    data class CreateCustomAlert(
        val sport: SportType,
        val reminderMinutesBefore: Int
    ) : AlertsIntent
}
