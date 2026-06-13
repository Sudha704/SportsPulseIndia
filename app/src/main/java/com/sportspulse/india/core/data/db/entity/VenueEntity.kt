package com.sportspulse.india.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sportspulse.india.core.data.db.converter.Converters

/**
 * Room entity for the `venues` table.
 * [availableSportsJson] is a JSON array of [SportType] names.
 * TTL: 2 hours from [cachedAt].
 */
@Entity(tableName = "venues")
@TypeConverters(Converters::class)
data class VenueEntity(
    @PrimaryKey val placeId: String,
    val name: String,
    val address: String,
    val type: String,
    val distanceKm: Double,
    val travelTimeMinutes: Int,
    val rating: Float,
    val reviewCount: Int,
    val isOpenNow: Boolean,
    val nextOpenTime: String?,
    /** JSON array of SportType enum names. */
    val availableSportsJson: String,
    val lat: Double,
    val lng: Double,
    val photoReference: String?,
    val phoneNumber: String?,
    val websiteUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
