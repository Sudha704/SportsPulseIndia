package com.sportspulse.india.features.dashboard.presentation

import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType

data class DashboardUiState(
    val events: List<SportEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSportFilter: SportType? = null,
    val isLiveFilterActive: Boolean = false,
    val isRefreshing: Boolean = false
)

sealed interface DashboardIntent {
    data class SelectSportFilter(val sport: SportType?) : DashboardIntent
    data class ToggleLiveFilter(val isLive: Boolean) : DashboardIntent
    object RefreshEvents : DashboardIntent
    object DismissError : DashboardIntent
}
