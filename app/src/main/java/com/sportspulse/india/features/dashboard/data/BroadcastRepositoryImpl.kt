package com.sportspulse.india.features.dashboard.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sportspulse.india.BuildConfig
import com.sportspulse.india.core.data.api.GeminiApiClient
import com.sportspulse.india.core.data.api.GistConfigService
import com.sportspulse.india.core.data.db.dao.BroadcastDao
import com.sportspulse.india.core.data.mapper.EntityMapper.toDomain
import com.sportspulse.india.core.data.mapper.EntityMapper.toEntity
import com.sportspulse.india.core.domain.entity.Broadcast
import com.sportspulse.india.core.domain.entity.BroadcastPlatform
import com.sportspulse.india.core.domain.repository.BroadcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Full implementation of [BroadcastRepository].
 *
 * 3-tier data strategy:
 *  1. **Remote Gist** — [GistConfigService] fetches broadcast_schedule.json every 6 h.
 *  2. **Room cache** — `broadcast_schedule` table (offline fallback).
 *  3. **Gemini API** — structured JSON prompt as last resort.
 *
 * Broadcast schedule JSON shape (hosted on GitHub Gist):
 * ```json
 * {
 *   "eventId": [
 *     {
 *       "channelName": "Star Sports 1 HD",
 *       "channelNumber": 451,
 *       "platform": "JIOSTAR",
 *       "isFree": false,
 *       "deeplinkUri": "hotstar://sports",
 *       "streamingUrl": "https://www.hotstar.com/in/sports"
 *     }
 *   ]
 * }
 * ```
 */
class BroadcastRepositoryImpl @Inject constructor(
    private val broadcastDao: BroadcastDao,
    private val gistConfigService: GistConfigService,
    private val geminiApiClient: GeminiApiClient,
    private val gson: Gson
) : BroadcastRepository {

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    override fun getBroadcastsForEvent(eventId: String): Flow<Result<List<Broadcast>>> = flow {
        // 1. Emit Room cache immediately
        val cached = broadcastDao.getBroadcastsForEvent(eventId).map { it.toDomain() }
        if (cached.isNotEmpty()) {
            emit(Result.success(cached))
        }

        // 2. Check if remote refresh is needed (6-hour TTL)
        val lastRefresh = broadcastDao.getLastRefreshTimestamp() ?: 0L
        val stale = System.currentTimeMillis() - lastRefresh > TimeUnit.HOURS.toMillis(6)

        if (stale) {
            val remoteResult = refreshRemoteConfig()
            if (remoteResult.isSuccess) {
                val fresh = broadcastDao.getBroadcastsForEvent(eventId).map { it.toDomain() }
                emit(Result.success(fresh))
                return@flow
            }
        }

        // 3. If still empty, try Gemini as last resort
        if (cached.isEmpty()) {
            Timber.w("BroadcastRepo: no cached data for $eventId, trying Gemini")
            val geminiResult = inferBroadcastsViaGemini(eventId, "")
            if (geminiResult.isSuccess) {
                val geminiList = geminiResult.getOrDefault(emptyList())
                cacheBroadcasts(eventId, geminiList)
                emit(Result.success(geminiList))
            } else {
                emit(Result.success(defaultBroadcasts()))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun refreshRemoteConfig(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = gistConfigService.getRawJson(BuildConfig.BROADCAST_CONFIG_GIST_URL)
            val scheduleMap = parseGistJson(rawJson)
            scheduleMap.forEach { (eventId, broadcasts) ->
                broadcastDao.deleteByEventId(eventId)
                broadcastDao.upsertAll(broadcasts.map { it.toEntity(eventId) })
            }
            Timber.d("BroadcastRepo: remote config refreshed, ${scheduleMap.size} events")
        }.onFailure { e ->
            Timber.e(e, "BroadcastRepo: remote config refresh failed")
        }
    }

    override suspend fun inferBroadcastsViaGemini(
        eventTitle: String,
        sport: String
    ): Result<List<Broadcast>> = withContext(Dispatchers.IO) {
        runCatching {
            geminiApiClient.inferBroadcasts(eventTitle, sport)
        }.onFailure { e ->
            Timber.e(e, "BroadcastRepo: Gemini inference failed")
        }
    }

    override suspend fun cacheBroadcasts(eventId: String, broadcasts: List<Broadcast>) {
        withContext(Dispatchers.IO) {
            broadcastDao.deleteByEventId(eventId)
            broadcastDao.upsertAll(broadcasts.map { it.toEntity(eventId) })
        }
    }

    override suspend fun getLastRefreshTimestamp(): Long =
        broadcastDao.getLastRefreshTimestamp() ?: 0L

    // ─────────────────────────────────────────────────────────────────────────
    // Gist JSON parsing
    // ─────────────────────────────────────────────────────────────────────────

    /** Parses `{ "eventId": [ BroadcastJson... ] }` structure from Gist. */
    private fun parseGistJson(raw: String): Map<String, List<Broadcast>> {
        return runCatching {
            val mapType = object : TypeToken<Map<String, List<GistBroadcastDto>>>() {}.type
            val rawMap: Map<String, List<GistBroadcastDto>> = gson.fromJson(raw, mapType)
            rawMap.mapValues { (_, dtos) -> dtos.mapNotNull { it.toDomain() } }
        }.onFailure { e ->
            Timber.e(e, "BroadcastRepo: Gist JSON parse error")
        }.getOrDefault(emptyMap())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Default fallback broadcasts (DD Sports is always free)
    // ─────────────────────────────────────────────────────────────────────────

    private fun defaultBroadcasts(): List<Broadcast> = listOf(
        Broadcast(
            channelName   = "DD Sports (Free)",
            channelNumber = 605,
            platform      = BroadcastPlatform.DD_SPORTS,
            isFree        = true
        ),
        Broadcast(
            channelName   = "JioStar Sports",
            platform      = BroadcastPlatform.JIOSTAR,
            isFree        = false,
            deeplinkUri   = BroadcastPlatform.JIOSTAR.defaultDeeplink
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Internal DTO for Gist JSON
    // ─────────────────────────────────────────────────────────────────────────

    private data class GistBroadcastDto(
        val channelName: String?,
        val channelNumber: Int?,
        val platform: String?,
        val isFree: Boolean?,
        val deeplinkUri: String?,
        val streamingUrl: String?
    ) {
        fun toDomain(): Broadcast? {
            val name = channelName ?: return null
            val plat = BroadcastPlatform.fromString(platform ?: "UNKNOWN")
            return Broadcast(
                channelName   = name,
                channelNumber = channelNumber,
                platform      = plat,
                isFree        = isFree ?: plat.isFreeByDefault,
                deeplinkUri   = deeplinkUri ?: plat.defaultDeeplink,
                streamingUrl  = streamingUrl
            )
        }
    }
}
