package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.dto.SportRadarEvent
import com.sportspulse.india.core.domain.entity.Broadcast
import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.MatchStatus
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.usecase.HierarchyFilter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Maps [SportRadarEvent] DTOs to [SportEvent] domain entities.
 *
 * SportRadar is used for:
 *  - Kabaddi (PKL)
 *  - Hockey (FIH / Hockey India League)
 *  - Motorsports (Formula 1, MotoGP)
 *  - Badminton (BWF)
 *
 * SportRadar status → [MatchStatus]:
 *  "scheduled"   → UPCOMING
 *  "live"        → LIVE
 *  "closed"      → COMPLETED
 *  "cancelled"   → CANCELLED
 *  "postponed"   → POSTPONED
 *  "suspended"   → POSTPONED
 */
object SportRadarMapper {

    private val UTC = TimeZone.getTimeZone("UTC")
    private val IST = TimeZone.getTimeZone("Asia/Kolkata")
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = UTC
    }
    private val displayFormatter = SimpleDateFormat("d MMM, h:mm a z", Locale.US).apply {
        timeZone = IST
    }

    fun SportRadarEvent.toDomain(
        sportType: SportType,
        broadcasts: List<Broadcast>,
        hierarchyFilter: HierarchyFilter
    ): SportEvent {
        val home    = competitors?.firstOrNull { it.qualifier == "home" }
        val away    = competitors?.firstOrNull { it.qualifier == "away" }
        val homeName = home?.name ?: competitors?.getOrNull(0)?.name ?: "Competitor A"
        val awayName = away?.name ?: competitors?.getOrNull(1)?.name ?: "Competitor B"

        val matchStatus  = parseStatus(status)
        val startMs      = parseScheduled(scheduled)
        val venueName    = venue?.name ?: "TBC"
        val city         = venue?.city ?: ""
        val country      = venue?.country ?: "India"
        val competition  = tournament?.name ?: sportType.displayName

        val event = SportEvent(
            id              = "sportradar_$id",
            sport           = sportType,
            title           = buildTitle(sportType, homeName, awayName, tournament?.name),
            homeTeam        = homeName,
            awayTeam        = awayName,
            homeTeamLogoUrl = null, // SportRadar trial doesn't expose logos
            awayTeamLogoUrl = null,
            status          = matchStatus,
            scoreOrTime     = buildScoreOrTime(matchStatus, startMs),
            venue           = venueName,
            city            = city,
            country         = country,
            hierarchyLevel  = HierarchyLevel.LOCAL,
            broadcasts      = broadcasts,
            startTimeIst    = startMs,
            competition     = competition,
            sourceUrl       = "https://sportradar.com"
        )
        return event.copy(hierarchyLevel = hierarchyFilter.classify(event))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildTitle(
        sport: SportType,
        home: String,
        away: String,
        tournamentName: String?
    ): String = when (sport) {
        SportType.MOTORSPORTS -> tournamentName ?: "$home vs $away"
        else                  -> "$home vs $away"
    }

    private fun buildScoreOrTime(status: MatchStatus, startMs: Long): String = when (status) {
        MatchStatus.LIVE      -> "LIVE"
        MatchStatus.COMPLETED -> "Completed"
        MatchStatus.POSTPONED -> "Postponed"
        MatchStatus.CANCELLED -> "Cancelled"
        MatchStatus.UPCOMING  -> displayFormatter.format(startMs)
    }

    private fun parseStatus(raw: String?): MatchStatus = when (raw?.lowercase()) {
        "live", "inprogress"              -> MatchStatus.LIVE
        "closed", "complete", "finished"  -> MatchStatus.COMPLETED
        "cancelled"                       -> MatchStatus.CANCELLED
        "postponed", "suspended"          -> MatchStatus.POSTPONED
        else                              -> MatchStatus.UPCOMING
    }

    private fun parseScheduled(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { isoParser.parse(raw)?.time }
            .getOrNull() ?: System.currentTimeMillis()
    }
}
