package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.dto.FootballMatch
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
 * Maps football-data.org [FootballMatch] DTOs to [SportEvent] domain entities.
 *
 * football-data.org status → [MatchStatus]:
 *  "SCHEDULED"          → UPCOMING
 *  "LIVE" / "IN_PLAY"  → LIVE
 *  "PAUSED"             → LIVE  (half-time, treat as live)
 *  "FINISHED"           → COMPLETED
 *  "POSTPONED"          → POSTPONED
 *  "CANCELLED"          → CANCELLED
 *  "SUSPENDED"          → POSTPONED
 */
object FootballDataMapper {

    private val UTC = TimeZone.getTimeZone("UTC")
    private val IST = TimeZone.getTimeZone("Asia/Kolkata")
    private val utcParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = UTC
    }
    private val displayFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.US).apply {
        timeZone = IST
    }

    fun FootballMatch.toDomain(
        broadcasts: List<Broadcast>,
        hierarchyFilter: HierarchyFilter
    ): SportEvent {
        val homeTeam   = homeTeam?.name ?: "Home"
        val awayTeam   = awayTeam?.name ?: "Away"
        val homeCrest  = this.homeTeam?.crest
        val awayCrest  = this.awayTeam?.crest

        val matchStatus = parseStatus(status)
        val startMs     = parseUtcDate(utcDate)
        val scoreStr    = buildScoreString(matchStatus, startMs)
        val compName    = competition?.name ?: "Football"
        val compCode    = competition?.code ?: ""

        val event = SportEvent(
            id              = "football_$id",
            sport           = SportType.FOOTBALL,
            title           = "$homeTeam vs $awayTeam",
            homeTeam        = homeTeam,
            awayTeam        = awayTeam,
            homeTeamLogoUrl = homeCrest,
            awayTeamLogoUrl = awayCrest,
            status          = matchStatus,
            scoreOrTime     = scoreStr,
            venue           = "TBC",
            city            = "",
            country         = deriveCountry(compCode),
            hierarchyLevel  = HierarchyLevel.LOCAL,
            broadcasts      = broadcasts,
            startTimeIst    = startMs,
            competition     = compName,
            sourceUrl       = "https://www.football-data.org/matches/$id"
        )
        return event.copy(hierarchyLevel = hierarchyFilter.classify(event))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Score string
    // ─────────────────────────────────────────────────────────────────────────

    private fun FootballMatch.buildScoreString(status: MatchStatus, startMs: Long): String {
        val home = score?.fullTime?.home
        val away = score?.fullTime?.away
        return when (status) {
            MatchStatus.LIVE -> if (home != null && away != null) "$home – $away" else "LIVE"
            MatchStatus.COMPLETED -> if (home != null && away != null) {
                val homeName = homeTeam?.tla ?: "H"
                val awayName = awayTeam?.tla ?: "A"
                "$homeName $home – $away $awayName"
            } else "FT"
            MatchStatus.UPCOMING  -> displayFormatter.format(startMs)
            MatchStatus.POSTPONED -> "Postponed"
            MatchStatus.CANCELLED -> "Cancelled"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status parsing
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseStatus(raw: String?): MatchStatus = when (raw?.uppercase()) {
        "LIVE", "IN_PLAY", "PAUSED" -> MatchStatus.LIVE
        "FINISHED", "AWARDED"       -> MatchStatus.COMPLETED
        "POSTPONED", "SUSPENDED"    -> MatchStatus.POSTPONED
        "CANCELLED"                 -> MatchStatus.CANCELLED
        else                        -> MatchStatus.UPCOMING
    }

    private fun parseUtcDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { utcParser.parse(raw)?.time }
            .getOrNull() ?: System.currentTimeMillis()
    }

    private fun deriveCountry(compCode: String): String = when (compCode.uppercase()) {
        "ISL" -> "India"
        "I-L" -> "India"
        else  -> "International"
    }
}
