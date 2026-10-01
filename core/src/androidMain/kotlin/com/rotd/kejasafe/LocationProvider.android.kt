package com.rotd.kejasafe

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rotd.kejasafe.storage.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

actual class LocationProvider actual constructor() {
    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): GeoCoordinates? = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
            
            // Try to get last location first (fast)
            val lastLoc = fusedLocationClient.lastLocation.await()
            if (lastLoc != null) {
                GeoCoordinates(lastLoc.latitude, lastLoc.longitude)
            } else {
                val token = CancellationTokenSource().token
                val freshLoc = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    token
                ).await()
                freshLoc?.let { GeoCoordinates(it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            println("LocationProvider error: ${e.message}")
            null
        }
    }

    actual suspend fun getPlaceName(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull()
            address?.let {
                listOfNotNull(it.subLocality, it.locality).joinToString(", ")
                    .ifBlank { it.thoroughfare }
                    ?.ifBlank { "Unknown location" }
            }
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
