package com.sportspulse.india.core.data.rss

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.sportspulse.india.core.domain.entity.NewsItem
import com.sportspulse.india.core.domain.entity.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses RSS / Atom feeds from Indian sports news sources using the Rome library.
 *
 * Supported feeds:
 *  - Cricbuzz Top Stories  — Cricket
 *  - Goal.com India        — Football
 *  - Khel Now              — Multi-sport
 *
 * Each feed is fetched on [Dispatchers.IO], parsed into [NewsItem] domain objects,
 * and sport-tagged using keyword matching.
 */
@Singleton
class RssFeedParser @Inject constructor() {

    companion object {
        private const val FEED_CRICBUZZ    = "https://www.cricbuzz.com/cricket-news/rss-feed"
        private const val FEED_GOAL_INDIA  = "https://www.goal.com/en-in/feeds/news?fmt=rss"
        private const val FEED_KHEL_NOW   = "https://khelnow.com/feed"
        private const val MAX_ITEMS        = 20
        private const val CONNECT_TIMEOUT  = 10_000  // ms
        private const val READ_TIMEOUT     = 15_000  // ms
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches and merges news from all configured RSS feeds.
     * Returns items sorted by [NewsItem.publishedAt] descending (newest first).
     */
    suspend fun fetchAllNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        val feeds = listOf(
            FEED_CRICBUZZ   to SportType.CRICKET,
            FEED_GOAL_INDIA to SportType.FOOTBALL,
            FEED_KHEL_NOW  to null   // multi-sport — classified per item
        )
        feeds.flatMap { (url, defaultSport) ->
            fetchFeed(url, defaultSport)
        }.sortedByDescending { it.publishedAt }
    }

    /**
     * Fetches news for a specific sport type.
     */
    suspend fun fetchNewsBySport(sport: SportType): List<NewsItem> = withContext(Dispatchers.IO) {
        val url = when (sport) {
            SportType.CRICKET    -> FEED_CRICBUZZ
            SportType.FOOTBALL   -> FEED_GOAL_INDIA
            else                 -> FEED_KHEL_NOW
        }
        fetchFeed(url, sport).filter { it.sport == sport || it.sport == null }
    }

    /**
     * Fetches a single RSS feed URL and returns parsed [NewsItem] list.
     */
    suspend fun fetchFeed(feedUrl: String, defaultSport: SportType?): List<NewsItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(feedUrl)
                val connection = url.openConnection().apply {
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout    = READ_TIMEOUT
                    setRequestProperty("User-Agent",
                        "SportsPulseIndia/1.0 (+https://sportspulseindia.app)")
                }
                val feed: SyndFeed = SyndFeedInput().build(XmlReader(connection.getInputStream()))
                feed.entries
                    .take(MAX_ITEMS)
                    .map { entry -> entry.toNewsItem(feedUrl, defaultSport) }
            }.onFailure { e ->
                Timber.e(e, "RssFeedParser: failed to fetch $feedUrl")
            }.getOrDefault(emptyList())
        }

    // ─────────────────────────────────────────────────────────────────────────
    // SyndEntry → NewsItem mapping
    // ─────────────────────────────────────────────────────────────────────────

    private fun SyndEntry.toNewsItem(feedUrl: String, defaultSport: SportType?): NewsItem {
        val id          = link ?: uri ?: title ?: System.currentTimeMillis().toString()
        val title       = title ?: "Untitled"
        val description = contents.firstOrNull()?.value
            ?: description?.value
            ?: ""
        val cleanDesc   = stripHtml(description).take(300)
        val imageUrl    = extractImageUrl(this)
        val publishedAt = (publishedDate ?: updatedDate ?: Date()).time
        val sourceName  = deriveFeedSourceName(feedUrl)
        val sport       = defaultSport ?: classifySport(title, cleanDesc)

        return NewsItem(
            id          = id,
            title       = title,
            description = cleanDesc,
            imageUrl    = imageUrl,
            sourceUrl   = link ?: feedUrl,
            sourceName  = sourceName,
            publishedAt = publishedAt,
            sport       = sport
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Removes HTML tags from text. */
    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()

    /**
     * Extracts first image URL from RSS entry — checks enclosures, media content,
     * and inline img tags in description.
     */
    private fun extractImageUrl(entry: SyndEntry): String? {
        // Check enclosures (common in podcast-style feeds)
        entry.enclosures?.firstOrNull { it.type?.startsWith("image") == true }?.let {
            return it.url
        }
        // Check media:content via foreign markup
        entry.foreignMarkup?.firstOrNull { it.name == "content" }?.let {
            return it.getAttributeValue("url")
        }
        // Extract from description HTML
        val desc = entry.description?.value ?: return null
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""").find(desc)
        return match?.groupValues?.getOrNull(1)
    }

    private fun deriveFeedSourceName(feedUrl: String): String = when {
        feedUrl.contains("cricbuzz")  -> "Cricbuzz"
        feedUrl.contains("goal.com")  -> "Goal India"
        feedUrl.contains("khelnow")   -> "Khel Now"
        else -> {
            val host = runCatching { URL(feedUrl).host }.getOrDefault(feedUrl)
            host.removePrefix("www.")
        }
    }

    /** Keyword-based sport classifier for multi-sport feeds (Khel Now). */
    private fun classifySport(title: String, desc: String): SportType? {
        val text = "$title $desc".lowercase()
        return when {
            text.containsAny("cricket", "ipl", "bcci", "test match", "odi", "t20i", "ranji") ->
                SportType.CRICKET
            text.containsAny("football", "isl", "fifa", "premier league", "soccer", "i-league") ->
                SportType.FOOTBALL
            text.containsAny("kabaddi", "pkl", "pro kabaddi") ->
                SportType.KABADDI
            text.containsAny("badminton", "bwf", "pbl", "sindhu", "saina") ->
                SportType.BADMINTON
            text.containsAny("hockey", "fih", "hil") ->
                SportType.HOCKEY
            text.containsAny("formula 1", "f1", "motogp", "inrc", "motorsport") ->
                SportType.MOTORSPORTS
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
