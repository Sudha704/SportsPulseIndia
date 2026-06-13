package com.sportspulse.india.core.domain.usecase

import kotlin.math.*

/**
 * Utility use case for computing straight-line distance between two geo-coordinates
 * using the Haversine formula.
 *
 * This is used as the *first-pass* distance before Distance Matrix API enrichment.
 *
 * Formula reference: https://en.wikipedia.org/wiki/Haversine_formula
 */
class HaversineDistanceUseCase {

    /**
     * Calculates the great-circle distance between two points on Earth.
     *
     * @param lat1  Latitude of origin in decimal degrees.
     * @param lon1  Longitude of origin in decimal degrees.
     * @param lat2  Latitude of destination in decimal degrees.
     * @param lon2  Longitude of destination in decimal degrees.
     * @return      Distance in kilometres (km), rounded to 2 decimal places.
     */
    operator fun invoke(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distanceKm = earthRadiusKm * c
        return (distanceKm * 100.0).roundToInt() / 100.0
    }

    /**
     * Sorts a list of items by their Haversine distance from [originLat]/[originLon],
     * using [latSelector] and [lonSelector] to extract coordinates from each item.
     */
    fun <T> sortByDistance(
        items: List<T>,
        originLat: Double,
        originLon: Double,
        latSelector: (T) -> Double,
        lonSelector: (T) -> Double
    ): List<T> = items.sortedBy { item ->
        invoke(originLat, originLon, latSelector(item), lonSelector(item))
    }
}
