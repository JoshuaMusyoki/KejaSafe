package com.rotd.kejasafe

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rotd.kejasafe.storage.AndroidAppContext
import kotlinx.coroutines.tasks.await

actual class LocationProvider actual constructor() {
    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): GeoCoordinates? {
        return try {
            val ctx = AndroidAppContext.require()
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
            
            // Try to get last location first (fast)
            val lastLoc = fusedLocationClient.lastLocation.await()
            if (lastLoc != null) {
                return GeoCoordinates(lastLoc.latitude, lastLoc.longitude)
            }
            
            // Request fresh location (slower but more accurate if last is null)
            val token = CancellationTokenSource().token
            val freshLoc = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                token
            ).await()
            
            freshLoc?.let { GeoCoordinates(it.latitude, it.longitude) }
        } catch (e: Exception) {
            println("LocationProvider error: ${e.message}")
            null
        }
    }

    actual suspend fun getPlaceName(latitude: Double, longitude: Double): String? {
        return try {
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

    actual suspend fun searchPlaces(query: String): List<String> {
        return try {
            val ctx = AndroidAppContext.require()
            val geocoder = Geocoder(ctx)
            val addresses = geocoder.getFromLocationName(query, 5)
            addresses?.mapNotNull { it.featureName ?: it.adminArea ?: it.subLocality }?.distinct() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
