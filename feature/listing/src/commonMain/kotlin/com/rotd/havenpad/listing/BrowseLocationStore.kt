package com.rotd.havenpad.listing

import com.rotd.havenpad.storage.JsonCache
import com.rotd.havenpad.utils.calculateDistance
import kotlinx.serialization.Serializable

@Serializable
data class SavedBrowseLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val name: String? = null,
    val skipped: Boolean = false,
    /** True when the pin came from the device, not from a named area lookup. */
    val fromDevice: Boolean = false
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null
}

/**
 * Device-level browse location. Survives logout so the campus picker
 * does not return on every sign-in.
 */
object BrowseLocationStore {
    private const val KEY = "settings.browse_location"

    fun read(): SavedBrowseLocation? =
        JsonCache.get(KEY, SavedBrowseLocation.serializer())

    fun hasChoice(): Boolean {
        val saved = read() ?: return false
        return saved.skipped || saved.hasCoordinates
    }

    fun saveCoordinates(
        latitude: Double,
        longitude: Double,
        name: String?,
        fromDevice: Boolean = false
    ) {
        val existing = read()
        val movedKm = if (existing?.latitude != null && existing.longitude != null) {
            calculateDistance(existing.latitude, existing.longitude, latitude, longitude)
        } else {
            Double.MAX_VALUE
        }
        val resolvedName = name?.trim()?.takeIf { it.isNotBlank() }
            ?: if (fromDevice && movedKm > 0.4) null else existing?.name
        JsonCache.put(
            KEY,
            SavedBrowseLocation(
                latitude = latitude,
                longitude = longitude,
                name = resolvedName,
                skipped = false,
                fromDevice = fromDevice
            ),
            SavedBrowseLocation.serializer()
        )
    }

    fun saveSkipped() {
        val current = read()
        JsonCache.put(
            KEY,
            SavedBrowseLocation(
                latitude = current?.latitude,
                longitude = current?.longitude,
                name = current?.name,
                skipped = true,
                fromDevice = current?.fromDevice == true
            ),
            SavedBrowseLocation.serializer()
        )
    }
}
