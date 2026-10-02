package com.rotd.havenpad.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.BookListingRequest
import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.CreateListingDraft
import com.rotd.havenpad.CreateListingRequest
import com.rotd.havenpad.CreateLiveTourRequest
import com.rotd.havenpad.DraftExtraCharge
import com.rotd.havenpad.ElectricityBilling
import com.rotd.havenpad.ExtraCharge
import com.rotd.havenpad.GenerateBillRequest
import com.rotd.havenpad.IncomingRequest
import com.rotd.havenpad.ListingAnalytics
import com.rotd.havenpad.ListingBooking
import com.rotd.havenpad.ListingCalendar
import com.rotd.havenpad.ListingCharges
import com.rotd.havenpad.ListingPerformance
import com.rotd.havenpad.ListingPhotoResponse
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.PendingListingPhoto
import com.rotd.havenpad.PickedImage
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.RoommateMatch
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.EscrowState
import com.rotd.havenpad.EscrowSummary
import com.rotd.havenpad.InitiatePaymentRequest
import com.rotd.havenpad.data.EscrowRepository
import com.rotd.havenpad.data.ListingRepository
import com.rotd.havenpad.data.PropertyRequestRepository
import com.rotd.havenpad.domain.listing.DistancePreference
import com.rotd.havenpad.domain.listing.HouseSize
import com.rotd.havenpad.domain.listing.HouseType
import com.rotd.havenpad.domain.listing.ListingType
import com.rotd.havenpad.domain.listing.PropertyRequest
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.listing.uiState.CreateListingUiState
import com.rotd.havenpad.listing.uiState.LandlordHomeState
import com.rotd.havenpad.listing.uiState.RequestPropertyUiState
import com.rotd.havenpad.listing.uiState.TenantHomeState
import com.rotd.havenpad.network.ConnectivityMonitor
import com.rotd.havenpad.network.ListingClient
import com.rotd.havenpad.network.ListingDataBus
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

private fun CreateListingDraft.toCharges(): ListingCharges {
    return ListingCharges(
        garbageCollection = garbageCollection.toDoubleOrNull() ?: 0.0,
        water = water.toDoubleOrNull() ?: 0.0,
        electricityBilling = if (electricityIsToken) {
            ElectricityBilling.TOKEN
        } else {
            ElectricityBilling.MONTHLY
        },
        electricityMonthly = if (electricityIsToken) {
            0.0
        } else {
            electricityMonthly.toDoubleOrNull() ?: 0.0
        },
        deposit = deposit.toDoubleOrNull() ?: 0.0,
        extraCharges = extraCharges.mapNotNull { c ->
            val name = c.name.trim()
            val amount = c.amount.toDoubleOrNull() ?: return@mapNotNull null
            if (name.isBlank() || amount <= 0) null else ExtraCharge(name, amount)
        }
    )
}

internal fun CreateListingDraft.estimateSummary(): String {
    val rent = pricePerMonth.toDoubleOrNull() ?: 0.0
    val charges = toCharges()
    val monthly = charges.monthlyGrandTotal(rent)
    val depositLabel = if (charges.deposit > 0) {
        " · Deposit KES ${charges.deposit.toInt()} (first bill)"
    } else ""
    val elec = if (charges.electricityBilling == ElectricityBilling.TOKEN) {
        " · Power: prepaid token"
    } else ""
    return "Est. monthly: KES ${monthly.toInt()}$depositLabel$elec"
}

