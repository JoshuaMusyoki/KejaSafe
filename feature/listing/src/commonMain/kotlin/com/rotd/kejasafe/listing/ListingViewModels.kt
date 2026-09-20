package com.rotd.kejasafe.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.BookListingRequest
import com.rotd.kejasafe.CampusProximity
import com.rotd.kejasafe.CreateListingDraft
import com.rotd.kejasafe.CreateListingRequest
import com.rotd.kejasafe.CreateLiveTourRequest
import com.rotd.kejasafe.DraftExtraCharge
import com.rotd.kejasafe.ElectricityBilling
import com.rotd.kejasafe.ExtraCharge
import com.rotd.kejasafe.GenerateBillRequest
import com.rotd.kejasafe.IncomingRequest
import com.rotd.kejasafe.ListingAnalytics
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.ListingCalendar
import com.rotd.kejasafe.ListingCharges
import com.rotd.kejasafe.ListingPerformance
import com.rotd.kejasafe.ListingPhotoResponse
import com.rotd.kejasafe.LiveTourMediaType
import com.rotd.kejasafe.PendingListingPhoto
import com.rotd.kejasafe.PickedImage
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.RoommateMatch
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.EscrowState
import com.rotd.kejasafe.EscrowSummary
import com.rotd.kejasafe.InitiatePaymentRequest
import com.rotd.kejasafe.data.EscrowRepository
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.data.PropertyRequestRepository
import com.rotd.kejasafe.domain.listing.DistancePreference
import com.rotd.kejasafe.domain.listing.HouseSize
import com.rotd.kejasafe.domain.listing.HouseType
import com.rotd.kejasafe.domain.listing.ListingType
import com.rotd.kejasafe.domain.listing.PropertyRequest
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.network.ConnectivityMonitor
import com.rotd.kejasafe.network.ListingClient
import com.rotd.kejasafe.network.ListingDataBus
import com.rotd.kejasafe.network.LiveTourClient
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RequestPropertyUiState(
    val size: HouseSize = HouseSize.ONE_BEDROOM,
    val houseType: HouseType? = HouseType.APARTMENT,
    val listingType: ListingType = ListingType.RENT,
    val area: String = "Rungiri, Kabete",
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

class RequestPropertyViewModel(
    private val repository: PropertyRequestRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RequestPropertyUiState())
    val state: StateFlow<RequestPropertyUiState> = _state.asStateFlow()

    init {
        // Pre-fill phone if available in session
        SessionManager.currentUser.value?.phone?.let { phone ->
            _state.update { it.copy(phoneNumber = phone) }
        }
    }

    fun updateSize(size: HouseSize) = _state.update { it.copy(size = size) }
    fun updateHouseType(type: HouseType?) = _state.update { it.copy(houseType = type) }
    fun updateListingType(type: ListingType) = _state.update { it.copy(listingType = type) }
    fun updateArea(area: String) = _state.update { it.copy(area = area) }
    fun updateDistance(dist: DistancePreference) = _state.update { it.copy(distance = dist) }
    fun updateBudgetFrom(from: String) = _state.update { it.copy(budgetFrom = from.filter { it.isDigit() }) }
    fun updateBudgetTo(to: String) = _state.update { it.copy(budgetTo = to.filter { it.isDigit() }) }
    fun updateAdditionalInfo(info: String) = _state.update { it.copy(additionalInfo = info) }
    fun updateAgentsCanCall(canCall: Boolean) = _state.update { it.copy(agentsCanCall = canCall) }
    fun updatePhoneNumber(phone: String) = _state.update { it.copy(phoneNumber = phone) }

    fun submit() {
        val s = _state.value
        if (s.area.isBlank()) {
            _state.update { it.copy(error = "Please specify the area") }
            return
        }
        if (s.phoneNumber.isBlank()) {
            _state.update { it.copy(error = "M-Pesa phone number is required") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val request = PropertyRequest(
                userId = SessionManager.currentUser.value?.id ?: "",
                size = s.size,
                houseType = s.houseType,
                listingType = s.listingType,
                area = s.area,
                distance = s.distance,
                budgetFrom = s.budgetFrom.toLongOrNull() ?: 0,
                budgetTo = s.budgetTo.toLongOrNull() ?: 0,
                additionalInfo = s.additionalInfo.takeIf { it.isNotBlank() },
                agentsCanCall = s.agentsCanCall
            )
            
            repository.submitRequest(s.phoneNumber, request)
                .onSuccess { requestId ->
                    _state.update { 
                        it.copy(
                            isSubmitting = false, 
                            completed = true,
                            successMessage = "Request placed! Check your phone for M-Pesa STK push."
                        ) 
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.userMessage()) }
                }
        }
    }
}

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

