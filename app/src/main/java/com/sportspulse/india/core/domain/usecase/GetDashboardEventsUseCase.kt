package com.sportspulse.india.core.domain.usecase

import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.repository.SportEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for fetching the ranked dashboard feed of sport events.
 *
 * Combines data from [SportEventRepository] with [HierarchyFilter] ranking.
 * Returns a [Flow] so the UI reactively updates when data changes.
 *
 * @param repository     Source of truth for sport events.
 * @param hierarchyFilter Ranking engine.
 */
class GetDashboardEventsUseCase @Inject constructor(
    private val repository: SportEventRepository,
    private val hierarchyFilter: HierarchyFilter
) {
    /**
     * @param sportType     Optional sport type filter (null = all sports).
     * @param onlyLive      When true, returns only events with LIVE status.
     * @param forceRefresh  When true, bypasses cache and hits the network.
     */
    operator fun invoke(
        sportType: SportType? = null,
        onlyLive: Boolean = false,
        forceRefresh: Boolean = false
    ): Flow<Result<List<SportEvent>>> {
        return repository.getEvents(forceRefresh).map { result ->
            result.map { events ->
                val filtered = when {
                    onlyLive && sportType != null ->
                        hierarchyFilter.filterLive(events).filter { it.sport == sportType }
                    onlyLive ->
                        hierarchyFilter.filterLive(events)
                    sportType != null ->
                        hierarchyFilter.filterBySport(events, sportType)
                    else ->
                        hierarchyFilter(events)
                }
                filtered
            }
        }
    }
}
