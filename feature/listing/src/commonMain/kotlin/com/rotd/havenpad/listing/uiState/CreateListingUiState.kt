package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.CampusSuggestion
import com.rotd.havenpad.CreateListingDraft
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.LandmarkSuggestion
import com.rotd.havenpad.ListingPhotoResponse
import com.rotd.havenpad.PendingListingPhoto

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
    val isSearchingPlaces: Boolean = false,
    /** Nearby campuses from Google/Mapbox + curated list, keyed to the listing pin. */
    val campusSuggestions: List<CampusSuggestion> = emptyList(),
    val isLoadingCampuses: Boolean = false,
    /** Fresh status from the server. New posts stay closed until this is VERIFIED. */
    val kycStatus: KycStatus = KycStatus.NOT_SUBMITTED,
    val checkingVerification: Boolean = false
)