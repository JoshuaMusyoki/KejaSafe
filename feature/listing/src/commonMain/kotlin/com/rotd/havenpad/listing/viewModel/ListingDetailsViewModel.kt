package com.rotd.havenpad.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.BookListingRequest
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.network.ListingClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingDetailsState(
    val isLoading: Boolean = true,
    val listing: PropertyListing? = null,
    val error: String? = null,
    val showBookDialog: Boolean = false,
    val isActing: Boolean = false,
    val actionMessage: String? = null,
    val dialogError: String? = null,
    val selectedStartDate: String = "2026-10-27",
    val notes: String = "",
    val occupantsCount: Int = 1,
    val isCohabiting: Boolean = false
)

class ListingDetailsViewModel(
    private val listingId: String
) : ViewModel() {
    private val _state = MutableStateFlow(ListingDetailsState())
    val state: StateFlow<ListingDetailsState> = _state.asStateFlow()

    init {
        loadListing()
    }

    fun loadListing() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            ListingClient.getListing(listingId)
                .onSuccess { listing ->
                    _state.update { it.copy(isLoading = false, listing = listing) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load listing") }
                }
        }
    }

    fun openBookDialog() {
        _state.update { it.copy(showBookDialog = true) }
    }

    fun dismissBookDialog() {
        _state.update { it.copy(showBookDialog = false, dialogError = null) }
    }

    fun onStartDateChange(date: String) {
        _state.update { it.copy(selectedStartDate = date) }
    }

    fun onNotesChange(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun onOccupantsChange(count: Int) {
        val c = count.coerceAtLeast(1)
        _state.update { it.copy(occupantsCount = c, isCohabiting = c > 1) }
    }

    fun requestBooking() {
        val st = _state.value
        val startDate = st.selectedStartDate.ifBlank { "2026-10-27" }
        viewModelScope.launch {
            _state.update { it.copy(isActing = true, dialogError = null) }
            ListingClient.bookListing(
                listingId,
                BookListingRequest(
                    startDate = startDate,
                    notes = st.notes,
                    slotCount = st.occupantsCount,
                    occupantsCount = st.occupantsCount,
                    isCohabiting = st.isCohabiting
                )
            ).onSuccess {
                _state.update {
                    it.copy(
                        isActing = false,
                        showBookDialog = false,
                        actionMessage = "Booking request sent for ${st.occupantsCount} occupant(s) (Cohabiting: ${st.isCohabiting}) — wait for landlord approval"
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isActing = false, dialogError = e.message ?: "Failed to book") }
            }
        }
    }
}
