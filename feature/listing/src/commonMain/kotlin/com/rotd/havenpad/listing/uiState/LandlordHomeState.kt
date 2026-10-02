package com.rotd.havenpad.listing.uiState

import com.rotd.havenpad.EscrowSummary
import com.rotd.havenpad.IncomingRequest
import com.rotd.havenpad.ListingBooking
import com.rotd.havenpad.ListingPerformance
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.RoommateMatch

data class LandlordHomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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
    val actingTourId: String? = null,
    val tourMessage: String? = null,
    val tourMessageIsError: Boolean = false,
    /** When set, UI should navigate to the live tour call screen. */
    val navigateToLiveTourId: String? = null,
    val showEscrowDetails: Boolean = false,
    val selectedEscrowSummary: EscrowSummary? = null,
    val isActingEscrow: Boolean = false,
    val escrowError: String? = null
)