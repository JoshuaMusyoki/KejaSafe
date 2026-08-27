package com.rotd.kejasafe.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * JSON blob cache on top of [LocalSettings] — shared across KMP targets.
 * Used for offline-first reads of listings, bills, and profile.
 */
object JsonCache {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun <T> get(key: String, serializer: KSerializer<T>): T? {
        val raw = LocalSettings.getString(key) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    fun <T> put(key: String, value: T, serializer: KSerializer<T>) {
        val raw = json.encodeToString(serializer, value)
        LocalSettings.putString(key, raw)
    }

    fun <T> getList(key: String, elementSerializer: KSerializer<T>): List<T>? =
        get(key, ListSerializer(elementSerializer))

    fun <T> putList(key: String, value: List<T>, elementSerializer: KSerializer<T>) {
        put(key, value, ListSerializer(elementSerializer))
    }

    fun remove(key: String) {
        LocalSettings.putString(key, null)
    }

    fun removeKeysWithPrefix(prefix: String, knownKeys: Iterable<String>) {
        knownKeys.filter { it.startsWith(prefix) }.forEach { remove(it) }
    }
}

object CacheKeys {
    const val BROWSE_LISTINGS = "cache.listings.browse"
    const val PENDING_LISTING_MUTATIONS = "cache.listings.pending_mutations"
    const val PREFIX = "cache."

    fun mineListings(userId: String) = "cache.listings.mine.$userId"
    fun performance(userId: String) = "cache.performance.$userId"
    fun incomingRequests(userId: String) = "cache.requests.$userId"
    fun myBookings(userId: String) = "cache.bookings.$userId"
    fun bills(userId: String) = "cache.rent.bills.$userId"
    fun profile(userId: String) = "cache.profile.$userId"
    fun listing(id: String) = "cache.listing.$id"

    /** Keys we know how to clear on logout for a given user. */
    fun userScoped(userId: String): List<String> = listOf(
        mineListings(userId),
        performance(userId),
        incomingRequests(userId),
        myBookings(userId),
        bills(userId),
        profile(userId),
        PENDING_LISTING_MUTATIONS
    )
}
