package com.sportspulse.india.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the `match_alerts` table.
 * Each row represents a user-configured match reminder backed by a WorkManager job.
 */
@Entity(tableName = "match_alerts")
data class MatchAlertEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val eventTitle: String,
    val sport: String,
    val eventStartTimeIst: Long,
    val reminderMinutesBefore: Int,
    val isEnabled: Boolean,
    /** UUID of the scheduled WorkRequest — used to cancel the job on deletion. */
    val workRequestId: String?
)
