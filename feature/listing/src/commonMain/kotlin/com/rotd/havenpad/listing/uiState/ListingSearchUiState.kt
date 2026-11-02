package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.PropertyType

enum class ListingSortOption {
    RELEVANCE,
    PRICE_LOW,
    PRICE_HIGH,
    DISTANCE
}

data class DistanceFilter(
    val label: String,
    val maxKm: Double? = null
) {
    companion object {
        val Any = DistanceFilter("Any distance")
        val Within1Km = DistanceFilter("Within 1 km", maxKm = 1.0)
        val Within5Km = DistanceFilter("Within 5 km", maxKm = 5.0)
        val Within10Km = DistanceFilter("Within 10 km", maxKm = 10.0)
        val Within30Km = DistanceFilter("Within 30 km", maxKm = 30.0)

        val presets: List<DistanceFilter> = listOf(
            Any, Within1Km, Within5Km, Within10Km, Within30Km
        )
    }
}

data class PriceRangeFilter(
    val label: String,
    val min: Double? = null,
    val max: Double? = null
) {
    companion object {
        val Any = PriceRangeFilter("Any price")
        val Under5k = PriceRangeFilter("Under 5k", max = 5_000.0)
        val From5to10k = PriceRangeFilter("5k – 10k", min = 5_000.0, max = 10_000.0)
        val From10to15k = PriceRangeFilter("10k – 15k", min = 10_000.0, max = 15_000.0)
        val From15kPlus = PriceRangeFilter("15k+", min = 15_000.0)

        val presets: List<PriceRangeFilter> = listOf(
            Any, Under5k, From5to10k, From10to15k, From15kPlus
        )
    }
}

data class ListingSearchUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val syncWarning: String? = null,
    val listings: List<PropertyListing> = emptyList(),
    val query: String = "",
    val priceRange: PriceRangeFilter = PriceRangeFilter.Any,
    val minPriceInput: String = "",
    val maxPriceInput: String = "",
    val distance: DistanceFilter = DistanceFilter.Any,
    val proximity: CampusProximity? = null,
    val propertyType: PropertyType? = null,
    val university: String? = null,
    val verifiedOnly: Boolean = false,
    val sort: ListingSortOption = ListingSortOption.RELEVANCE,
    val showFilters: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val locationMessage: String? = null,
    val results: List<PropertyListing> = emptyList(),
    val universities: List<String> = emptyList(),
    val distancesReady: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (hasPriceFilter) count++
            if (distance != DistanceFilter.Any) count++
            if (proximity != null) count++
            if (propertyType != null) count++
            if (!university.isNullOrBlank()) count++
            if (verifiedOnly) count++
            if (sort != ListingSortOption.RELEVANCE) count++
            return count
        }

    val hasPriceFilter: Boolean
        get() = priceRange != PriceRangeFilter.Any || hasCustomPrice

    private val hasCustomPrice: Boolean
        get() = minPriceInput.isNotBlank() || maxPriceInput.isNotBlank()

    fun effectiveMinPrice(): Double? =
        if (hasCustomPrice) minPriceInput.toDoubleOrNull() else priceRange.min

    fun effectiveMaxPrice(): Double? =
        if (hasCustomPrice) maxPriceInput.toDoubleOrNull() else priceRange.max
}
