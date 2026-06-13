package com.sportspulse.india.core.domain.entity

/**
 * A news headline associated with a [SportEvent] or a general sports topic.
 *
 * Sourced from RSS feeds (Cricbuzz, Goal.com India, Khel Now) and
 * optionally enriched by the Gemini API.
 *
 * @param id          Unique identifier (usually the item's link URL).
 * @param title       Headline text.
 * @param description Short excerpt or summary.
 * @param imageUrl    Thumbnail image URL (nullable).
 * @param sourceUrl   Original article URL.
 * @param sourceName  Publisher name (e.g. "Cricbuzz", "Khel Now").
 * @param publishedAt Epoch-ms of publication.
 * @param sport       Optional sport association for filtering.
 */
data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val sourceUrl: String,
    val sourceName: String,
    val publishedAt: Long,
    val sport: SportType? = null
)
