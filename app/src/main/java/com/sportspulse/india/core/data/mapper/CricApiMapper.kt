package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.dto.CricApiMatch
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
 * Maps [CricApiMatch] DTOs to [SportEvent] domain entities.
 *
 * CricAPI status strings → [MatchStatus]:
 *  "Match not started"  → UPCOMING
 *  "live" / "In Progress" → LIVE
 *  "result" / "Match over" → COMPLETED
 *  "abandoned"           → CANCELLED
 */
object CricApiMapper {

    private val IST = TimeZone.getTimeZone("Asia/Kolkata")
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = IST
    }
    private val displayFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.US).apply {
        timeZone = IST
    }

    fun CricApiMatch.toDomain(
        broadcasts: List<Broadcast>,
        hierarchyFilter: HierarchyFilter
    ): SportEvent {
        val homeTeam = teams?.getOrNull(0) ?: "Team A"
        val awayTeam = teams?.getOrNull(1) ?: "Team B"
        val homeLogo = teamInfo?.firstOrNull { it.name == homeTeam }?.img
        val awayLogo = teamInfo?.firstOrNull { it.name == awayTeam }?.img

        val matchStatus = parseStatus(status)
        val startMs     = parseStartTime(dateTimeGmt ?: date)
        val scoreStr    = buildScoreString(matchStatus, startMs)

        val competition = deriveCompetition(name, matchType)
        val country     = deriveCountry(name, homeTeam, awayTeam)

        val event = SportEvent(
            id              = "cricapi_$id",
            sport           = SportType.CRICKET,
            title           = buildTitle(homeTeam, awayTeam, name),
            homeTeam        = homeTeam,
            awayTeam        = awayTeam,
            homeTeamLogoUrl = homeLogo,
            awayTeamLogoUrl = awayLogo,
            status          = matchStatus,
            scoreOrTime     = scoreStr,
            venue           = venue ?: "TBC",
            city            = "",
            country         = country,
            hierarchyLevel  = HierarchyLevel.LOCAL, // overwritten below
            broadcasts      = broadcasts,
            startTimeIst    = startMs,
            competition     = competition,
            seriesId        = seriesId,
            sourceUrl       = "https://cricapi.com/series/$seriesId"
        )
        return event.copy(hierarchyLevel = hierarchyFilter.classify(event))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Score string builder
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildScoreString(status: MatchStatus, startMs: Long): String =
        when (status) {
            MatchStatus.UPCOMING  -> displayFormatter.format(startMs)
            MatchStatus.COMPLETED -> "Match Completed"
            MatchStatus.LIVE      -> "LIVE"
            MatchStatus.POSTPONED -> "Postponed"
            MatchStatus.CANCELLED -> "Abandoned"
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Status parsing
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseStatus(raw: String?): MatchStatus {
        val s = raw?.lowercase() ?: return MatchStatus.UPCOMING
        return when {
            s.contains("live")        || s.contains("in progress") -> MatchStatus.LIVE
            s.contains("result")      || s.contains("over")        -> MatchStatus.COMPLETED
            s.contains("abandon")     || s.contains("cancel")      -> MatchStatus.CANCELLED
            s.contains("postpone")    || s.contains("suspend")     -> MatchStatus.POSTPONED
            s.contains("not started") || s.contains("scheduled")   -> MatchStatus.UPCOMING
            else -> MatchStatus.UPCOMING
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Time parsing (ISO UTC → epoch ms)
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseStartTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { isoParser.parse(raw)?.time }
            .getOrNull()
            ?: runCatching { dateParser.parse(raw)?.time }
                .getOrNull()
            ?: System.currentTimeMillis()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title builder — produce "India vs Australia" from name or teams
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildTitle(home: String, away: String, rawName: String): String {
        // CricAPI name format: "India vs Australia, 4th Test"
        if (rawName.contains("vs", ignoreCase = true)) return rawName
        return "$home vs $away"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Derive competition from match name + type
    // ─────────────────────────────────────────────────────────────────────────

    private fun deriveCompetition(name: String, matchType: String?): String {
        val upper = name.uppercase()
        return when {
            upper.contains("IPL")       -> "IPL 2025"
            upper.contains("RANJI")     -> "Ranji Trophy 2024-25"
            upper.contains("DULEEP")    -> "Duleep Trophy 2024-25"
            upper.contains("VIJAY")     -> "Vijay Hazare Trophy"
            upper.contains("MUSHTAQ")   -> "Syed Mushtaq Ali Trophy"
            upper.contains("WORLD CUP") -> "ICC World Cup"
            upper.contains("ICC")       -> "ICC Tournament"
            upper.contains("T20I")      -> "T20I Series"
            upper.contains("ODI")       -> "ODI Series"
            upper.contains("TEST")      -> "Test Match Series"
            matchType != null           -> "${matchType.uppercase()} Match"
            else                        -> "Cricket"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Derive country for hierarchy classification
    // ─────────────────────────────────────────────────────────────────────────

    private val INDIA_KEYWORDS = setOf("INDIA", "IND", "BCCI")
    private val DOMESTIC_COMPETITIONS = setOf("IPL", "RANJI", "DULEEP", "VIJAY HAZARE", "MUSHTAQ")

    private fun deriveCountry(name: String, home: String, away: String): String {
        val upper = name.uppercase()
        // Domestic IPL / Ranji — played in India
        if (DOMESTIC_COMPETITIONS.any { upper.contains(it) }) return "India"
        // International: one team is India, other is foreign
        val homeUp = home.uppercase()
        val awayUp = away.uppercase()
        if (INDIA_KEYWORDS.any { homeUp.contains(it) } &&
            !INDIA_KEYWORDS.any { awayUp.contains(it) }) return "India"
        if (INDIA_KEYWORDS.any { awayUp.contains(it) } &&
            !INDIA_KEYWORDS.any { homeUp.contains(it) }) return "Various"
        return "India"
    }
}