private fun ListingCharges.toDraftFields(): CreateListingDraft.() -> CreateListingDraft = {
    val c = this@toDraftFields
    copy(
        garbageCollection = c.garbageCollection.takeIf { it > 0 }?.toInt()?.toString().orEmpty(),
        water = c.water.takeIf { it > 0 }?.toInt()?.toString().orEmpty(),
        electricityIsToken = c.electricityBilling == ElectricityBilling.TOKEN,
        electricityMonthly = c.electricityMonthly.takeIf { it > 0 }?.toInt()?.toString().orEmpty(),
        deposit = c.deposit.takeIf { it > 0 }?.toInt()?.toString().orEmpty(),
        extraCharges = c.extraCharges.mapIndexed { index, x ->
            DraftExtraCharge(
                id = "extra_$index",
                name = x.name,
                amount = x.amount.toInt().toString()
            )
        }
    )
}
class CreateListingViewModel(
    private val editingListingId: String? = null,
    private val listingRepository: ListingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        CreateListingUiState(
            listingId = editingListingId,
            isLoading = !editingListingId.isNullOrBlank()
        )
    )
    val state: StateFlow<CreateListingUiState> = _state.asStateFlow()

    private val _savedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvents = _savedEvents.asSharedFlow()

    init {
        if (!editingListingId.isNullOrBlank()) {
            loadListing(editingListingId)
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                listingRepository.syncPendingMutations()
                if (_state.value.syncNote != null) {
                    _state.update { it.copy(syncNote = null, error = null) }
                }
                if (!editingListingId.isNullOrBlank()) {
                    loadListing(editingListingId)
                }
            }
        }
    }

    /** Call when the screen becomes visible so a prior save can't immediately dismiss again. */
    fun onScreenVisible() {
        // Clear stale completion from a previous visit (activity-scoped ViewModel).
        if (_state.value.completed) {
            _state.update { it.copy(completed = false, isSaving = false) }
        }
        // Always re-fetch when editing so home→edit shows latest title/photos after saves.
        if (!editingListingId.isNullOrBlank()) {
            loadListing(editingListingId)
        } else if (!editingListingId.isNullOrBlank()) {
            loadListing(editingListingId)
        }
    }

    private var placeSearchJob: kotlinx.coroutines.Job? = null

    fun onLandmarkQueryChange(value: String) {
        updateDraft {
            it.copy(
                landmarkQuery = value,
                landmarkName = "",
                landmarkPlaceId = ""
            )
        }
        placeSearchJob?.cancel()
        val query = value.trim()
        if (query.length < 2) {
            _state.update { it.copy(placeSuggestions = emptyList(), isSearchingPlaces = false) }
            return
        }
        placeSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _state.update { it.copy(isSearchingPlaces = true) }
            val response = com.rotd.havenpad.network.PlacesClient.autocomplete(query).getOrNull()
            val suggestions = when {
                response == null || !response.configured ->
                    com.rotd.havenpad.network.PlacesClient.deviceSuggestions(query)
                else -> response.suggestions
            }
            if (_state.value.draft.landmarkQuery.trim() != query) return@launch
            _state.update { it.copy(placeSuggestions = suggestions, isSearchingPlaces = false) }
        }
    }

    fun selectLandmark(suggestion: com.rotd.havenpad.LandmarkSuggestion) {
        placeSearchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isSearchingPlaces = true, placeSuggestions = emptyList()) }
            val resolved = if (suggestion.latitude != null && suggestion.longitude != null) {
                suggestion
            } else {
                val details = com.rotd.havenpad.network.PlacesClient.details(suggestion.placeId).getOrNull()
                if (details == null) suggestion
                else suggestion.copy(
                    name = details.name.ifBlank { suggestion.name },
                    description = details.address.ifBlank { suggestion.description },
                    latitude = details.latitude,
                    longitude = details.longitude
                )
            }
            _state.update { s ->
                s.copy(
                    isSearchingPlaces = false,
                    placeSuggestions = emptyList(),
                    draft = s.draft.copy(
                        landmarkQuery = resolved.name,
                        landmarkName = resolved.name,
                        landmarkPlaceId = resolved.placeId,
                        latitude = resolved.latitude?.toString().orEmpty(),
                        longitude = resolved.longitude?.toString().orEmpty()
                    ),
                    error = if (resolved.latitude == null) {
                        "Could not load that place. Try another suggestion."
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun loadListing(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, completed = false) }

            fun applyListing(listing: PropertyListing) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        listingId = listing.id,
                        draft = CreateListingDraft(
                            title = listing.title,
                            address = listing.address,
                            proximity = listing.proximity ?: CampusProximity.INSIDE_CAMPUS,
                            university = listing.university.orEmpty(),
                            pricePerMonth = listing.pricePerMonth?.toString().orEmpty(),
                            propertyType = listing.propertyType ?: PropertyType.BEDSITTER,
                            unitsCount = (listing.unitsCount ?: 1).toString(),
                            latitude = listing.latitude.toString(),
                            longitude = listing.longitude.toString()
                        ).run { listing.charges.toDraftFields().invoke(this) },
                        existingPhotos = listing.photos.ifEmpty {
                            listing.imageUrls.mapIndexed { index, url ->
                                ListingPhotoResponse(
                                    id = "legacy_$index",
                                    listingId = listing.id,
                                    url = url,
                                    sortOrder = index
                                )
                            }
                        },
                        pendingPhotos = emptyList(),
                        completed = false,
                        error = null
                    )
                }
            }

            listingRepository.getListing(
                id = id,
                onCacheHit = { applyListing(it) }
            )
                .onSuccess { offline ->
                    applyListing(offline.value)
                    if (offline.syncWarning != null) {
                        _state.update { it.copy(error = offline.syncWarning) }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage()
                        )
                    }
                }
        }
    }

    fun updateDraft(transform: (CreateListingDraft) -> CreateListingDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun addPickedPhotos(images: List<PickedImage>) {
        if (images.isEmpty()) return
        val pending = images.map { image ->
            PendingListingPhoto(
                id = "photo_${Random.nextLong().toULong().toString(16)}",
                fileName = image.fileName,
                mimeType = image.mimeType,
                bytes = image.bytes
            )
        }
        _state.update { it.copy(pendingPhotos = it.pendingPhotos + pending) }
    }

    fun removePendingPhoto(id: String) {
        _state.update {
            it.copy(pendingPhotos = it.pendingPhotos.filterNot { photo -> photo.id == id })
        }
    }

    fun removeExistingPhoto(photoId: String) {
        val listingId = _state.value.listingId ?: return
        if (photoId.startsWith("legacy_")) {
            _state.update {
                it.copy(error = "Reload the listing, then try removing photos again")
            }
            return
        }
        viewModelScope.launch {
            ListingClient.deleteListingPhoto(listingId, photoId)
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            existingPhotos = s.existingPhotos.filterNot { it.id == photoId },
                            error = null
                        )
                    }
                    ListingDataBus.notifyListingsChanged()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(error = e.userMessage())
                    }
                }
        }
    }

    fun addExtraCharge() {
        val id = "extra_${Random.nextLong().toULong().toString(16)}"
        _state.update {
            it.copy(
                draft = it.draft.copy(
                    extraCharges = it.draft.extraCharges + DraftExtraCharge(id = id)
                )
            )
        }
    }

    fun updateExtraChargeName(id: String, name: String) {
        _state.update { s ->
            s.copy(
                draft = s.draft.copy(
                    extraCharges = s.draft.extraCharges.map {
                        if (it.id == id) it.copy(name = name) else it
                    }
                )
            )
        }
    }

    fun updateExtraChargeAmount(id: String, amount: String) {
        _state.update { s ->
            s.copy(
                draft = s.draft.copy(
                    extraCharges = s.draft.extraCharges.map {
                        if (it.id == id) it.copy(amount = amount) else it
                    }
                )
            )
        }
    }

    fun removeExtraCharge(id: String) {
        _state.update { s ->
            s.copy(
                draft = s.draft.copy(
                    extraCharges = s.draft.extraCharges.filterNot { it.id == id }
                )
            )
        }
    }

    fun submit() {
        val draft = _state.value.draft
        if (draft.title.isBlank()) {
            _state.update { it.copy(error = "Add a title for the house") }
            return
        }
        val landmark = draft.landmarkName.trim()
        val plot = draft.address.trim()
        if (plot.isBlank() && landmark.isBlank()) {
            _state.update { it.copy(error = "Add the building address or pick a nearby landmark") }
            return
        }
        if (landmark.isNotBlank() && draft.landmarkPlaceId.isBlank()) {
            _state.update { it.copy(error = "Select a landmark from the suggestions") }
            return
        }
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, syncNote = null) }

            val meters = draft.distanceMeters.toIntOrNull()
            val landmarkLine = when {
                landmark.isBlank() -> null
                meters != null && meters > 0 -> "$meters m from $landmark"
                else -> landmark
            }
            val address = listOfNotNull(
                plot.ifBlank { null },
                landmarkLine?.takeIf { line ->
                    plot.isBlank() || !plot.contains(landmark, ignoreCase = true)
                }
            ).joinToString(", ")

            val cleanLat = draft.latitude.replace(',', '.').trim()
            val cleanLng = draft.longitude.replace(',', '.').trim()

            val request = CreateListingRequest(
                title = draft.title.trim(),
                address = address,
                proximity = draft.proximity,
                university = draft.university.trim().ifBlank { null },
                pricePerMonth = draft.pricePerMonth.toDoubleOrNull(),
                propertyType = draft.propertyType,
                unitsCount = draft.unitsCount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                charges = draft.toCharges(),
                latitude = cleanLat.toDoubleOrNull(),
                longitude = cleanLng.toDoubleOrNull()
            )
            listingRepository.saveListing(
                listingId = _state.value.listingId,
                request = request,
                photos = _state.value.pendingPhotos
            )
                .onSuccess { saved ->
                    ListingDataBus.notifyListingsChanged()
                    _state.update {
                        it.copy(
                            listingId = saved.listing.id,
                            isSaving = false,
                            completed = true,
                            pendingPhotos = if (saved.pendingSync) it.pendingPhotos else emptyList(),
                            syncNote = saved.message,
                            error = null
                        )
                    }
                    // One-shot navigation event — never re-fire from stale state on re-open.
                    _savedEvents.tryEmit(Unit)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = e.userMessage()
                        )
                    }
                }
        }
    }
}