package com.rotd.kejasafe.listing.uiState

import com.rotd.kejasafe.ListingAnalytics

data class ListingAnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: ListingAnalytics? = null,
    val error: String? = null
)