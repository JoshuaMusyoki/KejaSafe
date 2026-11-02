package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.ListingAnalytics

data class ListingAnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: ListingAnalytics? = null,
    val error: String? = null
)