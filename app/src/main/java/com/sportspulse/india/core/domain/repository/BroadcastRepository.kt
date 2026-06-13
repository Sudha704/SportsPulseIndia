package com.sportspulse.india.core.domain.repository

import com.sportspulse.india.core.domain.entity.Broadcast
import kotlinx.coroutines.flow.Flow

/**
 * Contract for fetching and caching broadcast / telecast schedule data.
 *
 * Priority chain:
 *  1. Remote JSON config (GitHub Gist) — refreshed every 6 hours via WorkManager
 *  2. Room `broadcast_schedule` table (offline fallback)
 *  3. Gemini API (last resort: prompts for structured JSON)
 */
interface BroadcastRepository {

    /**
     * Returns all broadcasts scheduled for event [eventId].
     * Emits cached data first, then triggers a remote refresh if stale.
     */
    fun getBroadcastsForEvent(eventId: String): Flow<Result<List<Broadcast>>>

    /**
     * Forces a refresh of the remote broadcast JSON config from the GitHub Gist URL.
     * Called by WorkManager every 6 hours.
     */
    suspend fun refreshRemoteConfig(): Result<Unit>

    /**
     * Uses Gemini API to infer broadcast information for [eventTitle].
     * Returns a structured list of [Broadcast] objects parsed from Gemini's JSON response.
     */
    suspend fun inferBroadcastsViaGemini(eventTitle: String, sport: String): Result<List<Broadcast>>

    /**
     * Persists a list of [Broadcast] entries linked to [eventId] in Room.
     */
    suspend fun cacheBroadcasts(eventId: String, broadcasts: List<Broadcast>)

    /**
     * Returns the epoch-ms timestamp of the last successful remote config fetch.
     */
    suspend fun getLastRefreshTimestamp(): Long
}
