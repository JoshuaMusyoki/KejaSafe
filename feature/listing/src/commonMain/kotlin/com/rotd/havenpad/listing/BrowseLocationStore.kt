package com.rotd.havenpad.listing

import com.rotd.havenpad.storage.JsonCache
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
        JsonCache.put(
            KEY,
            SavedBrowseLocation(
                latitude = latitude,
                longitude = longitude,
                name = name ?: existing?.name,
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
