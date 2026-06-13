package com.sportspulse.india.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sportspulse.india.core.data.db.converter.Converters
import com.sportspulse.india.core.data.db.dao.AlertDao
import com.sportspulse.india.core.data.db.dao.BroadcastDao
import com.sportspulse.india.core.data.db.dao.SportEventDao
import com.sportspulse.india.core.data.db.dao.VenueDao
import com.sportspulse.india.core.data.db.entity.BroadcastScheduleEntity
import com.sportspulse.india.core.data.db.entity.MatchAlertEntity
import com.sportspulse.india.core.data.db.entity.SportEventEntity
import com.sportspulse.india.core.data.db.entity.VenueEntity

/**
 * Root Room database for SportsPulse India.
 *
 * Version history:
 *  1 → initial schema
 *
 * Entities:
 *  - [SportEventEntity]       — `sport_events` table
 *  - [VenueEntity]            — `venues` table (2-hour TTL)
 *  - [BroadcastScheduleEntity] — `broadcast_schedule` table (6-hour refresh)
 *  - [MatchAlertEntity]       — `match_alerts` table
 */
@Database(
    entities = [
        SportEventEntity::class,
        VenueEntity::class,
        BroadcastScheduleEntity::class,
        MatchAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SportsPulseDatabase : RoomDatabase() {

    abstract fun sportEventDao(): SportEventDao
    abstract fun venueDao(): VenueDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val DATABASE_NAME = "sportspulse_india.db"
    }
}