data class LandlordHomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Soft banner when showing cached data after a failed refresh. */
    val syncWarning: String? = null,
    val performance: ListingPerformance = ListingPerformance(
        totalViews = 0,
        viewsDeltaPercent = 0,
        inquiries = 0,
        hasNewInquiries = false,
        activeBookings = 0,
        unitsCount = 1
    ),
    val listings: List<PropertyListing> = emptyList(),
    val requests: List<IncomingRequest> = emptyList(),
    /** Confirmed stays where landlord can issue a bill. */
    val confirmedBookings: List<ListingBooking> = emptyList(),
    val billingMessage: String? = null,
    val selectedMatch: RoommateMatch? = null,
    val matchLoading: Boolean = false,
    val matchError: String? = null,
    val matchActing: Boolean = false,
    val rescheduleTourId: String? = null,
    val rescheduleNote: String = "",
    val tourActing: Boolean = false,
    val tourMessage: String? = null,
    /** When set, UI should navigate to the live tour call screen. */
    val navigateToLiveTourId: String? = null,
    val showEscrowDetails: Boolean = false,
    val selectedEscrowSummary: EscrowSummary? = null,
    val isActingEscrow: Boolean = false,
    val escrowError: String? = null
)

data class TenantHomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val syncWarning: String? = null,
    val listings: List<PropertyListing> = emptyList(),
    val searchQuery: String = ""
)

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
    val syncNote: String? = null
)

