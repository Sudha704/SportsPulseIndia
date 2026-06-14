package com.sportspulse.india.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sportspulse.india.BuildConfig
import com.sportspulse.india.core.data.api.CricApiService
import com.sportspulse.india.core.data.api.FootballDataService
import com.sportspulse.india.core.data.api.GistConfigService
import com.sportspulse.india.core.data.api.PlacesApiService
import com.sportspulse.india.core.data.api.SportRadarService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    // private const val BASE_URL_CRICAPI          = "https://api.cricapi.com/v1/"
    // private const val BASE_URL_SPORTRADAR       = "https://api.sportradar.com/"
    // private const val BASE_URL_FOOTBALL_DATA    = "https://api.football-data.org/v4/"
    private const val BASE_URL_CRICAPI          = "https://newsdata.io/api/1/"
    private const val BASE_URL_SPORTRADAR       = "https://newsapi.org/v2/"
    private const val BASE_URL_FOOTBALL_DATA    = "https://v3.football.api-sports.io/"
    private const val BASE_URL_PLACES           = "https://places.googleapis.com/"
    private const val BASE_URL_GIST             = "https://gist.githubusercontent.com/"

    private const val TIMEOUT_SECONDS           = 30L

    // ─────────────────────────────────────────────────────────────────────────
    // Gson
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()

    // ─────────────────────────────────────────────────────────────────────────
    // Shared OkHttpClient builder (logging + timeouts)
    // ─────────────────────────────────────────────────────────────────────────

    private fun baseOkHttpClient(vararg interceptors: Interceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        interceptors.forEach { builder.addInterceptor(it) }

        if (BuildConfig.ENABLE_LOGGING) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CricAPI client
    // Docs: https://cricapi.com/how-to-use/
    // Auth: ?apikey=<key> query param on every request
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("cricapi")
    fun provideCricApiOkHttp(): OkHttpClient =
        baseOkHttpClient(
            Interceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("apikey", BuildConfig.CRICAPI_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
        )

    @Provides
    @Singleton
    fun provideCricApiService(
        @Named("cricapi") okHttpClient: OkHttpClient,
        gson: Gson
    ): CricApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL_CRICAPI)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CricApiService::class.java)

    // ─────────────────────────────────────────────────────────────────────────
    // SportRadar client
    // Docs: https://developer.sportradar.com
    // Auth: api_key query param
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("sportradar")
    fun provideSportRadarOkHttp(): OkHttpClient =
        baseOkHttpClient(
            Interceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    // .addQueryParameter("api_key", BuildConfig.SPORTRADAR_API_KEY)
                    .addQueryParameter("apiKey", BuildConfig.SPORTRADAR_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
        )

    @Provides
    @Singleton
    fun provideSportRadarService(
        @Named("sportradar") okHttpClient: OkHttpClient,
        gson: Gson
    ): SportRadarService =
        Retrofit.Builder()
            .baseUrl(BASE_URL_SPORTRADAR)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SportRadarService::class.java)

    // ─────────────────────────────────────────────────────────────────────────
    // Football-data.org client
    // Docs: https://www.football-data.org/documentation/quickstart
    // Auth: X-Auth-Token header
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("footballdata")
    fun provideFootballDataOkHttp(): OkHttpClient =
        baseOkHttpClient(
            Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        // .addHeader("X-Auth-Token", BuildConfig.FOOTBALL_DATA_API_KEY)
                        .addHeader("x-apisports-key", BuildConfig.FOOTBALL_DATA_API_KEY)
                        .build()
                )
            }
        )

    @Provides
    @Singleton
    fun provideFootballDataService(
        @Named("footballdata") okHttpClient: OkHttpClient,
        gson: Gson
    ): FootballDataService =
        Retrofit.Builder()
            .baseUrl(BASE_URL_FOOTBALL_DATA)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FootballDataService::class.java)

    // ─────────────────────────────────────────────────────────────────────────
    // Google Places API (New) client
    // Auth: X-Goog-Api-Key and X-Goog-FieldMask headers
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("places")
    fun providePlacesOkHttp(): OkHttpClient =
        baseOkHttpClient(
            Interceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("X-Goog-Api-Key", BuildConfig.PLACES_API_KEY)
                    .addHeader("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.types,places.nationalPhoneNumber,places.regularOpeningHours,places.photos,places.websiteUri")
                    .build()
                chain.proceed(request)
            }
        )

    @Provides
    @Singleton
    fun providePlacesApiService(
        @Named("places") okHttpClient: OkHttpClient,
        gson: Gson
    ): PlacesApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL_PLACES)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PlacesApiService::class.java)

    // ─────────────────────────────────────────────────────────────────────────
    // GitHub Gist client (broadcast schedule JSON)
    // No auth required for public Gists; uses Scalars converter for raw JSON.
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("gist")
    fun provideGistOkHttp(): OkHttpClient = baseOkHttpClient()

    @Provides
    @Singleton
    fun provideGistConfigService(
        @Named("gist") okHttpClient: OkHttpClient
    ): GistConfigService =
        Retrofit.Builder()
            .baseUrl(BASE_URL_GIST)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GistConfigService::class.java)
}
