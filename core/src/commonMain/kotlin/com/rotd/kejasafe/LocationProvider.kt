package com.rotd.kejasafe

import kotlinx.serialization.Serializable

@Serializable
data class GeoCoordinates(val latitude: Double, val longitude: Double)

expect class LocationProvider() {
    suspend fun getCurrentLocation(): GeoCoordinates?
    suspend fun getPlaceName(latitude: Double, longitude: Double): String?
    suspend fun searchPlaces(query: String): List<String>
}
