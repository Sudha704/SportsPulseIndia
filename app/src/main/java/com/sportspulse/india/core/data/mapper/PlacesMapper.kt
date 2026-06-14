package com.sportspulse.india.core.data.mapper

import com.sportspulse.india.core.data.dto.NewPlaceResult
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

    fun NewPlaceResult.toDomain(
        userLat: Double,
        userLng: Double,
        travelTimeMinutes: Int = -1
    ): Venue {
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val distanceKm = haversine(userLat, userLng, lat, lng)
        val venueName  = displayName?.text ?: "Sports Venue"
        val types      = this.types ?: emptyList()

        return Venue(
            placeId           = id,
            name              = venueName,
            address           = formattedAddress ?: "",
            type              = classifyVenueType(venueName, types),
            distanceKm        = distanceKm,
            travelTimeMinutes = travelTimeMinutes,
            rating            = rating ?: 0f,
            reviewCount       = userRatingCount ?: 0,
            isOpenNow         = regularOpeningHours?.openNow ?: false,
            nextOpenTime      = null,
            availableSports   = inferSports(venueName, types),
            lat               = lat,
            lng               = lng,
            photoReference    = photos?.firstOrNull()?.name,
            phoneNumber       = nationalPhoneNumber,
            websiteUrl        = websiteUri,
            cachedAt          = System.currentTimeMillis()
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
