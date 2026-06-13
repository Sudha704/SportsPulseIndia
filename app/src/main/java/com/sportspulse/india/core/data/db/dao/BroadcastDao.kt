package com.sportspulse.india.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sportspulse.india.core.data.db.entity.BroadcastScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `broadcast_schedule` table.
 * Refreshed every 6 hours via WorkManager [BroadcastRefreshWorker].
 */
@Dao
interface BroadcastDao {

    /** Emits all broadcast entries for a given event. */
    @Query("SELECT * FROM broadcast_schedule WHERE eventId = :eventId ORDER BY isFree DESC")
    fun observeBroadcastsForEvent(eventId: String): Flow<List<BroadcastScheduleEntity>>

    /** One-shot fetch for a given event. */
    @Query("SELECT * FROM broadcast_schedule WHERE eventId = :eventId ORDER BY isFree DESC")
    suspend fun getBroadcastsForEvent(eventId: String): List<BroadcastScheduleEntity>

    /** Inserts or replaces all broadcast rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(broadcasts: List<BroadcastScheduleEntity>)

    /** Deletes all broadcasts linked to a specific event (before re-inserting fresh data). */
    @Query("DELETE FROM broadcast_schedule WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    /** Returns the most recent cachedAt timestamp across the table. */
    @Query("SELECT MAX(cachedAt) FROM broadcast_schedule")
    suspend fun getLastRefreshTimestamp(): Long?

    /** Deletes broadcast rows older than [expiryMs]. */
    @Query("DELETE FROM broadcast_schedule WHERE cachedAt < :expiryMs")
    suspend fun deleteStale(expiryMs: Long)

    /** Clears the entire table. */
    @Query("DELETE FROM broadcast_schedule")
    suspend fun clearAll()
}
