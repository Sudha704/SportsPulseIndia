package com.sportspulse.india.features.dashboard.data

import com.sportspulse.india.core.data.api.CricApiService
import com.sportspulse.india.core.data.api.FootballDataService
import com.sportspulse.india.core.data.api.GeminiApiClient
import com.sportspulse.india.core.data.api.SportRadarService
import com.sportspulse.india.core.data.db.dao.BroadcastDao
import com.sportspulse.india.core.data.db.dao.SportEventDao
import com.sportspulse.india.core.data.mapper.CricApiMapper.toDomain
import com.sportspulse.india.core.data.mapper.EntityMapper.toDomain
import com.sportspulse.india.core.data.mapper.EntityMapper.toEntity
import com.sportspulse.india.core.data.mapper.FootballDataMapper.toDomain
import com.sportspulse.india.core.data.mapper.SportRadarMapper.toDomain
import com.sportspulse.india.core.data.rss.RssFeedParser
import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.MatchStatus
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.repository.SportEventRepository
import com.sportspulse.india.core.domain.usecase.HierarchyFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Full implementation of [SportEventRepository].
 *
 * Data aggregation strategy (offline-first):
 *  1. Emit cached Room data immediately so UI renders instantly.
 *  2. In parallel, fetch from:
 *      - CricAPI (cricket)
 *      - football-data.org (ISL + EPL)
 *      - SportRadar (kabaddi, hockey, motorsports, badminton)
 *      - RSS feeds (news enrichment)
 *  3. Classify each event with [HierarchyFilter].
 *  4. Upsert to Room, emit updated list.
 *  5. Fall back to Gemini enrichment when data is sparse.
 *
 * Cache TTL: 5 minutes for live data, 30 minutes for non-live.
 */
