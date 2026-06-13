package com.sportspulse.india.core.data.api

import com.sportspulse.india.core.data.dto.CricApiCurrentMatchesResponse
import com.sportspulse.india.core.data.dto.CricApiMatchInfoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for CricAPI v1.
 * Base URL: https://api.cricapi.com/v1/
 * Auth: apikey query param injected by OkHttp interceptor in [NetworkModule].
 *
 * Docs: https://cricapi.com/how-to-use/
 */
interface CricApiService {

    /**
     * Fetches currently live and upcoming cricket matches.
     * @param offset  Pagination offset (default 0).
     */
    @GET("currentMatches")
    suspend fun getCurrentMatches(
        @Query("offset") offset: Int = 0
    ): CricApiCurrentMatchesResponse

    /**
     * Fetches detailed match information including score card.
     * @param id  CricAPI match ID.
     */
    @GET("match_info")
    suspend fun getMatchInfo(
        @Query("id") id: String
    ): CricApiMatchInfoResponse
}
