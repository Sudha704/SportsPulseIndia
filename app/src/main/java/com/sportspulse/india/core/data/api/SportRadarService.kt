package com.sportspulse.india.core.data.api

// import com.sportspulse.india.core.data.dto.SportRadarScheduleResponse
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query
// import retrofit2.http.Path

/**
 * Retrofit service interface for SportRadar API.
 * Base URL: https://api.sportradar.com/
 * Auth: api_key query param injected by OkHttp interceptor in [NetworkModule].
 *
 * Trial API provides access to:
 *  - Kabaddi (PKL)
 *  - Hockey schedules
 *  - Motorsports schedules
 *
 * Docs: https://developer.sportradar.com/docs/read/Home
 */
interface SportRadarService {

    /**
     * Fetches the kabaddi (PKL) daily schedule.
     * @param date  ISO 8601 date string, e.g. "2025-04-01".
     */
    // @GET("kabaddi/trial/v2/en/schedules/{date}/schedule.json")
    // suspend fun getKabaddiSchedule(
    //     @Path("date") date: String
    // ): SportRadarScheduleResponse

    // /**
    //  * Fetches the hockey daily schedule.
    //  */
    // @GET("hockey/trial/v2/en/schedules/{date}/schedule.json")
    // suspend fun getHockeySchedule(
    //     @Path("date") date: String
    // ): SportRadarScheduleResponse

    // /**
    //  * Fetches the Formula 1 season schedule.
    //  * @param year  Season year, e.g. 2025.
    //  */
    // @GET("formula1/trial/v2/en/sport_events.json")
    // suspend fun getFormula1Schedule(): SportRadarScheduleResponse

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "in",
        @Query("category") category: String = "sports"
    ): JsonElement
}
