package com.sportspulse.india.features.detail.presentation

import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.SportEvent

data class DetailUiState(
    val event: SportEvent? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSummaryLoading: Boolean = false,
    val isSettingAlert: Boolean = false,
    val currentAlert: MatchAlert? = null
)

sealed interface DetailIntent {
    data class LoadEvent(val eventId: String) : DetailIntent
    data class GenerateAiSummary(val eventId: String) : DetailIntent
    data class SetMatchAlert(val minutesBefore: Int) : DetailIntent
    object RemoveMatchAlert : DetailIntent
    object DismissError : DetailIntent
}
