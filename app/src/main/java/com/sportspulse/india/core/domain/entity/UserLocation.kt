package com.sportspulse.india.core.domain.entity

/**
 * Represents the user's last known or manually selected location.
 *
 * Stored in DataStore (not SharedPreferences) to support Flow-based reactivity.
 *
 * @param latitude       WGS-84 latitude.
 * @param longitude      WGS-84 longitude.
 * @param areaName       Human-readable area/locality name (reverse-geocoded).
 * @param cityName       City name.
 * @param capturedAtMs   Epoch-ms when this location was captured.
 * @param isManual       True when user typed a city name rather than using GPS.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val areaName: String = "",
    val cityName: String = "",
    val capturedAtMs: Long = System.currentTimeMillis(),
    val isManual: Boolean = false
) {
    companion object {
        /** Default to Bengaluru when no location is available. */
        val DEFAULT = UserLocation(
            latitude = 12.9716,
            longitude = 77.5946,
            areaName = "Bengaluru",
            cityName = "Bengaluru",
            isManual = true
        )
    }
}
