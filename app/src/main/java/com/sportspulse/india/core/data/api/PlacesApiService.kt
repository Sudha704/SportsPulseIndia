package com.sportspulse.india.core.data.api

import com.sportspulse.india.core.data.dto.PlacesNearbySearchResponse
import com.sportspulse.india.core.data.dto.PlaceDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Google Places API (legacy JSON).
 * Base URL: https://maps.googleapis.com/maps/api/
 * Auth: key query param injected by OkHttp interceptor in [NetworkModule].
 *
 * Docs: https://developers.google.com/maps/documentation/places/web-service
 */
interface PlacesApiService {

    /**
     * Nearby Search — finds venues within a radius.
     *
     * @param location   "lat,lng" string, e.g. "12.9716,77.5946"
     * @param radius     Radius in metres (max 50000). We convert km → m before calling.
     * @param type       Place type filter, e.g. "stadium"
     * @param keyword    Additional keyword, e.g. "turf" or "badminton court"
     * @param rankBy     "prominence" (default) or "distance"
     */
    @GET("place/nearbysearch/json")
    suspend fun nearbySearch(
        @Query("location")  location: String,
        @Query("radius")    radius: Int,
        @Query("type")      type: String? = null,
        @Query("keyword")   keyword: String? = null,
        @Query("rankby")    rankBy: String = "prominence",
        @Query("language")  language: String = "en"
    ): PlacesNearbySearchResponse

    /**
     * Place Details — fetches full info for a specific place.
     *
     * @param placeId  Google Places place_id
     * @param fields   Comma-separated field list to reduce billing
     */
    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields")   fields: String = "name,formatted_address,rating,user_ratings_total," +
                "opening_hours,photos,formatted_phone_number,website,geometry"
    ): PlaceDetailsResponse
}
