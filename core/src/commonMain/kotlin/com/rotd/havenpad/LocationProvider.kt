package com.rotd.havenpad

import kotlinx.serialization.Serializable

@Serializable
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    /** How tight the fix is, in meters. Empty when the platform does not report it. */
    val accuracyMeters: Float? = null,
)

@Serializable
data class PlaceSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

expect class LocationProvider() {
    suspend fun getCurrentLocation(): GeoCoordinates?
    suspend fun getPlaceName(latitude: Double, longitude: Double, accuracyMeters: Float? = null): String?
    suspend fun searchPlaces(query: String): List<String>
    suspend fun findPlaces(query: String): List<PlaceSuggestion>
    suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates?
}
