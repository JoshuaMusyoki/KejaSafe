package com.rotd.kejasafe

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): GeoCoordinates? = null
    actual suspend fun getPlaceName(latitude: Double, longitude: Double): String? = null
    actual suspend fun searchPlaces(query: String): List<String> = emptyList()
}
