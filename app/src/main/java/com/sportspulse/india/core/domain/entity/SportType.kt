package com.sportspulse.india.core.domain.entity

/**
 * Enumerates every sport category supported by SportsPulse India.
 * Used as a discriminator for filtering, routing, and icon resolution.
 */
enum class SportType(
    val displayName: String,
    val emoji: String
) {
    CRICKET("Cricket", "🏏"),
    FOOTBALL("Football", "⚽"),
    KABADDI("Kabaddi", "🤼"),
    BADMINTON("Badminton", "🏸"),
    HOCKEY("Hockey", "🏑"),
    MOTORSPORTS("Motorsports", "🏎️");

    companion object {
        fun fromString(value: String): SportType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CRICKET
    }
}
