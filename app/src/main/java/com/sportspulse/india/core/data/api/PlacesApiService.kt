package com.sportspulse.india.core.data.api

import com.sportspulse.india.core.data.dto.NewPlacesSearchRequest
import com.sportspulse.india.core.data.dto.NewPlacesSearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for Google Places API (New).
 * Base URL: https://places.googleapis.com/
 *
 * Docs: https://developers.google.com/maps/documentation/places/web-service/text-search
 */
interface PlacesApiService {

    @POST("v1/places:searchText")
    suspend fun searchText(
        @Body request: NewPlacesSearchRequest
    ): NewPlacesSearchResponse
}
