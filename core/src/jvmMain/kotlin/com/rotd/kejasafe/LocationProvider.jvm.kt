package com.rotd.kejasafe

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): GeoCoordinates? = null
    actual suspend fun getPlaceName(latitude: Double, longitude: Double): String? = null
    actual suspend fun searchPlaces(query: String): List<String> = emptyList()
    actual suspend fun findPlaces(query: String): List<PlaceSuggestion> = emptyList()
    actual suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates? = null
}
