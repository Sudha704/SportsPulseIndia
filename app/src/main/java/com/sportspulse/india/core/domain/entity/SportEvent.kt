package com.sportspulse.india.core.domain.entity

/**
 * Core domain entity representing a single sporting event (match / race / session).
 *
 * All timestamps are stored in epoch-milliseconds (IST = UTC+5:30 offset applied at display layer).
 *
 * @param id              Unique event identifier (source-prefixed, e.g. "cricapi_12345").
 * @param sport           The sport category.
 * @param title           Short display title, e.g. "India vs Australia – 3rd ODI".
 * @param homeTeam        Home team / entry 1 name.
 * @param awayTeam        Away team / entry 2 name.
 * @param homeTeamLogoUrl URL of the home team logo (nullable).
 * @param awayTeamLogoUrl URL of the away team logo (nullable).
 * @param status          Current match status.
 * @param scoreOrTime     Live score string or scheduled time string (IST formatted).
 * @param venue           Venue/circuit name.
 * @param city            Host city.
 * @param country         Host country.
 * @param hierarchyLevel  Computed hierarchy tier used for feed ranking.
 * @param broadcasts      List of telecast / streaming slots.
 * @param startTimeIst    Event start epoch-ms in IST.
 * @param competition     Tournament / series name (e.g. "IPL 2025", "FIH World Cup").
 * @param seriesId        Optional parent series identifier for grouping.
 * @param geminiSummary   AI-generated match preview or analysis (lazy-loaded on detail).
 * @param newsItems       Latest news headlines associated with this event.
 * @param sourceUrl       Link to the original data source page.
 */
data class SportEvent(
    val id: String,
    val sport: SportType,
    val title: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogoUrl: String? = null,
    val awayTeamLogoUrl: String? = null,
    val status: MatchStatus,
    val scoreOrTime: String,
    val venue: String,
    val city: String = "",
    val country: String = "India",
    val hierarchyLevel: HierarchyLevel,
    val broadcasts: List<Broadcast>,
    val startTimeIst: Long,
    val competition: String,
    val seriesId: String? = null,
    val geminiSummary: String? = null,
    val newsItems: List<NewsItem> = emptyList(),
    val sourceUrl: String? = null
) {
    /** True when the event is currently in progress. */
    val isLive: Boolean get() = status == MatchStatus.LIVE

    /** True when broadcasts contain at least one free option. */
    val hasFreeStream: Boolean get() = broadcasts.any { it.isFree }
}
