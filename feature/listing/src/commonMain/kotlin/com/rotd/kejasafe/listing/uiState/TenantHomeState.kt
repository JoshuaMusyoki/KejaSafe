package com.rotd.kejasafe.listing.uiState

import com.rotd.kejasafe.PropertyListing

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
    val showLocationPrompt: Boolean = false,
    val visibleListings: List<PropertyListing> = emptyList(),
    val showingDistantSuggestions: Boolean = false,
    val distancesReady: Boolean = false,
    val nearbyHubs: List<Pair<String, Pair<Double, Double>>> = emptyList(),
    val isLoadingHubs: Boolean = false
)