package com.sportspulse.india.features.venues.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Concrete implementation of [LocationRepository].
 *
 * Location is persisted to [DataStore] using primitive preference keys.
 * Device location is obtained via [FusedLocationProviderClient].
 */
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val locationRequest: LocationRequest,
    private val dataStore: DataStore<Preferences>
) : LocationRepository {

    // ─────────────────────────────────────────────────────────────────────────
    // DataStore preference keys
    // ─────────────────────────────────────────────────────────────────────────
    private object Keys {
        val LATITUDE     = doublePreferencesKey("loc_latitude")
        val LONGITUDE    = doublePreferencesKey("loc_longitude")
        val AREA_NAME    = stringPreferencesKey("loc_area_name")
        val CITY_NAME    = stringPreferencesKey("loc_city_name")
        val CAPTURED_AT  = longPreferencesKey("loc_captured_at")
        val IS_MANUAL    = stringPreferencesKey("loc_is_manual") // "true" | "false"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    override fun getUserLocation(): Flow<UserLocation> =
        dataStore.data.map { prefs ->
            val lat = prefs[Keys.LATITUDE]
            val lng = prefs[Keys.LONGITUDE]
            if (lat == null || lng == null) {
                UserLocation.DEFAULT
            } else {
                UserLocation(
                    latitude     = lat,
                    longitude    = lng,
                    areaName     = prefs[Keys.AREA_NAME] ?: "",
                    cityName     = prefs[Keys.CITY_NAME] ?: "",
                    capturedAtMs = prefs[Keys.CAPTURED_AT] ?: System.currentTimeMillis(),
                    isManual     = prefs[Keys.IS_MANUAL] == "true"
                )
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Write
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun saveUserLocation(location: UserLocation) {
        dataStore.edit { prefs ->
            prefs[Keys.LATITUDE]    = location.latitude
            prefs[Keys.LONGITUDE]   = location.longitude
            prefs[Keys.AREA_NAME]   = location.areaName
            prefs[Keys.CITY_NAME]   = location.cityName
            prefs[Keys.CAPTURED_AT] = location.capturedAtMs
            prefs[Keys.IS_MANUAL]   = location.isManual.toString()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Device location via FusedLocationProviderClient
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun fetchDeviceLocation(): Result<UserLocation> = runCatching {
        // 1. Try last known location first (fast, no GPS warmup)
        val lastKnown = suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { cont.resume(null) }
        }

        if (lastKnown != null) {
            Timber.d("LocationRepository: last known location lat=${lastKnown.latitude}")
            val (area, city) = reverseGeocode(lastKnown.latitude, lastKnown.longitude)
                .getOrDefault(Pair("", ""))
            return@runCatching UserLocation(
                latitude     = lastKnown.latitude,
                longitude    = lastKnown.longitude,
                areaName     = area,
                cityName     = city,
                capturedAtMs = System.currentTimeMillis(),
                isManual     = false
            )
        }

        // 2. Request a fresh location if last known is null
        Timber.d("LocationRepository: requesting fresh location")
        val freshLocation = suspendCancellableCoroutine { cont ->
            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    cont.resume(result.lastLocation)
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
            cont.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }

        val loc = freshLocation ?: throw IllegalStateException("Could not obtain device location")
        val (area, city) = reverseGeocode(loc.latitude, loc.longitude).getOrDefault(Pair("", ""))
        UserLocation(
            latitude     = loc.latitude,
            longitude    = loc.longitude,
            areaName     = area,
            cityName     = city,
            capturedAtMs = System.currentTimeMillis(),
            isManual     = false
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reverse geocoding via Android Geocoder
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<Pair<String, String>> = runCatching {
        val geocoder = Geocoder(context, Locale("en", "IN"))
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val area = addr.subLocality ?: addr.locality ?: addr.adminArea ?: ""
            val city = addr.locality ?: addr.adminArea ?: ""
            Pair(area, city)
        } else {
            Pair("", "")
        }
    }
}
