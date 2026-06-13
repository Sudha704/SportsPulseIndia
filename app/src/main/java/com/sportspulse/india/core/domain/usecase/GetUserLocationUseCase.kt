package com.sportspulse.india.core.domain.usecase

import com.sportspulse.india.core.domain.entity.UserLocation
import com.sportspulse.india.core.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for obtaining the user's current device location.
 *
 * Strategy:
 *  1. Try [LocationRepository.fetchDeviceLocation] (FusedLocationProvider).
 *  2. On failure, return the last saved [UserLocation] from DataStore.
 *  3. If DataStore is also empty, return [UserLocation.DEFAULT] (Bengaluru).
 *
 * @param locationRepository  Handles GPS and DataStore operations.
 */
class GetUserLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    /**
     * Exposes a reactive stream of the user's location from the repository.
     */
    fun observeLocation(): Flow<UserLocation> {
        return locationRepository.getUserLocation()
    }

    /**
     * Explicit alias for [invoke] to improve readability in some contexts.
     */
    suspend fun fetchFreshLocation(saveToStore: Boolean = true): Result<UserLocation> {
        return invoke(saveToStore)
    }

    /**
     * @param saveToStore  When true, persists the fetched location to DataStore.
     */
    suspend operator fun invoke(saveToStore: Boolean = true): Result<UserLocation> {
        val deviceResult = locationRepository.fetchDeviceLocation()
        return if (deviceResult.isSuccess) {
            val location = deviceResult.getOrThrow()
            if (saveToStore) locationRepository.saveUserLocation(location)
            deviceResult
        } else {
            // Fallback: return last saved location or default
            runCatching {
                var fallback = UserLocation.DEFAULT
                locationRepository.getUserLocation().collect { saved ->
                    fallback = saved
                    // collect only once
                    throw StopCollectionException(fallback)
                }
                fallback
            }.recoverCatching { e ->
                if (e is StopCollectionException) e.location
                else UserLocation.DEFAULT
            }
        }
    }

    private class StopCollectionException(val location: UserLocation) : Throwable()
}