class LandlordHomeViewModel(
    private val listingRepository: ListingRepository,
    private val escrowRepository: EscrowRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LandlordHomeState())
    val state: StateFlow<LandlordHomeState> = _state.asStateFlow()

    init {
        refresh(showLoading = true)
        viewModelScope.launch {
            ListingDataBus.listingsChanged.collect {
                refresh(showLoading = false)
            }
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                refresh(showLoading = false)
            }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _state.value.listings.isEmpty()) {
                _state.update { it.copy(isLoading = true, error = null, syncWarning = null) }
            } else {
                _state.update { it.copy(error = null, syncWarning = null) }
            }
            listingRepository.loadLandlordHome(
                onCacheHit = { snapshot ->
                    _state.update {
                        it.copy(
                            listings = snapshot.listings,
                            performance = snapshot.performance,
                            requests = snapshot.requests,
                            confirmedBookings = snapshot.confirmedBookings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            )
                .onSuccess { offline ->
                    val snapshot = offline.value
                    _state.update {
                        it.copy(
                            listings = snapshot.listings,
                            performance = snapshot.performance,
                            requests = snapshot.requests,
                            confirmedBookings = snapshot.confirmedBookings,
                            isLoading = false,
                            error = null,
                            syncWarning = offline.syncWarning
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage(),
                            syncWarning = null
                        )
                    }
                }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            ListingClient.approveBooking(requestId)
                .onSuccess {
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            ListingClient.declineBooking(requestId)
                .onSuccess {
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
        }
    }

    fun openRoommateMatch(matchId: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(matchLoading = true, matchError = null, selectedMatch = null)
            }
            ListingClient.getRoommateMatch(matchId)
                .onSuccess { match ->
                    _state.update {
                        it.copy(
                            selectedMatch = match,
                            matchLoading = false,
                            matchError = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            matchLoading = false,
                            matchError = e.userMessage(),
                            selectedMatch = null
                        )
                    }
                }
        }
    }

    fun dismissRoommateMatch() {
        _state.update {
            it.copy(
                selectedMatch = null,
                matchLoading = false,
                matchError = null,
                matchActing = false
            )
        }
    }

    fun approveRoommateMatch(matchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(matchActing = true, matchError = null) }
            ListingClient.approveRoommateMatch(matchId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            matchActing = false,
                            selectedMatch = null,
                            matchLoading = false
                        )
                    }
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(matchActing = false, matchError = e.userMessage())
                    }
                }
        }
    }

    fun declineRoommateMatch(matchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(matchActing = true, matchError = null) }
            ListingClient.declineRoommateMatch(matchId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            matchActing = false,
                            selectedMatch = null,
                            matchLoading = false
                        )
                    }
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(matchActing = false, matchError = e.userMessage())
                    }
                }
        }
    }

    fun confirmLiveTour(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(tourActing = true, tourMessage = null) }
            LiveTourClient.confirmTour(sessionId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            tourActing = false,
                            navigateToLiveTourId = sessionId,
                            tourMessage = "Tour confirmed — joining…"
                        )
                    }
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(tourActing = false, tourMessage = e.userMessage())
                    }
                }
        }
    }

    fun openRescheduleLiveTour(sessionId: String) {
        _state.update {
            it.copy(rescheduleTourId = sessionId, rescheduleNote = "", tourMessage = null)
        }
    }

    fun onRescheduleNoteChange(note: String) {
        _state.update { it.copy(rescheduleNote = note) }
    }

    fun dismissRescheduleLiveTour() {
        _state.update { it.copy(rescheduleTourId = null, rescheduleNote = "") }
    }

    fun submitRescheduleLiveTour() {
        val id = _state.value.rescheduleTourId ?: return
        val note = _state.value.rescheduleNote.trim()
        if (note.isBlank()) {
            _state.update { it.copy(tourMessage = "Enter a new preferred time") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(tourActing = true, tourMessage = null) }
            LiveTourClient.rescheduleTour(id, note)
                .onSuccess {
                    _state.update {
                        it.copy(
                            tourActing = false,
                            rescheduleTourId = null,
                            rescheduleNote = "",
                            tourMessage = "Reschedule sent to tenant"
                        )
                    }
                    ListingDataBus.notifyListingsChanged()
                    refresh(showLoading = false)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(tourActing = false, tourMessage = e.userMessage())
                    }
                }
        }
    }

    fun consumeLiveTourNavigation() {
        _state.update { it.copy(navigateToLiveTourId = null) }
    }

    fun viewEscrowStatus(transactionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActingEscrow = true, escrowError = null) }
            escrowRepository.getEscrowStatus(transactionId)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            isActingEscrow = false,
                            selectedEscrowSummary = response.summary,
                            showEscrowDetails = true
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isActingEscrow = false, escrowError = e.userMessage()) }
                }
        }
    }

    fun dismissEscrowDetails() {
        _state.update { it.copy(showEscrowDetails = false, selectedEscrowSummary = null) }
    }

    fun generateBill(bookingId: String) {
        viewModelScope.launch {
            ListingClient.generateBill(GenerateBillRequest(bookingId = bookingId))
                .onSuccess { bill ->
                    _state.update {
                        it.copy(
                            billingMessage = "Bill issued: KES ${bill.totalAmount.toInt()} · ${bill.month}/${bill.year}"
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(billingMessage = e.userMessage())
                    }
                }
        }
    }
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
                            unitsCount = (listing.unitsCount ?: 1).toString()
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
        if (draft.title.isBlank() || draft.address.isBlank()) {
            _state.update { it.copy(error = "Title and address are required") }
            return
        }
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, syncNote = null) }
            val request = CreateListingRequest(
                title = draft.title.trim(),
                address = draft.address.trim(),
                proximity = draft.proximity,
                university = draft.university.trim().ifBlank { null },
                pricePerMonth = draft.pricePerMonth.toDoubleOrNull(),
                propertyType = draft.propertyType,
                unitsCount = draft.unitsCount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                charges = draft.toCharges()
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

data class ListingAnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: ListingAnalytics? = null,
    val error: String? = null
)

