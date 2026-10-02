package com.rotd.havenpad.listing

import com.rotd.havenpad.GeoCoordinates
import com.rotd.havenpad.LocationProvider
import com.rotd.havenpad.PropertyListing
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 * Fills in missing house coordinates after the list is already on screen.
 * Results are remembered so opening search does not geocode the same addresses again.
 */
internal object ListingCoordinateEnricher {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, GeoCoordinates>()
    private val misses = mutableSetOf<String>()

    suspend fun enrich(listings: List<PropertyListing>): List<PropertyListing> {
        if (listings.none { it.needsBackgroundGeocode() }) return listings
        return buildList(listings.size) {
            for (listing in listings) {
                coroutineContext.ensureActive()
                add(resolve(listing))
            }
        }
    }

    private suspend fun resolve(listing: PropertyListing): PropertyListing {
        if (!listing.needsBackgroundGeocode()) return listing
        val key = listing.address.trim().lowercase()
        mutex.withLock { cache[key] }?.let { known ->
            return listing.copy(latitude = known.latitude, longitude = known.longitude)
        }
        if (mutex.withLock { key in misses }) return listing
        val coords = runCatching { LocationProvider().getCoordinatesFromAddress(listing.address) }.getOrNull()
        mutex.withLock {
            if (coords != null) cache[key] = coords else misses.add(key)
        }
        return if (coords != null) {
            listing.copy(latitude = coords.latitude, longitude = coords.longitude)
        } else {
            listing
        }
    }
}

/** True when the house still needs its address turned into coordinates. */
private fun PropertyListing.needsBackgroundGeocode(): Boolean = address.isNotBlank()
