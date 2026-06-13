package com.sportspulse.india.core.domain.usecase

import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import javax.inject.Inject

/**
 * Ranks a flat list of [SportEvent]s by their [HierarchyLevel] priority,
 * then by [SportEvent.status] (LIVE first), then by [SportEvent.startTimeIst].
 *
 * Hierarchy levels (descending priority):
 *  INTERNATIONAL (4) → NATIONAL (3) → REGIONAL (2) → LOCAL (1)
 *
 * Within the same hierarchy level:
 *  LIVE → UPCOMING → COMPLETED → POSTPONED → CANCELLED
 *
 * Usage:
 * ```kotlin
 * val sorted = hierarchyFilter(rawEvents)
 * ```
 */
class HierarchyFilter @Inject constructor() {

    /**
     * @param events  Raw, unsorted list of sport events.
     * @return        Sorted list with highest-priority events first.
     */
    operator fun invoke(events: List<SportEvent>): List<SportEvent> =
        events.sortedWith(
            compareByDescending<SportEvent> { it.hierarchyLevel.priority }
                .thenBy { statusSortKey(it) }
                .thenBy { it.startTimeIst }
        )

    /**
     * Filters by [SportType] before ranking.
     */
    fun filterBySport(events: List<SportEvent>, sportType: SportType): List<SportEvent> =
        invoke(events.filter { it.sport == sportType })

    /**
     * Filters to only [com.sportspulse.india.core.domain.entity.MatchStatus.LIVE] events.
     */
    fun filterLive(events: List<SportEvent>): List<SportEvent> =
        invoke(events.filter { it.isLive })

    /**
     * Assigns a numeric sort key to [SportEvent.status].
     * Lower key = higher in the list.
     */
    private fun statusSortKey(event: SportEvent): Int = when (event.status) {
        com.sportspulse.india.core.domain.entity.MatchStatus.LIVE      -> 0
        com.sportspulse.india.core.domain.entity.MatchStatus.UPCOMING  -> 1
        com.sportspulse.india.core.domain.entity.MatchStatus.COMPLETED -> 2
        com.sportspulse.india.core.domain.entity.MatchStatus.POSTPONED -> 3
        com.sportspulse.india.core.domain.entity.MatchStatus.CANCELLED -> 4
    }

    /**
     * Assigns a [HierarchyLevel] to a [SportEvent] based on its [SportEvent.competition]
     * and [SportEvent.country].
     *
     * Classification rules:
     * - India playing outside India, or a multi-nation global tournament → INTERNATIONAL
     * - IPL / PKL / ISL / BWF Super Series India / Hockey India League → NATIONAL
     * - ISL franchise city match / state-level representation → REGIONAL
     * - Ranji Trophy / Santosh Trophy / local leagues → LOCAL
     */
    fun classify(event: SportEvent): HierarchyLevel {
        val comp = event.competition.uppercase()
        val country = event.country.uppercase()

        // International signals
        if (country != "INDIA" || INTERNATIONAL_COMPETITIONS.any { comp.contains(it) }) {
            return HierarchyLevel.INTERNATIONAL
        }
        // National signals
        if (NATIONAL_COMPETITIONS.any { comp.contains(it) }) {
            return HierarchyLevel.NATIONAL
        }
        // Regional signals
        if (REGIONAL_COMPETITIONS.any { comp.contains(it) }) {
            return HierarchyLevel.REGIONAL
        }
        return HierarchyLevel.LOCAL
    }

    companion object {
        private val INTERNATIONAL_COMPETITIONS = setOf(
            "WORLD CUP", "ICC", "FIFA", "AFC", "FIH", "BWF", "GRAND PRIX",
            "FORMULA 1", "F1", "MOTOGP", "INRC INTERNATIONAL", "OLYMPICS",
            "ASIAN GAMES", "COMMONWEALTH", "CHAMPIONS TROPHY", "T20 WORLD",
            "ODI WORLD", "TEST MATCH", "BILATERAL SERIES"
        )
        private val NATIONAL_COMPETITIONS = setOf(
            "IPL", "PKL", "ISL", "PRO KABADDI", "PREMIER BADMINTON",
            "HOCKEY INDIA LEAGUE", "SUPER SERIES INDIA", "INDIAN SUPER LEAGUE",
            "I-LEAGUE", "DULEEP TROPHY", "VIJAY HAZARE", "SYED MUSHTAQ ALI"
        )
        private val REGIONAL_COMPETITIONS = setOf(
            "RANJI TROPHY", "SANTOSH TROPHY", "STATE LEAGUE", "REGIONAL",
            "INRC", "INRC ROUND", "LOCAL FOOTBALL LEAGUE"
        )
    }
}
