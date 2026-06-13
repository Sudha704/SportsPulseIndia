package com.sportspulse.india.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sportspulse.india.core.data.db.entity.SportEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `sport_events` table.
 */
@Dao
interface SportEventDao {

    /** Emits the full list of cached events, sorted by hierarchy priority (desc) then start time. */
    @Query(
        """
        SELECT * FROM sport_events
        ORDER BY
            CASE hierarchyLevel
                WHEN 'INTERNATIONAL' THEN 4
                WHEN 'NATIONAL'      THEN 3
                WHEN 'REGIONAL'      THEN 2
                ELSE                      1
            END DESC,
            CASE status
                WHEN 'LIVE'      THEN 0
                WHEN 'UPCOMING'  THEN 1
                WHEN 'COMPLETED' THEN 2
                ELSE                  3
            END ASC,
            startTimeIst ASC
        """
    )
    fun observeAllEvents(): Flow<List<SportEventEntity>>

    /** Returns only LIVE events ordered by hierarchy. */
    @Query(
        """
        SELECT * FROM sport_events
        WHERE status = 'LIVE'
        ORDER BY
            CASE hierarchyLevel
                WHEN 'INTERNATIONAL' THEN 4
                WHEN 'NATIONAL'      THEN 3
                WHEN 'REGIONAL'      THEN 2
                ELSE                      1
            END DESC
        """
    )
    fun observeLiveEvents(): Flow<List<SportEventEntity>>

    /** Returns events filtered by sport. */
    @Query(
        """
        SELECT * FROM sport_events
        WHERE sport = :sport
        ORDER BY startTimeIst ASC
        """
    )
    fun observeEventsBySport(sport: String): Flow<List<SportEventEntity>>

    /** Fetches a single event by ID (one-shot). */
    @Query("SELECT * FROM sport_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): SportEventEntity?

    /** Upserts a list of events (replace on conflict). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<SportEventEntity>)

    /** Updates the Gemini summary for a specific event. */
    @Query("UPDATE sport_events SET geminiSummary = :summary WHERE id = :id")
    suspend fun updateGeminiSummary(id: String, summary: String)

    /** Deletes events cached more than [expiryMs] milliseconds ago. */
    @Query("DELETE FROM sport_events WHERE cachedAt < :expiryMs")
    suspend fun deleteStaleEvents(expiryMs: Long)

    /** Returns the epoch-ms of the most recently cached event row. */
    @Query("SELECT MAX(cachedAt) FROM sport_events")
    suspend fun getLatestCacheTimestamp(): Long?

    /** Clears the entire table. */
    @Query("DELETE FROM sport_events")
    suspend fun clearAll()
}
