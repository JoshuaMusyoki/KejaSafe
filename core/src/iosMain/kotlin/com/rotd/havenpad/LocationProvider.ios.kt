package com.rotd.havenpad

import com.rotd.havenpad.trust.browsePlaceName
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): GeoCoordinates? {
        val location = requestOneLocation() ?: return null
        val accuracy = location.horizontalAccuracy
        val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
        return GeoCoordinates(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy.takeIf { it > 0.0 }?.toFloat(),
        )
    }

    actual suspend fun getPlaceName(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
    ): String? {
        val mark = reverse(latitude, longitude) ?: return null
        return browsePlaceName(
            neighborhood = mark.subLocality,
            city = mark.locality,
            accuracyMeters = accuracyMeters,
        )
    }

    actual suspend fun searchPlaces(query: String): List<String> =
        findPlaces(query).map { it.name }

    actual suspend fun findPlaces(query: String): List<PlaceSuggestion> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val marks = geocode(q).ifEmpty {
            if (q.contains("Kenya", ignoreCase = true)) emptyList()
            else geocode("$q, Nairobi, Kenya").ifEmpty { geocode("$q, Kenya") }
        }
        return marks.mapNotNull { mark ->
            val location = mark.location ?: return@mapNotNull null
            val primary = listOf(mark.name, mark.subLocality, mark.thoroughfare, mark.locality)
                .firstOrNull { !it.isNullOrBlank() }
                ?: q
            val full = listOf(primary, mark.subLocality, mark.locality, mark.country)
                .filterNot { it.isNullOrBlank() }
                .distinct()
                .joinToString(", ")
            val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
            PlaceSuggestion(
                name = full.ifBlank { primary },
                latitude = latitude,
                longitude = longitude,
            )
        }.distinctBy { "${it.latitude},${it.longitude}" }
    }

    actual suspend fun getCoordinatesFromAddress(address: String): GeoCoordinates? {
        val q = address.trim()
        if (q.isBlank()) return null
        val marks = geocode(q).ifEmpty {
            if (q.contains("Kenya", ignoreCase = true)) emptyList()
            else geocode("$q, Nairobi, Kenya")
        }
        val location = marks.firstOrNull()?.location ?: return null
        val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
        return GeoCoordinates(latitude, longitude)
    }
}

internal fun locationAccessGranted(): Boolean = CLLocationManager().isAllowed()

internal fun requestLocationAccess(onResult: (Boolean) -> Unit) {
    val manager = CLLocationManager()
    if (manager.isAllowed()) {
        onResult(true)
        return
    }
    if (manager.authorizationStatus != CLAuthorizationStatus.kCLAuthorizationStatusNotDetermined) {
        onResult(false)
        return
    }
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (manager.authorizationStatus == CLAuthorizationStatus.kCLAuthorizationStatusNotDetermined) return
            heldLocationDelegate = null
            onResult(manager.isAllowed())
        }
    }
    heldLocationDelegate = delegate
    manager.delegate = delegate
    heldLocationManager = manager
    manager.requestWhenInUseAuthorization()
}

private var heldLocationDelegate: NSObject? = null
private var heldLocationManager: CLLocationManager? = null

private fun CLLocationManager.isAllowed(): Boolean {
    val status = authorizationStatus
    return status == CLAuthorizationStatus.kCLAuthorizationStatusAuthorizedWhenInUse ||
        status == CLAuthorizationStatus.kCLAuthorizationStatusAuthorizedAlways
}

private suspend fun requestOneLocation(): CLLocation? = withTimeoutOrNull(12_000) {
    suspendCancellableCoroutine { continuation ->
        val manager = CLLocationManager()
        fun finish(location: CLLocation?) {
            heldLocationDelegate = null
            heldLocationManager = null
            manager.delegate = null
            if (continuation.isActive) continuation.resume(location)
        }
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                finish(didUpdateLocations.lastOrNull() as? CLLocation)
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                finish(null)
            }

            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                when {
                    manager.isAllowed() -> manager.requestLocation()
                    manager.authorizationStatus == CLAuthorizationStatus.kCLAuthorizationStatusNotDetermined ->
                        manager.requestWhenInUseAuthorization()
                    else -> finish(null)
                }
            }
        }
        heldLocationDelegate = delegate
        heldLocationManager = manager
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        continuation.invokeOnCancellation {
            heldLocationDelegate = null
            heldLocationManager = null
            manager.delegate = null
        }
        when {
            manager.isAllowed() -> manager.requestLocation()
            manager.authorizationStatus == CLAuthorizationStatus.kCLAuthorizationStatusNotDetermined ->
                manager.requestWhenInUseAuthorization()
            else -> finish(null)
        }
    }
}

private suspend fun reverse(latitude: Double, longitude: Double): CLPlacemark? =
    suspendCancellableCoroutine { continuation ->
        val location = CLLocation(latitude = latitude, longitude = longitude)
        CLGeocoder().reverseGeocodeLocation(location) { marks, _ ->
            if (continuation.isActive) {
                continuation.resume((marks as? List<*>)?.firstOrNull() as? CLPlacemark)
            }
        }
    }

private suspend fun geocode(query: String): List<CLPlacemark> =
    suspendCancellableCoroutine { continuation ->
        CLGeocoder().geocodeAddressString(query) { marks, _ ->
            if (continuation.isActive) {
                val places = (marks as? List<*>)?.mapNotNull { it as? CLPlacemark }.orEmpty()
                continuation.resume(places)
            }
        }
    }
