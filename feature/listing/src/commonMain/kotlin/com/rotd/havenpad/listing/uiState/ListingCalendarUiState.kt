package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.EscrowSummary
import com.rotd.havenpad.ListingCalendar

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