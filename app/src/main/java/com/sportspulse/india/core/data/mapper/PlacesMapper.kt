package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.dto.PlaceDetailResult
import com.sportspulse.india.core.data.dto.PlaceResult
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import com.sportspulse.india.core.domain.usecase.HaversineDistanceUseCase

/**
 * Maps Google Places API DTOs to [Venue] domain entities.
 *
 * VenueType classification is based on the Place's `types` array and `name` keyword matching.
 * Available sports are inferred from the same signals.
 */
object PlacesMapper {

    private val haversine = HaversineDistanceUseCase()

    // ─────────────────────────────────────────────────────────────────────────
    // Nearby Search result → Venue (lightweight, no travel time yet)
    // ─────────────────────────────────────────────────────────────────────────

    fun PlaceResult.toDomain(
        userLat: Double,
        userLng: Double,
        travelTimeMinutes: Int = -1
    ): Venue {
        val lat = geometry?.location?.lat ?: 0.0
        val lng = geometry?.location?.lng ?: 0.0
        val distanceKm = haversine(userLat, userLng, lat, lng)
        val venueName  = name ?: "Sports Venue"
        val types      = this.types ?: emptyList()

        return Venue(
            placeId           = placeId,
            name              = venueName,
            address           = vicinity ?: "",
            type              = classifyVenueType(venueName, types),
            distanceKm        = distanceKm,
            travelTimeMinutes = travelTimeMinutes,
            rating            = rating ?: 0f,
            reviewCount       = userRatingsTotal ?: 0,
            isOpenNow         = openingHours?.openNow ?: false,
            nextOpenTime      = null,
            availableSports   = inferSports(venueName, types),
            lat               = lat,
            lng               = lng,
            photoReference    = photos?.firstOrNull()?.photoReference,
            phoneNumber       = null,
            websiteUrl        = null,
            cachedAt          = System.currentTimeMillis()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Place Details result → enriched Venue
    // ─────────────────────────────────────────────────────────────────────────

    fun PlaceDetailResult.enrichVenue(existing: Venue): Venue {
        val weekdayText = openingHours?.weekdayText
        val nextOpenStr = if (openingHours?.openNow == false && !weekdayText.isNullOrEmpty()) {
            weekdayText.firstOrNull()
        } else null

        return existing.copy(
            address        = formattedAddress ?: existing.address,
            rating         = rating ?: existing.rating,
            reviewCount    = userRatingsTotal ?: existing.reviewCount,
            isOpenNow      = openingHours?.openNow ?: existing.isOpenNow,
            nextOpenTime   = nextOpenStr,
            photoReference = photos?.firstOrNull()?.photoReference ?: existing.photoReference,
            phoneNumber    = formattedPhoneNumber,
            websiteUrl     = website
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VenueType classification from Places types + name
    // ─────────────────────────────────────────────────────────────────────────

    fun classifyVenueType(name: String, types: List<String>): VenueType {
        val nameLower = name.lowercase()
        val typesLower = types.map { it.lowercase() }
        return when {
            nameLower.contains("badminton")                         -> VenueType.BADMINTON_COURT
            nameLower.contains("turf") || nameLower.contains("football")
                    || nameLower.contains("cricket")                -> VenueType.TURF
            nameLower.contains("pool") || nameLower.contains("swim") -> VenueType.SWIMMING_POOL
            nameLower.contains("indoor") || nameLower.contains("complex") -> VenueType.INDOOR_COMPLEX
            nameLower.contains("stadium")                           -> VenueType.STADIUM
            nameLower.contains("gym") || nameLower.contains("fitness") -> VenueType.GYM
            typesLower.contains("stadium")                          -> VenueType.STADIUM
            typesLower.contains("gym")                              -> VenueType.GYM
            typesLower.contains("sports_complex")                   -> VenueType.INDOOR_COMPLEX
            else                                                    -> VenueType.UNKNOWN
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Available sports inference
    // ─────────────────────────────────────────────────────────────────────────

    fun inferSports(name: String, types: List<String>): List<SportType> {
        val lower    = name.lowercase()
        val typeStr  = types.joinToString(" ").lowercase()
        val combined = "$lower $typeStr"
        val sports   = mutableListOf<SportType>()

        if (combined.contains("cricket") || combined.contains("net"))
            sports += SportType.CRICKET
        if (combined.contains("football") || combined.contains("turf") || combined.contains("soccer"))
            sports += SportType.FOOTBALL
        if (combined.contains("badminton"))
            sports += SportType.BADMINTON
        if (combined.contains("hockey"))
            sports += SportType.HOCKEY
        if (combined.contains("kabaddi"))
            sports += SportType.KABADDI
        if (sports.isEmpty() && (combined.contains("indoor") || combined.contains("complex")
                    || combined.contains("sports"))) {
            sports += SportType.CRICKET
            sports += SportType.FOOTBALL
            sports += SportType.BADMINTON
        }
        return sports
    }
}
