package com.sportspulse.india.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sportspulse.india.core.data.db.entity.MatchAlertEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `match_alerts` table.
 */
@Dao
interface AlertDao {

    /** Emits all alerts in real-time, ordered by event start time ascending. */
    @Query("SELECT * FROM match_alerts ORDER BY eventStartTimeIst ASC")
    fun observeAllAlerts(): Flow<List<MatchAlertEntity>>

    /** One-shot fetch of a single alert by event ID. */
    @Query("SELECT * FROM match_alerts WHERE eventId = :eventId LIMIT 1")
    suspend fun getAlertForEvent(eventId: String): MatchAlertEntity?

    /** Inserts or replaces an alert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: MatchAlertEntity)

    /** Updates the workRequestId after WorkManager schedules the job. */
    @Query("UPDATE match_alerts SET workRequestId = :workRequestId WHERE id = :alertId")
    suspend fun updateWorkRequestId(alertId: String, workRequestId: String)

    /** Toggles the isEnabled flag for an alert. */
    @Query("UPDATE match_alerts SET isEnabled = :enabled WHERE eventId = :eventId")
    suspend fun setEnabled(eventId: String, enabled: Boolean)

    /** Deletes the alert for a given event. */
    @Query("DELETE FROM match_alerts WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    /** Deletes all alerts whose event has already started (past events). */
    @Query("DELETE FROM match_alerts WHERE eventStartTimeIst < :nowMs")
    suspend fun deletePastAlerts(nowMs: Long)
}
