package com.sportspulse.india.core.data.api

// import com.sportspulse.india.core.data.dto.FootballMatchesResponse
import com.google.gson.JsonElement
import retrofit2.http.GET
// import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for football-data.org API v4.
 * Base URL: https://api.football-data.org/v4/
 * Auth: X-Auth-Token header injected by OkHttp interceptor in [NetworkModule].
 *
 * Competitions tracked:
 *  - ISL (Indian Super League) — code: ISL
 *  - EPL (English Premier League) — code: PL  (subset)
 *
 * Docs: https://www.football-data.org/documentation/api
 */
interface FootballDataService {

    /**
     * Fetches scheduled / live matches for a competition.
     * @param competitionCode  e.g. "ISL", "PL"
     * @param status           Filter: "SCHEDULED", "LIVE", "FINISHED", "IN_PLAY"
     */
    // @GET("competitions/{competitionCode}/matches")
    // suspend fun getMatchesByCompetition(
    //     @Path("competitionCode") competitionCode: String,
    //     @Query("status") status: String? = null,
    //     @Query("dateFrom") dateFrom: String? = null,
    //     @Query("dateTo")   dateTo: String? = null
    // ): FootballMatchesResponse

    // /**
    //  * Fetches today's matches across all tracked competitions.
    //  */
    // @GET("matches")
    // suspend fun getTodaysMatches(
    //     @Query("competitions") competitions: String = "ISL,PL"
    // ): FootballMatchesResponse

    @GET("leagues")
    suspend fun getLeagues(
        @Query("country") country: String = "India"
    ): JsonElement
}
