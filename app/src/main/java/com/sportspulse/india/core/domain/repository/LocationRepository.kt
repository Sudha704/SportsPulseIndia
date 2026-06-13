package com.sportspulse.india.core.domain.repository

import com.sportspulse.india.core.domain.entity.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and storing the user's location.
 * Backed by DataStore (not SharedPreferences) for Flow-reactive reads.
 */
interface LocationRepository {

    /**
     * Emits the current user location (GPS or manual).
     * Defaults to [UserLocation.DEFAULT] when none has been saved.
     */
    fun getUserLocation(): Flow<UserLocation>

    /**
     * Persists [location] to DataStore.
     */
    suspend fun saveUserLocation(location: UserLocation)

    /**
     * Attempts to obtain the current device location via [FusedLocationProviderClient].
     *  1. Returns last known location if available.
     *  2. Falls back to a fresh location request.
     * Throws [SecurityException] if location permission is not granted.
     */
    suspend fun fetchDeviceLocation(): Result<UserLocation>

    /**
     * Reverse-geocodes [latitude]/[longitude] to a human-readable area + city name.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<Pair<String, String>>
}
