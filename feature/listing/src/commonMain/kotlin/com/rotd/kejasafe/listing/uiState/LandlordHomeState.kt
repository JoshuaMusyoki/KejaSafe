package com.rotd.kejasafe.listing.uiState

import com.rotd.kejasafe.EscrowSummary
import com.rotd.kejasafe.IncomingRequest
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.ListingPerformance
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.RoommateMatch

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
    val tourMessage: String? = null,
    /** When set, UI should navigate to the live tour call screen. */
    val navigateToLiveTourId: String? = null,
    val showEscrowDetails: Boolean = false,
    val selectedEscrowSummary: EscrowSummary? = null,
    val isActingEscrow: Boolean = false,
    val escrowError: String? = null
)