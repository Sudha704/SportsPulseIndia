package com.sportspulse.india.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Top-level extension property for the location DataStore. */
private val Context.locationDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_location_prefs")

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    // ─────────────────────────────────────────────────────────────────────────
    // FusedLocationProviderClient
    // Uses PRIORITY_BALANCED_POWER_ACCURACY as specified in the requirements.
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Default [LocationRequest] used when last-known location is null.
     * PRIORITY_BALANCED_POWER_ACCURACY = GPS + Wi-Fi + cell towers for a good
     * balance between accuracy (~100 m) and battery usage.
     */
    @Provides
    @Singleton
    fun provideLocationRequest(): LocationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            /* intervalMillis = */ 10_000L
        )
            .setMinUpdateIntervalMillis(5_000L)
            .setMaxUpdateDelayMillis(15_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()

    // ─────────────────────────────────────────────────────────────────────────
    // DataStore<Preferences>
    // Stores: latitude, longitude, area name, city name, capturedAtMs, isManual
    // ─────────────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLocationDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.locationDataStore
}
