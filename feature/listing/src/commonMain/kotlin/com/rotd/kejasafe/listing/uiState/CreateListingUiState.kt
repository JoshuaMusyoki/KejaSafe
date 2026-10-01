package com.rotd.kejasafe.listing.uiState

import com.rotd.kejasafe.CreateListingDraft
import com.rotd.kejasafe.LandmarkSuggestion
import com.rotd.kejasafe.ListingPhotoResponse
import com.rotd.kejasafe.PendingListingPhoto

data class CreateListingUiState(
    val listingId: String? = null,
    val draft: CreateListingDraft = CreateListingDraft(),
    val existingPhotos: List<ListingPhotoResponse> = emptyList(),
    val pendingPhotos: List<PendingListingPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,
    /** Soft note when save was queued for later sync. */
    val syncNote: String? = null,
    val placeSuggestions: List<LandmarkSuggestion> = emptyList(),
    val isSearchingPlaces: Boolean = false
)