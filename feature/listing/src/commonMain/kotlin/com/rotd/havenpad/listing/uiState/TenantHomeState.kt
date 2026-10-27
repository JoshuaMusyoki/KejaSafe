package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.storage.JsonCache
import kotlinx.serialization.Serializable

@Serializable
data class RecentSearchItem(
    val destination: String,
    val dates: String,
    val guests: String
)

object RecentSearchStore {
    private const val KEY = "settings.recent_searches_v2"

    fun read(): List<RecentSearchItem> =
        JsonCache.getList(KEY, RecentSearchItem.serializer()) ?: emptyList()

    fun save(items: List<RecentSearchItem>) {
        JsonCache.putList(KEY, items.take(10), RecentSearchItem.serializer())
    }
}

data class TenantHomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val syncWarning: String? = null,
    val listings: List<PropertyListing> = emptyList(),
    val searchQuery: String = "",
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val userLocationName: String? = null,
    val isLocating: Boolean = false,
    val showLocationPrompt: Boolean = false,
    val visibleListings: List<PropertyListing> = emptyList(),
    val showingDistantSuggestions: Boolean = false,
    val distancesReady: Boolean = false,
    val nearbyHubs: List<Pair<String, Pair<Double, Double>>> = emptyList(),
    val isLoadingHubs: Boolean = false,
    val isSearchOverlayOpen: Boolean = false,
    val destinationQuery: String = "",
    val moveInDateText: String? = null,
    val roommatesCount: Int = 1,
    val recentSearches: List<RecentSearchItem> = RecentSearchStore.read()
)
