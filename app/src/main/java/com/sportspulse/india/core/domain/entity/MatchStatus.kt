package com.sportspulse.india.core.domain.entity

/**
 * Life-cycle state of a sporting event.
 *
 * Used to drive badge colour, icon, and refresh cadence:
 *  - [LIVE]      → red pulsing badge, 30-second refresh
 *  - [UPCOMING]  → grey/blue badge, no live refresh needed
 *  - [COMPLETED] → muted badge, data is static
 *  - [POSTPONED] → amber badge
 *  - [CANCELLED] → dark/struck-through badge
 */
enum class MatchStatus(val displayLabel: String) {
    LIVE("LIVE"),
    UPCOMING("Upcoming"),
    COMPLETED("Full Time"),
    POSTPONED("Postponed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): MatchStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UPCOMING
    }
}