class ListingAnalyticsViewModel(
    private val listingId: String
) : ViewModel() {
    private val _state = MutableStateFlow(ListingAnalyticsUiState())
    val state: StateFlow<ListingAnalyticsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            ListingClient.getListingAnalytics(listingId)
                .onSuccess { analytics ->
                    _state.update {
                        it.copy(isLoading = false, analytics = analytics, error = null)
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
}

data class ListingCalendarUiState(
    val isLoading: Boolean = true,
    val calendar: ListingCalendar? = null,
    val selectedDate: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val error: String? = null,
    val actionMessage: String? = null,
    val isActing: Boolean = false,
    val isLandlordOwner: Boolean = false,
    val notes: String = "",
    val seekingRoommate: Boolean = false,
    val budgetMaxText: String = "",
    /** Tenant: show book dialog when a date is picked. */
    val showBookDialog: Boolean = false,
    val dialogError: String? = null,
    val showLiveTourDialog: Boolean = false,
    val liveTourNote: String = "",
    val liveTourVideo: Boolean = true,
    /** When landlord confirmed, tenant can join this session. */
    val joinableLiveTourId: String? = null,
    val isPayingEscrow: Boolean = false,
    val escrowMessage: String? = null,
    val showPayDepositDialog: Boolean = false,
    val payDepositBookingId: String? = null,
    val payDepositAmount: Double = 0.0,
    val payDepositPhone: String = "",
    val showReleaseDialog: Boolean = false,
    val releaseTransactionId: String? = null,
    val releaseOtp: String = "",
    val showEscrowDetails: Boolean = false,
    val selectedEscrowSummary: EscrowSummary? = null
)

class ListingCalendarViewModel(
    private val listingId: String,
    private val escrowRepository: EscrowRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ListingCalendarUiState())
    val state: StateFlow<ListingCalendarUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun reload() = load(_state.value.year, _state.value.month)

    fun previousMonth() {
        val cal = _state.value.calendar ?: return
        var m = cal.month - 1
        var y = cal.year
        if (m < 1) {
            m = 12
            y -= 1
        }
        load(y, m)
    }

    fun nextMonth() {
        val cal = _state.value.calendar ?: return
        var m = cal.month + 1
        var y = cal.year
        if (m > 12) {
            m = 1
            y += 1
        }
        load(y, m)
    }

    fun selectDate(date: String) {
        val cal = _state.value.calendar
        val forLandlord = _state.value.isLandlordOwner
        // Landlord: only highlight date (no booking UI). Tenant: open book dialog.
        if (forLandlord) {
            _state.update {
                it.copy(selectedDate = if (it.selectedDate == date) null else date)
            }
            return
        }
        val day = cal?.days?.firstOrNull { it.date == date }
        if (day?.fullyBooked == true || cal?.fullyBooked == true) {
            _state.update {
                it.copy(
                    selectedDate = date,
                    actionMessage = "That day is full — pick another date"
                )
            }
            return
        }
        _state.update {
            it.copy(
                selectedDate = date,
                showBookDialog = true,
                dialogError = null,
                notes = "",
                seekingRoommate = false,
                budgetMaxText = "",
                actionMessage = null
            )
        }
    }

    fun dismissBookDialog() {
        _state.update {
            it.copy(
                showBookDialog = false,
                dialogError = null,
                notes = "",
                seekingRoommate = false,
                budgetMaxText = ""
            )
        }
    }

    fun onNotesChange(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun onSeekingRoommateChange(seeking: Boolean) {
        _state.update { it.copy(seekingRoommate = seeking) }
    }

    fun onBudgetMaxChange(text: String) {
        _state.update { it.copy(budgetMaxText = text.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun openLiveTourDialog() {
        _state.update {
            it.copy(
                showLiveTourDialog = true,
                liveTourNote = "",
                liveTourVideo = true,
                dialogError = null
            )
        }
    }

    fun dismissLiveTourDialog() {
        _state.update {
            it.copy(showLiveTourDialog = false, dialogError = null, liveTourNote = "")
        }
    }

    fun onLiveTourNoteChange(note: String) {
        _state.update { it.copy(liveTourNote = note) }
    }

    fun onLiveTourVideoChange(video: Boolean) {
        _state.update { it.copy(liveTourVideo = video) }
    }

    fun requestLiveTour() {
        val note = _state.value.liveTourNote
        val video = _state.value.liveTourVideo
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            LiveTourClient.createTour(
                listingId,
                CreateLiveTourRequest(
                    preferredTimeNote = note,
                    mediaType = if (video) LiveTourMediaType.VIDEO else LiveTourMediaType.AUDIO
                )
            ).onSuccess {
                _state.update {
                    it.copy(
                        isActing = false,
                        showLiveTourDialog = false,
                        liveTourNote = "",
                        actionMessage = "Live tour requested — landlord will Confirm a time"
                    )
                }
                ListingDataBus.notifyListingsChanged()
            }.onFailure { e ->
                _state.update {
                    it.copy(isActing = false, dialogError = e.userMessage())
                }
            }
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    fun requestBooking() {
        val start = _state.value.selectedDate
            ?: return markDialogError("Select a start date first")
        val notes = _state.value.notes
        val seeking = _state.value.seekingRoommate
        val budgetMax = _state.value.budgetMaxText.toDoubleOrNull()?.takeIf { it > 0 }
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            ListingClient.bookListing(
                listingId,
                BookListingRequest(
                    startDate = start,
                    notes = notes,
                    seekingRoommate = seeking,
                    budgetMax = budgetMax
                )
            ).onSuccess {
                _state.update {
                    it.copy(
                        isActing = false,
                        showBookDialog = false,
                        notes = "",
                        seekingRoommate = false,
                        budgetMaxText = "",
                        actionMessage = if (seeking) {
                            "Looking for a compatible roommate — we'll notify the landlord when matched"
                        } else {
                            "Request sent — wait for landlord approval"
                        }
                    )
                }
                ListingDataBus.notifyListingsChanged()
                load(_state.value.year, _state.value.month)
            }.onFailure { e ->
                _state.update {
                    it.copy(isActing = false, dialogError = e.userMessage())
                }
            }
        }
    }

    private fun markDialogError(message: String) {
        _state.update { it.copy(dialogError = message) }
    }

    private fun refreshJoinableLiveTour() {
        viewModelScope.launch {
            LiveTourClient.myOpenTours()
                .onSuccess { tours ->
                    val joinable = tours.firstOrNull {
                        it.listingId == listingId &&
                                (it.status == com.rotd.kejasafe.LiveTourStatus.READY ||
                                        it.status == com.rotd.kejasafe.LiveTourStatus.ACTIVE)
                    }
                    _state.update { it.copy(joinableLiveTourId = joinable?.id) }
                }
        }
    }

    private fun load(year: Int? = null, month: Int? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = it.calendar == null,
                    error = null,
                    year = year,
                    month = month
                )
            }
            ListingClient.getListingCalendar(listingId, year, month)
                .onSuccess { calendar ->
                    val user = SessionManager.currentUser.value
                    _state.update {
                        it.copy(
                            isLoading = false,
                            calendar = calendar,
                            year = calendar.year,
                            month = calendar.month,
                            error = null,
                            isLandlordOwner = false
                        )
                    }
                    if (user?.role == UserRole.LANDLORD || user?.role == UserRole.ADMIN) {
                        refineOwnership(calendar.listingId)
                    }
                    refreshJoinableLiveTour()
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
    fun payDeposit(bookingId: String, amount: Double) {
        val phone = SessionManager.currentUser.value?.phone.orEmpty()
        _state.update {
            it.copy(
                showPayDepositDialog = true,
                payDepositBookingId = bookingId,
                payDepositAmount = amount,
                payDepositPhone = phone,
                dialogError = null,
                escrowMessage = null
            )
        }
    }

    fun dismissPayDepositDialog() {
        if (_state.value.isPayingEscrow) return
        _state.update {
            it.copy(
                showPayDepositDialog = false,
                payDepositBookingId = null,
                payDepositAmount = 0.0,
                dialogError = null
            )
        }
    }

    fun onPayDepositPhoneChange(phone: String) {
        _state.update { it.copy(payDepositPhone = phone) }
    }

    fun confirmPayDeposit() {
        val bookingId = _state.value.payDepositBookingId ?: return
        val amount = _state.value.payDepositAmount
        val phone = _state.value.payDepositPhone.trim()
        if (phone.length < 9) {
            markDialogError("Enter a valid M-Pesa phone number")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isPayingEscrow = true, dialogError = null, escrowMessage = null) }
            val amountCents = (amount * 100).toLong()
            escrowRepository.initiatePayment(InitiatePaymentRequest(bookingId, phone, amountCents))
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            showPayDepositDialog = false,
                            escrowMessage = response.message
                                ?: "STK Push sent. Enter your M-Pesa PIN on your phone."
                        )
                    }
                    observeEscrow(response.transactionId)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isPayingEscrow = false, dialogError = e.userMessage())
                    }
                }
        }
    }

    fun openReleaseDialog(transactionId: String) {
        _state.update {
            it.copy(
                showReleaseDialog = true,
                releaseTransactionId = transactionId,
                releaseOtp = "",
                dialogError = null
            )
        }
    }

    fun dismissReleaseDialog() {
        _state.update { it.copy(showReleaseDialog = false, releaseTransactionId = null) }
    }

    fun onReleaseOtpChange(otp: String) {
        _state.update { it.copy(releaseOtp = otp.take(6).filter { ch -> ch.isDigit() }) }
    }

    fun confirmRelease() {
        val tid = _state.value.releaseTransactionId ?: return
        val otp = _state.value.releaseOtp
        if (otp.length < 4) {
            markDialogError("Enter the 4-digit move-in code")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            escrowRepository.releaseEscrow(tid, otp)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isActing = false,
                            showReleaseDialog = false,
                            actionMessage = "Funds released to landlord. Move-in complete!"
                        )
                    }
                    reload()
                }
                .onFailure { e ->
                    _state.update { it.copy(isActing = false, dialogError = e.userMessage()) }
                }
        }
    }

    fun refreshEscrowStatus(transactionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            escrowRepository.getEscrowStatus(transactionId)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            isActing = false,
                            selectedEscrowSummary = response.summary,
                            showEscrowDetails = true
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isActing = false, dialogError = e.userMessage()) }
                }
        }
    }

    fun dismissEscrowDetails() {
        _state.update { it.copy(showEscrowDetails = false, selectedEscrowSummary = null) }
    }

    fun disputePayment(transactionId: String, reason: String) {
        if (reason.isBlank()) {
            markDialogError("Please provide a reason for the dispute")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            escrowRepository.disputeEscrow(transactionId, reason)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isActing = false,
                            showEscrowDetails = false,
                            actionMessage = "Dispute filed. Payout is frozen until resolved."
                        )
                    }
                    reload()
                }
                .onFailure { e ->
                    _state.update { it.copy(isActing = false, dialogError = e.userMessage()) }
                }
        }
    }

    private fun observeEscrow(transactionId: String) {
        viewModelScope.launch {
            // Prefer live WS updates; also poll in case the socket drops.
            launch {
                escrowRepository.observeTransaction(transactionId).collect { response ->
                    handleEscrowProgress(response.state)
                }
            }
            repeat(40) {
                delay(3_000)
                val result = escrowRepository.getEscrowStatus(transactionId).getOrNull() ?: return@repeat
                if (handleEscrowProgress(result.state)) return@launch
            }
            if (_state.value.isPayingEscrow) {
                _state.update {
                    it.copy(
                        isPayingEscrow = false,
                        escrowMessage = "Still waiting for M-Pesa confirmation. Pull to refresh or check again shortly."
                    )
                }
            }
        }
    }

    /** @return true when payment is no longer pending */
    private fun handleEscrowProgress(state: EscrowState): Boolean {
        return when (state) {
            EscrowState.HELD_IN_ESCROW -> {
                _state.update {
                    it.copy(
                        isPayingEscrow = false,
                        escrowMessage = "Payment successful! Funds held in escrow."
                    )
                }
                reload()
                true
            }
            EscrowState.REFUNDED -> {
                _state.update {
                    it.copy(
                        isPayingEscrow = false,
                        escrowMessage = "M-Pesa payment was cancelled or failed. Try again."
                    )
                }
                true
            }
            else -> false
        }
    }

    private fun refineOwnership(listingId: String) {
        viewModelScope.launch {
            val user = SessionManager.currentUser.value ?: return@launch
            if (user.role != UserRole.LANDLORD && user.role != UserRole.ADMIN) {
                _state.update { it.copy(isLandlordOwner = false) }
                return@launch
            }
            ListingClient.getMyListings()
                .onSuccess { mine ->
                    _state.update {
                        it.copy(isLandlordOwner = mine.any { l -> l.id == listingId })
                    }
                }
        }
    }
}

