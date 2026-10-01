package com.rotd.kejasafe.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.GenerateBillRequest
import com.rotd.kejasafe.LocationProvider
import com.rotd.kejasafe.data.EscrowRepository
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.listing.uiState.LandlordHomeState
import com.rotd.kejasafe.network.ConnectivityMonitor
import com.rotd.kejasafe.network.ListingClient
import com.rotd.kejasafe.network.ListingDataBus
import com.rotd.kejasafe.network.LiveTourClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LandlordHomeViewModel(
    private val listingRepository: ListingRepository,
    private val escrowRepository: EscrowRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LandlordHomeState())
    val state: StateFlow<LandlordHomeState> = _state.asStateFlow()
    private var refreshGeneration = 0

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
        val generation = ++refreshGeneration
        viewModelScope.launch {
            val firstLoad = showLoading && _state.value.listings.isEmpty()
            _state.update {
                it.copy(
                    isLoading = firstLoad,
                    isRefreshing = !firstLoad,
                    error = null,
                    syncWarning = null
                )
            }
            listingRepository.loadLandlordHome(
                onCacheHit = { snapshot ->
                    if (generation == refreshGeneration && _state.value.listings.isEmpty()) {
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
                }
            )
                .onSuccess { offline ->
                    if (generation != refreshGeneration) return@onSuccess
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
                    if (generation != refreshGeneration) return@onFailure
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage(),
                            syncWarning = null
                        )
                    }
                }
            if (generation == refreshGeneration) {
                _state.update { it.copy(isRefreshing = false, isLoading = false) }
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