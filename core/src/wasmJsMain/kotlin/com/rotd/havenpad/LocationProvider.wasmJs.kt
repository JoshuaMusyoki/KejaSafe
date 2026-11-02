package com.rotd.havenpad

import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): GeoCoordinates? {
        val payload = withTimeoutOrNull(15_000) { readFreshBrowserPosition() } ?: return null
        val parts = payload.split(',')
        if (parts.size < 2) return null
        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        val accuracy = parts.getOrNull(2)?.toFloatOrNull()?.takeIf { it > 0f }
        return GeoCoordinates(latitude, longitude, accuracy)
    }

    actual suspend fun getPlaceName(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
    ): String? = null

    actual suspend fun searchPlaces(query: String): List<String> = emptyList()
    actual suspend fun findPlaces(query: String): List<PlaceSuggestion> = emptyList()
    actual suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates? = null
}

private suspend fun readFreshBrowserPosition(): String {
    val deferred = CompletableDeferred<String>()
    bindBrowserPosition { payload ->
        if (!deferred.isCompleted) deferred.complete(payload)
    }
    requestBrowserPosition()
    return deferred.await()
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(callback) => { globalThis.__havenPosition = callback; }"
)
private external fun bindBrowserPosition(callback: (String) -> Unit)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
      var done = globalThis.__havenPosition;
      if (!navigator.geolocation || !done) { if (done) done(''); return; }
      navigator.geolocation.getCurrentPosition(
        function (pos) {
          done(pos.coords.latitude + ',' + pos.coords.longitude + ',' + (pos.coords.accuracy || 0));
        },
        function () { done(''); },
        { enableHighAccuracy: true, maximumAge: 0, timeout: 12000 }
      );
    }"""
)
private external fun requestBrowserPosition()
