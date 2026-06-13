package com.sportspulse.india.core.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sportspulse.india.core.data.db.entity.BroadcastScheduleEntity

/**
 * Room TypeConverters for all non-primitive fields stored in the database.
 *
 * Handles:
 * - List<String>            → JSON string (sport names, etc.)
 * - List<BroadcastScheduleEntity> → not stored inline; kept as List<String>
 *
 * The [Gson] instance here is a lightweight local instance. The singleton
 * Gson provided by Hilt is used elsewhere; this keeps converters decoupled.
 */
class Converters {

    private val gson = Gson()

    // ─── List<String> ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(list: List<String>): String =
        gson.toJson(list)

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // ─── List<Int> ───────────────────────────────────────────────────────────

    @TypeConverter
    fun fromIntList(list: List<Int>): String =
        gson.toJson(list)

    @TypeConverter
    fun toIntList(json: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
