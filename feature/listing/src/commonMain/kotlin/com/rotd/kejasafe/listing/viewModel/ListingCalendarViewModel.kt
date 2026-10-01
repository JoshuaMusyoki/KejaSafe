package com.rotd.kejasafe.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.BookListingRequest
import com.rotd.kejasafe.CreateLiveTourRequest
import com.rotd.kejasafe.EscrowState
import com.rotd.kejasafe.InitiatePaymentRequest
import com.rotd.kejasafe.LiveTourMediaType
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.data.EscrowRepository
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.listing.uiState.ListingCalendarUiState
import com.rotd.kejasafe.network.ListingClient
import com.rotd.kejasafe.network.ListingDataBus
import com.rotd.kejasafe.network.LiveTourClient
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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