package com.rotd.havenpad

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rotd.havenpad.storage.AndroidAppContext
import com.rotd.havenpad.trust.browsePlaceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

actual class LocationProvider actual constructor() {
    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): GeoCoordinates? = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val token = CancellationTokenSource()
            val fresh = withTimeoutOrNull(12_000.milliseconds) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            }
            val last = runCatching { client.lastLocation.await() }.getOrNull()
            val chosen = fresh ?: last?.takeIf {
                System.currentTimeMillis() - it.time <= RECENT_FIX_MS
            } ?: return@withContext null
            GeoCoordinates(
                latitude = chosen.latitude,
                longitude = chosen.longitude,
                accuracyMeters = chosen.accuracy.takeIf { it > 0f },
            )
        } catch (e: Exception) {
            println("LocationProvider error: ${e.message}")
            null
        }
    }

    actual suspend fun getPlaceName(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            val addresses = geocoder.suspendGetFromLocation(latitude, longitude, 1)
            val address = addresses.firstOrNull() ?: return@withContext null
            browsePlaceName(
                neighborhood = address.subLocality,
                city = address.locality,
                accuracyMeters = accuracyMeters,
            )
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun searchPlaces(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            val addresses = geocoder.suspendGetFromLocationName(query, 5)
            addresses.mapNotNull { it.featureName ?: it.adminArea ?: it.subLocality }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun findPlaces(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        try {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)

            var addresses = geocoder.suspendGetFromLocationName(q, 8)

            // Regional fallbacks for Kenya/Nairobi if standard location name search returns empty
            if (addresses.isEmpty() && !q.contains("Kenya", ignoreCase = true)) {
                val nairobiQuery = "$q, Nairobi, Kenya"
                addresses = geocoder.suspendGetFromLocationName(nairobiQuery, 8)
            }
            if (addresses.isEmpty() && !q.contains("Kenya", ignoreCase = true)) {
                val kenyaQuery = "$q, Kenya"
                addresses = geocoder.suspendGetFromLocationName(kenyaQuery, 8)
            }

            val suggestions = addresses.mapNotNull { address ->
                if (!address.hasLatitude() || !address.hasLongitude()) return@mapNotNull null
                val primaryName = listOfNotNull(
                    address.subLocality,
                    address.featureName,
                    address.locality,
                    address.thoroughfare
                ).firstOrNull { it.isNotBlank() } ?: q

                val fullAddress = listOfNotNull(
                    primaryName,
                    address.subLocality.takeIf { it != primaryName },
                    address.locality,
                    address.countryName
                ).distinct().joinToString(", ")

                PlaceSuggestion(
                    name = fullAddress.ifBlank { primaryName },
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }.distinctBy { "${it.latitude},${it.longitude}" }

            if (suggestions.isEmpty()) {
                val singleCoords = getCoordinatesFromAddress(q)
                if (singleCoords != null) {
                    return@withContext listOf(
                        PlaceSuggestion(
                            name = "$q, Kenya",
                            latitude = singleCoords.latitude,
                            longitude = singleCoords.longitude
                        )
                    )
                }
            }

            suggestions
        } catch (e: Exception) {
            println("findPlaces geocoder error: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates? = withContext(Dispatchers.IO) {
        try {
            val q = address.trim()
            if (q.isBlank()) return@withContext null
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            
            var addresses = geocoder.suspendGetFromLocationName(q, 1)
            if (addresses.isEmpty() && !q.contains("Kenya", ignoreCase = true)) {
                addresses = geocoder.suspendGetFromLocationName("$q, Nairobi, Kenya", 1)
            }
            val first = addresses.firstOrNull()
            if (first != null && first.hasLatitude() && first.hasLongitude()) {
                GeoCoordinates(first.latitude, first.longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@SuppressLint("NewApi")
private suspend fun Geocoder.suspendGetFromLocationName(query: String, maxResults: Int): List<Address> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            try {
                getFromLocationName(query, maxResults, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        continuation.resume(emptyList())
                    }
                })
            } catch (e: Exception) {
                continuation.resume(emptyList())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        runCatching { getFromLocationName(query, maxResults).orEmpty() }.getOrDefault(emptyList())
    }
}

@SuppressLint("NewApi")
private suspend fun Geocoder.suspendGetFromLocation(latitude: Double, longitude: Double, maxResults: Int): List<Address> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            try {
                getFromLocation(latitude, longitude, maxResults, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses)
                    }
                    override fun onError(errorMessage: String?) {
                        continuation.resume(emptyList())
                    }
                })
            } catch (e: Exception) {
                continuation.resume(emptyList())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        runCatching { getFromLocation(latitude, longitude, maxResults).orEmpty() }.getOrDefault(emptyList())
    }
}

private const val RECENT_FIX_MS = 2 * 60 * 1000L
