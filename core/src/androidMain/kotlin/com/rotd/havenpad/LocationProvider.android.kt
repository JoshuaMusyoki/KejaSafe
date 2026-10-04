package com.rotd.havenpad

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rotd.havenpad.storage.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

actual class LocationProvider actual constructor() {
    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): GeoCoordinates? = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val token = CancellationTokenSource()
            val fresh = withTimeoutOrNull(12_000) {
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
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                ?: return@withContext null
            val neighborhood = address.subLocality?.trim()?.takeIf { it.isNotBlank() }
            val city = address.locality?.trim()?.takeIf { it.isNotBlank() }
            val precise = accuracyMeters != null && accuracyMeters > 0f && accuracyMeters <= PRECISE_METERS
            val label = if (precise) {
                listOfNotNull(neighborhood, city).distinct().joinToString(", ")
            } else {
                city.orEmpty()
            }
            label.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun searchPlaces(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            val addresses = geocoder.getFromLocationName(query, 5)
            addresses?.mapNotNull { it.featureName ?: it.adminArea ?: it.subLocality }?.distinct() ?: emptyList()
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
            val addresses = geocoder.getFromLocationName(q, 8).orEmpty()
            addresses.mapNotNull { address ->
                if (!address.hasLatitude() || !address.hasLongitude()) return@mapNotNull null
                val name = listOfNotNull(
                    address.subLocality,
                    address.featureName,
                    address.locality
                ).firstOrNull { it.isNotBlank() } ?: q
                PlaceSuggestion(
                    name = name,
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }.distinctBy { "${it.latitude},${it.longitude}" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates? = withContext(Dispatchers.IO) {
        try {
            val q = address.trim()
            if (q.isBlank()) return@withContext null
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            
            var addresses = geocoder.getFromLocationName(q, 1)
            if ((addresses == null || addresses.isEmpty()) && !q.contains("Kenya", ignoreCase = true)) {
                addresses = geocoder.getFromLocationName("$q, Nairobi, Kenya", 1)
            }
            val first = addresses?.firstOrNull()
            if (first != null && first.hasLatitude() && first.hasLongitude()) {
                GeoCoordinates(first.latitude, first.longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

private const val RECENT_FIX_MS = 2 * 60 * 1000L
private const val PRECISE_METERS = 200f
