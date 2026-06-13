package com.sportspulse.india.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sportspulse.india.core.data.db.converter.Converters

/**
 * Room entity mirroring [com.sportspulse.india.core.domain.entity.SportEvent].
 * The `broadcasts` list is serialised to JSON via [Converters].
 */
@Entity(tableName = "sport_events")
@TypeConverters(Converters::class)
data class SportEventEntity(
    @PrimaryKey val id: String,
    val sport: String,
    val title: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogoUrl: String?,
    val awayTeamLogoUrl: String?,
    val status: String,
    val scoreOrTime: String,
    val venue: String,
    val city: String,
    val country: String,
    val hierarchyLevel: String,
    /** JSON array of Broadcast objects (serialised by Converters). */
    val broadcastsJson: String,
    val startTimeIst: Long,
    val competition: String,
    val seriesId: String?,
    val geminiSummary: String?,
    /** JSON array of NewsItem objects. */
    val newsItemsJson: String,
    val sourceUrl: String?,
    /** Epoch-ms when this row was last fetched from the network. */
    val cachedAt: Long = System.currentTimeMillis()
)
