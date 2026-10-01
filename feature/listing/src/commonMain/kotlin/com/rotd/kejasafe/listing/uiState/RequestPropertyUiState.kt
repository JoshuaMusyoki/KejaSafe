package com.rotd.kejasafe.listing.uiState

import com.rotd.kejasafe.domain.listing.DistancePreference
import com.rotd.kejasafe.domain.listing.HouseSize
import com.rotd.kejasafe.domain.listing.HouseType
import com.rotd.kejasafe.domain.listing.ListingType

data class RequestPropertyUiState(
    val size: HouseSize = HouseSize.ONE_BEDROOM,
    val houseType: HouseType? = HouseType.APARTMENT,
    val listingType: ListingType = ListingType.RENT,
    val area: String = "Rungiri, Kabete",
    val estateSuggestions: List<String> = emptyList(),
    val distance: DistancePreference = DistancePreference.SHORT_MATATU_RIDE,
    val budgetFrom: String = "10000",
    val budgetTo: String = "15000",
    val additionalInfo: String = "",
    val agentsCanCall: Boolean = true,
    val phoneNumber: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val completed: Boolean = false
)