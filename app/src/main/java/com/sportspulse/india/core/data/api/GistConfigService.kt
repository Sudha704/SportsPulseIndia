package com.sportspulse.india.core.data.api

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit service interface for fetching the broadcast schedule JSON
 * from a GitHub Gist (or any raw URL).
 *
 * Uses [ScalarsConverterFactory] so the body is returned as a raw [String],
 * allowing the repository to parse it with Gson manually.
 *
 * The full Gist URL is configured via [BuildConfig.BROADCAST_CONFIG_GIST_URL]
 * and passed to [getRawJson] as a dynamic URL.
 */
interface GistConfigService {

    /**
     * Fetches the raw JSON content of the broadcast schedule Gist.
     * @param url  Full URL to the raw Gist content.
     */
    @GET
    suspend fun getRawJson(@Url url: String): String
}
