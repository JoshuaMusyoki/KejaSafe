package com.rotd.havenpad.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.EscrowState
import com.rotd.havenpad.RequestType
import com.rotd.havenpad.RoommateMatch
import com.rotd.havenpad.components.EscrowDetailsDialog
import com.rotd.havenpad.components.IncomingRequestCard
import com.rotd.havenpad.components.HavenPadCard
import com.rotd.havenpad.components.HavenPadErrorState
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeaderContentInset
import com.rotd.havenpad.components.HavenPadOfflineBanner
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.ListingMetricShimmer
import com.rotd.havenpad.listing.viewModel.LandlordHomeViewModel
import com.rotd.havenpad.theme.Emerald700
import com.rotd.havenpad.theme.HavenPadTheme
import org.koin.compose.viewmodel.koinViewModel

private enum class BookingsTab {
    REQUESTS,
    APPROVED
}

@Composable
fun LandlordBookingsScreen(
    onBack: () -> Unit,
    onOpenLiveTour: (sessionId: String) -> Unit = {},
    viewModel: LandlordHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(BookingsTab.REQUESTS) }
    val showShimmer = state.isLoading && state.requests.isEmpty() && state.confirmedBookings.isEmpty()

    LaunchedEffect(Unit) {
        viewModel.refresh(showLoading = state.requests.isEmpty() && state.confirmedBookings.isEmpty())
    }

    LaunchedEffect(state.navigateToLiveTourId) {
        val id = state.navigateToLiveTourId ?: return@LaunchedEffect
        viewModel.consumeLiveTourNavigation()
        onOpenLiveTour(id)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HavenPadTheme.spacing.containerMarginMobile,
                    end = HavenPadTheme.spacing.containerMarginMobile,
                    top = HavenPadHeaderContentInset + 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OverlappingBookingsTabs(
                        selected = selectedTab,
                        requestsCount = state.requests.size,
                        approvedCount = state.confirmedBookings.size,
                        onSelect = { selectedTab = it }
                    )
                }

                if (state.syncWarning != null) {
                    item {
                        HavenPadOfflineBanner(
                            message = state.syncWarning.orEmpty(),
                            onRetry = { viewModel.refresh(showLoading = false) }
                        )
                    }
                }

                if (state.error != null && state.requests.isEmpty() && state.confirmedBookings.isEmpty()) {
                    item {
                        HavenPadErrorState(
                            message = state.error.orEmpty(),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                if (!state.billingMessage.isNullOrBlank()) {
                    item {
                        Text(
                            text = state.billingMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!state.tourMessage.isNullOrBlank() && state.rescheduleTourId == null) {
                    item {
                        Text(
                            text = state.tourMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.tourMessageIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                when (selectedTab) {
                    BookingsTab.REQUESTS -> {
                        if (showShimmer) {
                            item { ListingMetricShimmer() }
                            item { ListingMetricShimmer() }
                        } else {
                            items(state.requests, key = { it.id }) { request ->
                                val readyToJoin = request.type == RequestType.LIVE_INSPECTION &&
                                    (request.timeLabel == "Ready to join" || request.timeLabel == "In progress")
                                val confirming = state.actingTourId == request.id && state.tourActing
                                val (primary, secondary) = when (request.type) {
                                    RequestType.LIVE_INSPECTION ->
                                        (if (confirming) "Confirming…" else if (readyToJoin) "Join" else "Confirm") to "Reschedule"
                                    RequestType.ROOMMATE_SPLIT -> "View Profiles" to "Decline"
                                    RequestType.BOOKING -> "Accept" to "Decline"
                                    RequestType.GENERAL -> "Respond" to "Dismiss"
                                }
                                IncomingRequestCard(
                                    request = request,
                                    primaryActionLabel = primary,
                                    secondaryActionLabel = secondary,
                                    actionsEnabled = !confirming,
                                    onPrimaryAction = {
                                        when (request.type) {
                                            RequestType.BOOKING -> viewModel.approveRequest(request.id)
                                            RequestType.ROOMMATE_SPLIT -> viewModel.openRoommateMatch(request.id)
                                            RequestType.LIVE_INSPECTION ->
                                                if (readyToJoin) onOpenLiveTour(request.id)
                                                else viewModel.confirmLiveTour(request.id)
                                            else -> Unit
                                        }
                                    },
                                    onSecondaryAction = {
                                        when (request.type) {
                                            RequestType.BOOKING -> viewModel.declineRequest(request.id)
                                            RequestType.ROOMMATE_SPLIT -> viewModel.declineRoommateMatch(request.id)
                                            RequestType.LIVE_INSPECTION -> viewModel.openRescheduleLiveTour(request.id)
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                            if (state.requests.isEmpty()) {
                                item {
                                    EmptyBookingsMessage("No pending booking requests.")
                                }
                            }
                        }
                    }

                    BookingsTab.APPROVED -> {
                        if (showShimmer) {
                            item { ListingMetricShimmer() }
                        } else {
                            items(state.confirmedBookings, key = { "c-${it.id}" }) { booking ->
                                HavenPadCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = booking.guestName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = booking.listingTitle.ifBlank { "Property" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (booking.escrowState != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Emerald700.copy(alpha = 0.08f))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "Escrow: ${booking.escrowState!!.name.replace('_', ' ')}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Emerald700,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                if (booking.escrowState == EscrowState.HELD_IN_ESCROW && !booking.escrowVerificationOtp.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Move-in Code: ${booking.escrowVerificationOtp}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "Give this code to the student upon arrival to release funds.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Text(
                                                    text = "View payment details →",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Emerald700,
                                                    modifier = Modifier
                                                        .padding(top = 8.dp)
                                                        .clickable { booking.escrowTransactionId?.let { viewModel.viewEscrowStatus(it) } }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HavenPadPrimaryButton(
                                            text = "Generate bill",
                                            onClick = { viewModel.generateBill(booking.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            if (state.confirmedBookings.isEmpty()) {
                                item {
                                    EmptyBookingsMessage("No approved stays yet.")
                                }
                            }
                        }
                    }
                }
            }

            HavenPadHeader(
                title = "Bookings",
                onBackClick = onBack,
                frosted = false,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            when {
                state.matchLoading -> {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissRoommateMatch,
                        title = { Text("Roommate pair") },
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = viewModel::dismissRoommateMatch) {
                                Text("Close")
                            }
                        }
                    )
                }
                state.selectedMatch != null || !state.matchError.isNullOrBlank() -> {
                    RoommateMatchDialog(
                        match = state.selectedMatch,
                        error = state.matchError,
                        isActing = state.matchActing,
                        onDismiss = viewModel::dismissRoommateMatch,
                        onApprove = { id -> viewModel.approveRoommateMatch(id) },
                        onDecline = { id -> viewModel.declineRoommateMatch(id) }
                    )
                }
            }

            if (state.rescheduleTourId != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!state.tourActing) viewModel.dismissRescheduleLiveTour()
                    },
                    title = { Text("Reschedule live tour") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Suggest a new time for the tenant.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = state.rescheduleNote,
                                onValueChange = viewModel::onRescheduleNoteChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Saturday 3pm") },
                                enabled = !state.tourActing,
                                maxLines = 3
                            )
                            if (!state.tourMessage.isNullOrBlank()) {
                                Text(
                                    text = state.tourMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = viewModel::submitRescheduleLiveTour,
                            enabled = !state.tourActing
                        ) {
                            Text(if (state.tourActing) "Sending…" else "Send")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::dismissRescheduleLiveTour,
                            enabled = !state.tourActing
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (state.showEscrowDetails) {
                EscrowDetailsDialog(
                    summary = state.selectedEscrowSummary,
                    isActing = state.isActingEscrow,
                    error = state.escrowError,
                    onDismiss = viewModel::dismissEscrowDetails
                )
            }
        }
    }
}

@Composable
private fun RoommateMatchDialog(
    match: RoommateMatch?,
    error: String?,
    isActing: Boolean,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isActing) onDismiss() },
        title = {
            Text(
                text = if (match != null) "${match.score}% match" else "Roommate pair",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (match != null) {
                    Text(
                        text = match.listingTitle.ifBlank { "Property" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    match.pricePerMonth?.let { price ->
                        Text(
                            text = "${match.currency} ${price.toInt()}/mo shared unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    match.guests.forEach { guest ->
                        HavenPadCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = guest.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val meta = listOfNotNull(
                                    guest.major?.takeIf { it.isNotBlank() },
                                    guest.yearOfStudy?.let { "Year $it" }
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${formatMatchDay(guest.startDateMillis)} → ${formatMatchDay(guest.endDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                guest.budgetMax?.let { budget ->
                                    Text(
                                        text = "Budget up to ${match.currency} ${budget.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (guest.preferences.isNotEmpty()) {
                                    Text(
                                        text = guest.preferences.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (match != null) {
                TextButton(
                    onClick = { onApprove(match.id) },
                    enabled = !isActing
                ) {
                    Text(if (isActing) "Working…" else "Approve pair")
                }
            }
        },
        dismissButton = {
            Row {
                if (match != null) {
                    TextButton(
                        onClick = { onDecline(match.id) },
                        enabled = !isActing
                    ) {
                        Text("Decline")
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isActing) {
                    Text("Close")
                }
            }
        }
    )
}

private fun formatMatchDay(millis: Long): String {
    val day = millis / 86_400_000L
    var days = day.toInt()
    var year = 1970
    while (true) {
        val diy = if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 366 else 365
        if (days < diy) break
        days -= diy
        year++
    }
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (len in monthDays) {
        if (days < len) break
        days -= len
        month++
    }
    val d = days + 1
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

@Composable
private fun EmptyBookingsMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center
    )
}

/**
 * Segmented tabs with a slight overlap so the selected chip sits on top.
 * Avoids Modifier.shadow (CMP graphics-layer bug that flips / clips text).
 */
@Composable
private fun OverlappingBookingsTabs(
    selected: BookingsTab,
    requestsCount: Int,
    approvedCount: Int,
    onSelect: (BookingsTab) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(track)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OverlappingTabChip(
            label = "Requests",
            count = requestsCount,
            selected = selected == BookingsTab.REQUESTS,
            onClick = { onSelect(BookingsTab.REQUESTS) },
            modifier = Modifier
                .weight(1f)
                .zIndex(if (selected == BookingsTab.REQUESTS) 1f else 0f)
                .offset(x = 2.dp)
        )
        OverlappingTabChip(
            label = "Approved",
            count = approvedCount,
            selected = selected == BookingsTab.APPROVED,
            onClick = { onSelect(BookingsTab.APPROVED) },
            modifier = Modifier
                .weight(1f)
                .zIndex(if (selected == BookingsTab.APPROVED) 1f else 0f)
                .offset(x = (-2).dp)
        )
    }
}

@Composable
private fun OverlappingTabChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (selected) Emerald700 else Color.Transparent
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label ($count)" else label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
