package com.sportspulse.india.core.domain.repository

import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.MatchStatus
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import kotlinx.coroutines.flow.Flow

/**
 * Contract for aggregating sporting event data from all sources:
 * - SportRadar trial API
 * - CricAPI (cricket-specific)
 * - Football-data.org
 * - RSS news feeds
 * - Gemini API enrichment (fallback)
 * - Local Room cache
 */
interface SportEventRepository {

    /**
     * Emits a combined, hierarchy-sorted list of sport events.
     * Implementation should:
     *  1. Return cached data immediately (offline-first)
     *  2. Fetch fresh data from remote sources
     *  3. Emit updated list after remote fetch
     *
     * @param forceRefresh  When true, bypasses cache and fetches fresh data.
     */
    fun getEvents(forceRefresh: Boolean = false): Flow<Result<List<SportEvent>>>

    /**
     * Returns only events currently in [MatchStatus.LIVE] state.
     */
    fun getLiveEvents(): Flow<Result<List<SportEvent>>>

    /**
     * Returns events filtered by [sportType].
     */
    fun getEventsBySport(sportType: SportType): Flow<Result<List<SportEvent>>>

    /**
     * Returns events filtered by [hierarchyLevel].
     */
    fun getEventsByHierarchy(hierarchyLevel: HierarchyLevel): Flow<Result<List<SportEvent>>>

    /**
     * Fetches a single event by its [eventId], enriching with Gemini if [withAiSummary] is true.
     */
    suspend fun getEventById(eventId: String, withAiSummary: Boolean = false): Result<SportEvent>

    /**
     * Refreshes telecast/broadcast data for all cached events.
     * Called by WorkManager every 15 minutes.
     */
    suspend fun refreshBroadcastData(): Result<Unit>

    /**
     * Persists a list of events to the local Room cache.
     */
    suspend fun cacheEvents(events: List<SportEvent>)

    /**
     * Clears expired events from local cache.
     */
    suspend fun pruneStaleEvents()
}
