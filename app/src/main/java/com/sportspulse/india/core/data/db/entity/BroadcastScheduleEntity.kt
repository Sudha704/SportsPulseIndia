package com.sportspulse.india.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sportspulse.india.core.data.db.converter.Converters

/**
 * Room entity for the `broadcast_schedule` table.
 * Stores one row per event-channel combination.
 * Refreshed every 6 hours via WorkManager [BroadcastRefreshWorker].
 */
@Entity(tableName = "broadcast_schedule")
@TypeConverters(Converters::class)
data class BroadcastScheduleEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    /** Foreign key reference to [SportEventEntity.id]. */
    val eventId: String,
    val channelName: String,
    val channelNumber: Int?,
    val platform: String,
    val isFree: Boolean,
    val deeplinkUri: String?,
    val streamingUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