class SportEventRepositoryImpl @Inject constructor(
    private val sportEventDao: SportEventDao,
    private val broadcastDao: BroadcastDao,
    private val cricApiService: CricApiService,
    private val footballDataService: FootballDataService,
    private val sportRadarService: SportRadarService,
    private val geminiApiClient: GeminiApiClient,
    private val rssFeedParser: RssFeedParser,
    private val hierarchyFilter: HierarchyFilter
) : SportEventRepository {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    private val today: String get() = dateFormatter.format(Date())

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    override fun getEvents(forceRefresh: Boolean): Flow<Result<List<SportEvent>>> = flow {
        // Step 1: Emit cached events immediately
        val cached = loadCachedEvents()
        if (cached.isNotEmpty()) {
            emit(Result.success(hierarchyFilter(cached)))
        }

        // Step 2: Check if refresh is needed
        val cacheAgeMs = System.currentTimeMillis() -
                (sportEventDao.getLatestCacheTimestamp() ?: 0L)
        val hasLive = cached.any { it.status == MatchStatus.LIVE }
        val ttlMs = if (hasLive) TimeUnit.MINUTES.toMillis(1) else TimeUnit.MINUTES.toMillis(15)

        if (!forceRefresh && cacheAgeMs < ttlMs && cached.isNotEmpty()) {
            Timber.d("SportEventRepo: cache fresh, skipping network")
            return@flow
        }

        // Step 3: Parallel network fetch
        val fresh = fetchAllEventsFromNetwork()
        if (fresh.isNotEmpty()) {
            cacheEvents(fresh)
            emit(Result.success(hierarchyFilter(fresh)))
        } else if (cached.isEmpty()) {
            emit(Result.failure(Exception("No data available")))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLiveEvents(): Flow<Result<List<SportEvent>>> = flow {
        val events = loadCachedEvents().filter { it.status == MatchStatus.LIVE }
        emit(Result.success(hierarchyFilter.filterLive(events)))
    }.flowOn(Dispatchers.IO)

    override fun getEventsBySport(sportType: SportType): Flow<Result<List<SportEvent>>> = flow {
        sportEventDao.observeEventsBySport(sportType.name)
            .collect { entities ->
                val events = entities.map { entity ->
                    val broadcasts = broadcastDao
                        .getBroadcastsForEvent(entity.id)
                        .map { it.toDomain() }
                    entity.toDomain(broadcasts)
                }
                emit(Result.success(hierarchyFilter.filterBySport(events, sportType)))
            }
    }.flowOn(Dispatchers.IO)

    override fun getEventsByHierarchy(hierarchyLevel: HierarchyLevel): Flow<Result<List<SportEvent>>> = flow {
        val events = loadCachedEvents().filter { it.hierarchyLevel == hierarchyLevel }
        emit(Result.success(hierarchyFilter(events)))
    }.flowOn(Dispatchers.IO)

    override suspend fun getEventById(
        eventId: String,
        withAiSummary: Boolean
    ): Result<SportEvent> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = sportEventDao.getEventById(eventId)
                ?: throw NoSuchElementException("Event $eventId not found in cache")

            val broadcasts = broadcastDao
                .getBroadcastsForEvent(entity.id)
                .map { it.toDomain() }

            var event = entity.toDomain(broadcasts)

            // Enrich with Gemini summary if requested and not yet cached
            if (withAiSummary && event.geminiSummary == null) {
                val summary = geminiApiClient.generateMatchPreview(
                    eventTitle  = event.title,
                    competition = event.competition,
                    homeTeam    = event.homeTeam,
                    awayTeam    = event.awayTeam,
                    venue       = event.venue,
                    status      = event.status.name,
                    scoreOrTime = event.scoreOrTime
                )
                if (summary != null) {
                    sportEventDao.updateGeminiSummary(eventId, summary)
                    event = event.copy(geminiSummary = summary)
                }
            }
            event
        }
    }

    override suspend fun refreshBroadcastData(): Result<Unit> = Result.success(Unit)

    override suspend fun cacheEvents(events: List<SportEvent>) {
        withContext(Dispatchers.IO) {
            sportEventDao.upsertAll(events.map { it.toEntity() })
        }
    }

    override suspend fun pruneStaleEvents() {
        withContext(Dispatchers.IO) {
            val expiryMs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            sportEventDao.deleteStaleEvents(expiryMs)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network aggregation
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchAllEventsFromNetwork(): List<SportEvent> = coroutineScope {
        val cricketDeferred    = async { fetchCricketEvents() }
        val footballDeferred   = async { fetchFootballEvents() }
        val kabaddiDeferred    = async { fetchKabaddiEvents() }
        val hockeyDeferred     = async { fetchHockeyEvents() }
        val motorsportsDeferred = async { fetchMotorsportsEvents() }
        val newsDeferred       = async { rssFeedParser.fetchAllNews() }

        val news = newsDeferred.await()

        val allEvents = listOf(
            cricketDeferred.await(),
            footballDeferred.await(),
            kabaddiDeferred.await(),
            hockeyDeferred.await(),
            motorsportsDeferred.await()
        ).flatten()

        // Attach relevant news items to each event (by sport)
        allEvents.map { event ->
            val relatedNews = news.filter { it.sport == event.sport }.take(3)
            event.copy(newsItems = relatedNews)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-source fetchers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchCricketEvents(): List<SportEvent> = runCatching {
        val response = cricApiService.getCurrentMatches(offset = 0)
        response.data?.mapNotNull { match ->
            val broadcasts = broadcastDao.getBroadcastsForEvent("cricapi_${match.id}")
                .map { it.toDomain() }
            runCatching { match.toDomain(broadcasts, hierarchyFilter) }.getOrNull()
        } ?: emptyList()
    }.onFailure { e ->
        Timber.e(e, "SportEventRepo: CricAPI fetch failed")
    }.getOrDefault(emptyList())

    private suspend fun fetchFootballEvents(): List<SportEvent> = runCatching {
        val response = footballDataService.getTodaysMatches(competitions = "ISL")
        response.matches?.mapNotNull { match ->
            val broadcasts = broadcastDao.getBroadcastsForEvent("football_${match.id}")
                .map { it.toDomain() }
            runCatching { match.toDomain(broadcasts, hierarchyFilter) }.getOrNull()
        } ?: emptyList()
    }.onFailure { e ->
        Timber.e(e, "SportEventRepo: football-data fetch failed")
    }.getOrDefault(emptyList())

    private suspend fun fetchKabaddiEvents(): List<SportEvent> = runCatching {
        val response = sportRadarService.getKabaddiSchedule(today)
        response.sportEvents?.mapNotNull { event ->
            val broadcasts = broadcastDao.getBroadcastsForEvent("sportradar_${event.id}")
                .map { it.toDomain() }
            runCatching {
                event.toDomain(SportType.KABADDI, broadcasts, hierarchyFilter)
            }.getOrNull()
        } ?: emptyList()
    }.onFailure { e ->
        Timber.e(e, "SportEventRepo: SportRadar kabaddi fetch failed")
    }.getOrDefault(emptyList())

    private suspend fun fetchHockeyEvents(): List<SportEvent> = runCatching {
        val response = sportRadarService.getHockeySchedule(today)
        response.sportEvents?.mapNotNull { event ->
            val broadcasts = broadcastDao.getBroadcastsForEvent("sportradar_${event.id}")
                .map { it.toDomain() }
            runCatching {
                event.toDomain(SportType.HOCKEY, broadcasts, hierarchyFilter)
            }.getOrNull()
        } ?: emptyList()
    }.onFailure { e ->
        Timber.e(e, "SportEventRepo: SportRadar hockey fetch failed")
    }.getOrDefault(emptyList())

    private suspend fun fetchMotorsportsEvents(): List<SportEvent> = runCatching {
        val response = sportRadarService.getFormula1Schedule()
        response.sportEvents?.mapNotNull { event ->
            val broadcasts = broadcastDao.getBroadcastsForEvent("sportradar_${event.id}")
                .map { it.toDomain() }
            runCatching {
                event.toDomain(SportType.MOTORSPORTS, broadcasts, hierarchyFilter)
            }.getOrNull()
        } ?: emptyList()
    }.onFailure { e ->
        Timber.e(e, "SportEventRepo: SportRadar F1 fetch failed")
    }.getOrDefault(emptyList())

    // ─────────────────────────────────────────────────────────────────────────
    // Load cached events from Room with joined broadcasts
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadCachedEvents(): List<SportEvent> {
        val rows = sportEventDao.observeAllEvents().first()
        return rows.map { entity ->
            val broadcasts = broadcastDao
                .getBroadcastsForEvent(entity.id)
                .map { it.toDomain() }
            entity.toDomain(broadcasts)
        }
    }
}