class TenantHomeViewModel(
    private val listingRepository: ListingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TenantHomeState())
    val state: StateFlow<TenantHomeState> = _state.asStateFlow()

    init {
        refresh(showLoading = true)
        viewModelScope.launch {
            ListingDataBus.listingsChanged.collect {
                refresh(showLoading = false)
            }
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                refresh(showLoading = false)
            }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _state.value.listings.isEmpty()) {
                _state.update { it.copy(isLoading = true, error = null, syncWarning = null) }
            } else {
                _state.update { it.copy(error = null, syncWarning = null) }
            }
            listingRepository.getBrowseListings(
                onCacheHit = { cached ->
                    _state.update {
                        it.copy(listings = cached, isLoading = false, error = null)
                    }
                }
            )
                .onSuccess { offline ->
                    _state.update {
                        it.copy(
                            listings = offline.value,
                            isLoading = false,
                            error = null,
                            syncWarning = offline.syncWarning
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage(),
                            syncWarning = null
                        )
                    }
                }
        }
    }

    fun onSearchChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun filteredListings(): List<PropertyListing> {
        val q = _state.value.searchQuery.trim()
        if (q.isEmpty()) return _state.value.listings
        return _state.value.listings.filter {
            it.title.contains(q, ignoreCase = true) ||
                    it.address.contains(q, ignoreCase = true) ||
                    (it.university?.contains(q, ignoreCase = true) == true)
        }
    }
}
