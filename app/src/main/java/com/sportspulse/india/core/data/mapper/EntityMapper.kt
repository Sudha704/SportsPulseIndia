package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.db.entity.BroadcastScheduleEntity
import com.sportspulse.india.core.data.db.entity.MatchAlertEntity
import com.sportspulse.india.core.data.db.entity.SportEventEntity
import com.sportspulse.india.core.data.db.entity.VenueEntity
import com.sportspulse.india.core.domain.entity.Broadcast
import com.sportspulse.india.core.domain.entity.BroadcastPlatform
import com.sportspulse.india.core.domain.entity.HierarchyLevel
import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.MatchStatus
import com.sportspulse.india.core.domain.entity.NewsItem
import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Bidirectional mappers between Room database entities and domain objects.
 *
 * All serialisation of nested objects (broadcasts, news items, available sports)
 * uses Gson so Room only stores primitive types in columns.
 */
object EntityMapper {

    private val gson = Gson()

    // ─────────────────────────────────────────────────────────────────────────
    // SportEvent ↔ SportEventEntity
    // ─────────────────────────────────────────────────────────────────────────

    fun SportEventEntity.toDomain(broadcasts: List<Broadcast> = emptyList()): SportEvent {
        val newsType = object : TypeToken<List<NewsItemJson>>() {}.type
        val newsItems = runCatching<List<NewsItemJson>> {
            gson.fromJson(newsItemsJson, newsType)
        }.getOrDefault(emptyList())

        return SportEvent(
            id               = id,
            sport            = SportType.fromString(sport),
            title            = title,
            homeTeam         = homeTeam,
            awayTeam         = awayTeam,
            homeTeamLogoUrl  = homeTeamLogoUrl,
            awayTeamLogoUrl  = awayTeamLogoUrl,
            status           = MatchStatus.fromString(status),
            scoreOrTime      = scoreOrTime,
            venue            = venue,
            city             = city,
            country          = country,
            hierarchyLevel   = HierarchyLevel.fromString(hierarchyLevel),
            broadcasts       = broadcasts,
            startTimeIst     = startTimeIst,
            competition      = competition,
            seriesId         = seriesId,
            geminiSummary    = geminiSummary,
            newsItems        = newsItems.map { it.toDomain() },
            sourceUrl        = sourceUrl
        )
    }

    fun SportEvent.toEntity(): SportEventEntity {
        val newsJson = gson.toJson(newsItems.map { it.toJson() })
        return SportEventEntity(
            id               = id,
            sport            = sport.name,
            title            = title,
            homeTeam         = homeTeam,
            awayTeam         = awayTeam,
            homeTeamLogoUrl  = homeTeamLogoUrl,
            awayTeamLogoUrl  = awayTeamLogoUrl,
            status           = status.name,
            scoreOrTime      = scoreOrTime,
            venue            = venue,
            city             = city,
            country          = country,
            hierarchyLevel   = hierarchyLevel.name,
            broadcastsJson   = "[]",   // broadcasts stored in separate table
            startTimeIst     = startTimeIst,
            competition      = competition,
            seriesId         = seriesId,
            geminiSummary    = geminiSummary,
            newsItemsJson    = newsJson,
            sourceUrl        = sourceUrl,
            cachedAt         = System.currentTimeMillis()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Broadcast ↔ BroadcastScheduleEntity
    // ─────────────────────────────────────────────────────────────────────────

    fun BroadcastScheduleEntity.toDomain() = Broadcast(
        channelName    = channelName,
        channelNumber  = channelNumber,
        platform       = BroadcastPlatform.fromString(platform),
        isFree         = isFree,
        deeplinkUri    = deeplinkUri,
        streamingUrl   = streamingUrl
    )

    fun Broadcast.toEntity(eventId: String) = BroadcastScheduleEntity(
        eventId       = eventId,
        channelName   = channelName,
        channelNumber = channelNumber,
        platform      = platform.name,
        isFree        = isFree,
        deeplinkUri   = deeplinkUri,
        streamingUrl  = streamingUrl,
        cachedAt      = System.currentTimeMillis()
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Venue ↔ VenueEntity
    // ─────────────────────────────────────────────────────────────────────────

    fun VenueEntity.toDomain(): Venue {
        val sportsType = object : TypeToken<List<String>>() {}.type
        val sports = runCatching<List<String>> {
            gson.fromJson(availableSportsJson, sportsType)
        }.getOrDefault(emptyList())

        return Venue(
            placeId             = placeId,
            name                = name,
            address             = address,
            type                = VenueType.fromString(type),
            distanceKm          = distanceKm,
            travelTimeMinutes   = travelTimeMinutes,
            rating              = rating,
            reviewCount         = reviewCount,
            isOpenNow           = isOpenNow,
            nextOpenTime        = nextOpenTime,
            availableSports     = sports.map { SportType.fromString(it) },
            lat                 = lat,
            lng                 = lng,
            photoReference      = photoReference,
            phoneNumber         = phoneNumber,
            websiteUrl          = websiteUrl,
            cachedAt            = cachedAt
        )
    }

    fun Venue.toEntity() = VenueEntity(
        placeId             = placeId,
        name                = name,
        address             = address,
        type                = type.name,
        distanceKm          = distanceKm,
        travelTimeMinutes   = travelTimeMinutes,
        rating              = rating,
        reviewCount         = reviewCount,
        isOpenNow           = isOpenNow,
        nextOpenTime        = nextOpenTime,
        availableSportsJson = gson.toJson(availableSports.map { it.name }),
        lat                 = lat,
        lng                 = lng,
        photoReference      = photoReference,
        phoneNumber         = phoneNumber,
        websiteUrl          = websiteUrl,
        cachedAt            = System.currentTimeMillis()
    )

    // ─────────────────────────────────────────────────────────────────────────
    // MatchAlert ↔ MatchAlertEntity
    // ─────────────────────────────────────────────────────────────────────────

    fun MatchAlertEntity.toDomain() = MatchAlert(
        id                    = id,
        eventId               = eventId,
        eventTitle            = eventTitle,
        sport                 = SportType.fromString(sport),
        eventStartTimeIst     = eventStartTimeIst,
        reminderMinutesBefore = reminderMinutesBefore,
        isEnabled             = isEnabled,
        workRequestId         = workRequestId
    )

    fun MatchAlert.toEntity() = MatchAlertEntity(
        id                    = id,
        eventId               = eventId,
        eventTitle            = eventTitle,
        sport                 = sport.name,
        eventStartTimeIst     = eventStartTimeIst,
        reminderMinutesBefore = reminderMinutesBefore,
        isEnabled             = isEnabled,
        workRequestId         = workRequestId
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Internal JSON helper for NewsItem serialisation
    // ─────────────────────────────────────────────────────────────────────────

    private data class NewsItemJson(
        val id: String,
        val title: String,
        val description: String,
        val imageUrl: String?,
        val sourceUrl: String,
        val sourceName: String,
        val publishedAt: Long,
        val sport: String?
    )

    private fun NewsItemJson.toDomain() = NewsItem(
        id          = id,
        title       = title,
        description = description,
        imageUrl    = imageUrl,
        sourceUrl   = sourceUrl,
        sourceName  = sourceName,
        publishedAt = publishedAt,
        sport       = sport?.let { SportType.fromString(it) }
    )

    private fun NewsItem.toJson() = NewsItemJson(
        id          = id,
        title       = title,
        description = description,
        imageUrl    = imageUrl,
        sourceUrl   = sourceUrl,
        sourceName  = sourceName,
        publishedAt = publishedAt,
        sport       = sport?.name
    )
}
