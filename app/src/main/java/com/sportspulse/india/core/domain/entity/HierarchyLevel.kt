package com.sportspulse.india.core.domain.entity

/**
 * Priority-ranked hierarchy levels used by [HierarchyFilter] to sort [SportEvent]s.
 *
 * Priority order (ascending index = higher priority):
 *  INTERNATIONAL > NATIONAL > REGIONAL > LOCAL
 *
 * @param priority  Numeric rank — higher value = shown earlier in the feed.
 * @param label     Human-readable label shown on the match card badge.
 */
enum class HierarchyLevel(
    val priority: Int,
    val label: String
) {
    /** India vs. another nation, or a global multi-nation tournament (e.g., ICC World Cup, FIH WC). */
    INTERNATIONAL(priority = 4, label = "International"),

    /** Top-tier domestic league or national body competition (e.g., IPL, PKL, ISL, BWF Super Series). */
    NATIONAL(priority = 3, label = "National"),

    /** State-level teams, city-based ISL/I-League franchises, regional circuits. */
    REGIONAL(priority = 2, label = "Regional"),

    /** Ranji Trophy, Santosh Trophy, local state leagues, club-level competitions. */
    LOCAL(priority = 1, label = "Local / Domestic");

    companion object {
        /** Returns the [HierarchyLevel] with the highest priority. */
        fun highest(): HierarchyLevel = INTERNATIONAL

        /** Parses a string value; defaults to [LOCAL] if unrecognised. */
        fun fromString(value: String): HierarchyLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOCAL
    }
}
