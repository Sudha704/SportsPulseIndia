package com.sportspulse.india.core.domain.entity

/**
 * Domain entity representing a sports venue discovered via Google Places API.
 *
 * Distance and travel time are computed client-side (Haversine) and then
 * refined via the Distance Matrix API.
 *
 * @param placeId            Google Places unique place ID.
 * @param name               Venue display name.
 * @param address            Full formatted address.
 * @param type               Category of the venue.
 * @param distanceKm         Straight-line (Haversine) distance from user in km.
 * @param travelTimeMinutes  Driving + transit ETA from Google Distance Matrix (-1 if unavailable).
 * @param rating             Google Places rating (0.0–5.0).
 * @param reviewCount        Total number of user reviews.
 * @param isOpenNow          Whether the venue is currently open.
 * @param nextOpenTime       Human-readable string for when the venue next opens (nullable).
 * @param availableSports    Sports playable at this venue.
 * @param lat                Latitude of the venue.
 * @param lng                Longitude of the venue.
 * @param photoReference     Google Places photo reference (used to load image via Places Photos API).
 * @param phoneNumber        Contact phone number (nullable).
 * @param websiteUrl         Venue website URL (nullable).
 * @param cachedAt           Epoch-ms when this record was last fetched (used for 2-hour TTL).
 */
data class Venue(
    val placeId: String,
    val name: String,
    val address: String,
    val type: VenueType,
    val distanceKm: Double,
    val travelTimeMinutes: Int,
    val rating: Float,
    val reviewCount: Int,
    val isOpenNow: Boolean,
    val nextOpenTime: String? = null,
    val availableSports: List<SportType>,
    val lat: Double,
    val lng: Double,
    val photoReference: String? = null,
    val phoneNumber: String? = null,
    val websiteUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
) {
    /** Google Maps directions deeplink URI. */
    val mapsDirectionsUri: String
        get() = "google.navigation:q=$lat,$lng&mode=d"

    /** Full Google Maps URL (web fallback). */
    val mapsWebUrl: String
        get() = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&destination_place_id=$placeId"

    /** True when cache age exceeds 2 hours (7 200 000 ms). */
    val isCacheStale: Boolean
        get() = (System.currentTimeMillis() - cachedAt) > 7_200_000L

    /** Display string: "1.2 km · ~8 min drive" */
    val distanceLabel: String
        get() {
            val kmStr = "%.1f km".format(distanceKm)
            return if (travelTimeMinutes > 0) "$kmStr · ~$travelTimeMinutes min drive"
            else kmStr
        }
}
