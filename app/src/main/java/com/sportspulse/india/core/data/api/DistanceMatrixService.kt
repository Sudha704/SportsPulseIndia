package com.sportspulse.india.core.data.api

import com.sportspulse.india.core.data.dto.DistanceMatrixResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Google Distance Matrix API.
 * Base URL: https://maps.googleapis.com/maps/api/
 * Auth: key query param injected by OkHttp interceptor in [NetworkModule].
 *
 * Used to compute actual driving + transit travel times after
 * the initial Haversine straight-line sort.
 *
 * Docs: https://developers.google.com/maps/documentation/distance-matrix
 */
interface DistanceMatrixService {

    /**
     * Computes travel time from one origin to multiple destinations.
     *
     * @param origins       "lat,lng" of the user's location.
     * @param destinations  Pipe-separated "lat,lng" strings for each venue.
     *                      Max 25 destinations per request.
     * @param mode          "driving" | "transit" | "walking"
     * @param units         "metric" (km) | "imperial" (miles)
     */
    @GET("distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins")       origins: String,
        @Query("destinations")  destinations: String,
        @Query("mode")          mode: String = "driving",
        @Query("units")         units: String = "metric",
        @Query("language")      language: String = "en-IN",
        @Query("region")        region: String = "in"
    ): DistanceMatrixResponse
}
