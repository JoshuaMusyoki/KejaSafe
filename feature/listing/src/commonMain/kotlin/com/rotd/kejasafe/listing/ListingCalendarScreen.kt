package com.rotd.kejasafe.listing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.rotd.kejasafe.BookingStatus
import com.rotd.kejasafe.CalendarDaySlot
import com.rotd.kejasafe.EscrowState
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.ListingCalendar
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.components.EscrowDetailsDialog
import com.rotd.kejasafe.components.InsightCard
import com.rotd.kejasafe.components.KejaSafeCard
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeHeaderContentInset
import com.rotd.kejasafe.components.KejaSafeOutlinedButton
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600
import com.rotd.kejasafe.theme.KejaSafeTheme

private fun typeLabel(type: PropertyType): String = when (type) {
    PropertyType.SINGLE_ROOM -> "Single Room"
    PropertyType.BEDSITTER -> "Bedsitter"
    PropertyType.ONE_BEDROOM -> "1 bedroom"
    PropertyType.TWO_BEDROOM -> "2 bedroom"
    PropertyType.THREE_BEDROOM -> "3 bedroom"
    PropertyType.MAISONETTE -> "Maisonette"
}

@Composable
fun ListingCalendarScreen(
    listingId: String,
    onBack: () -> Unit,
    onOpenLiveTour: (sessionId: String) -> Unit = {},
    viewModel: ListingCalendarViewModel = koinViewModel(key = "cal-$listingId") {
        parametersOf(listingId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by SessionManager.currentUser.collectAsStateWithLifecycle()
    val userId = user?.id

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val calendar = state.calendar
            if (state.isLoading && calendar == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null && calendar == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = KejaSafeHeaderContentInset + 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    KejaSafePrimaryButton(text = "Retry", onClick = { viewModel.reload() })
                }
            } else if (calendar != null) {
                CalendarBody(
                    calendar = calendar,
                    selectedDate = state.selectedDate,
                    isLandlordOwner = state.isLandlordOwner,
                    actionMessage = state.actionMessage,
                    myUserId = userId,
                    joinableLiveTourId = state.joinableLiveTourId,
                    isPayingEscrow = state.isPayingEscrow,
                    escrowMessage = state.escrowMessage,
                    onSelectDate = viewModel::selectDate,
                    onPrevMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onRequestLiveTour = viewModel::openLiveTourDialog,
                    onJoinLiveTour = onOpenLiveTour,
                    onPayDeposit = viewModel::payDeposit,
                    onOpenRelease = viewModel::openReleaseDialog,
                    onViewEscrow = viewModel::refreshEscrowStatus,
                    modifier = Modifier.fillMaxSize()
                )
            }

            KejaSafeHeader(
                title = if (state.isLandlordOwner) "Availability" else "Book house",
                onBackClick = onBack,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (state.showBookDialog && !state.isLandlordOwner && calendar != null) {
                BookHouseDialog(
                    date = state.selectedDate.orEmpty(),
                    title = calendar.title,
                    priceLabel = calendar.pricePerMonth?.let {
                        "${calendar.currency} ${it.toInt()}/mo"
                    } ?: "Price on request",
                    notes = state.notes,
                    seekingRoommate = state.seekingRoommate,
                    budgetMaxText = state.budgetMaxText,
                    isActing = state.isActing,
                    error = state.dialogError,
                    onNotesChange = viewModel::onNotesChange,
                    onSeekingRoommateChange = viewModel::onSeekingRoommateChange,
                    onBudgetMaxChange = viewModel::onBudgetMaxChange,
                    onDismiss = viewModel::dismissBookDialog,
                    onConfirm = viewModel::requestBooking
                )
            }

            if (state.showPayDepositDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissPayDepositDialog,
                    title = {
                        Text("Pay deposit via M-Pesa", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "KES ${state.payDepositAmount.toInt()} will be held in escrow. " +
                                    "You'll get an STK Push on your phone — enter your M-Pesa PIN to confirm.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = state.payDepositPhone,
                                onValueChange = viewModel::onPayDepositPhoneChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("M-Pesa phone") },
                                placeholder = { Text("07xxxxxxxx") },
                                enabled = !state.isPayingEscrow,
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            if (!state.dialogError.isNullOrBlank()) {
                                Text(
                                    text = state.dialogError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = viewModel::confirmPayDeposit,
                            enabled = !state.isPayingEscrow
                        ) {
                            Text(if (state.isPayingEscrow) "Sending STK…" else "Send M-Pesa prompt")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::dismissPayDepositDialog,
                            enabled = !state.isPayingEscrow
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (state.showLiveTourDialog && !state.isLandlordOwner) {
                AlertDialog(
                    onDismissRequest = {
                        if (!state.isActing) viewModel.dismissLiveTourDialog()
                    },
                    title = {
                        Text(
                            text = "Request live tour",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Schedule a LiveKit house walkthrough with the landlord. Tours are watermarked — recording/redistribution is prohibited.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Video tour")
                                Switch(
                                    checked = state.liveTourVideo,
                                    onCheckedChange = viewModel::onLiveTourVideoChange,
                                    enabled = !state.isActing
                                )
                            }
                            OutlinedTextField(
                                value = state.liveTourNote,
                                onValueChange = viewModel::onLiveTourNoteChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Preferred time (e.g. Sat 4pm)") },
                                enabled = !state.isActing,
                                maxLines = 3,
                                shape = MaterialTheme.shapes.medium
                            )
                            if (!state.dialogError.isNullOrBlank()) {
                                Text(
                                    text = state.dialogError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = viewModel::requestLiveTour,
                            enabled = !state.isActing
                        ) {
                            Text(if (state.isActing) "Sending…" else "Send request")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::dismissLiveTourDialog,
                            enabled = !state.isActing
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (state.showReleaseDialog) {
                AlertDialog(
                    onDismissRequest = { if (!state.isActing) viewModel.dismissReleaseDialog() },
                    title = { Text("Release Escrow Funds", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Enter the 4-digit verification code provided by your landlord to confirm move-in and release funds.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = state.releaseOtp,
                                onValueChange = viewModel::onReleaseOtpChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Move-in Code") },
                                placeholder = { Text("4 digits") },
                                enabled = !state.isActing,
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            if (!state.dialogError.isNullOrBlank()) {
                                Text(
                                    state.dialogError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::confirmRelease, enabled = !state.isActing) {
                            Text(if (state.isActing) "Releasing..." else "Confirm & Release")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissReleaseDialog() }, enabled = !state.isActing) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (state.showEscrowDetails) {
                EscrowDetailsDialog(
                    summary = state.selectedEscrowSummary,
                    isActing = state.isActing,
                    error = state.dialogError,
                    onDispute = viewModel::disputePayment,
                    onDismiss = viewModel::dismissEscrowDetails
                )
            }
        }
    }
}

@Composable
private fun BookHouseDialog(
    date: String,
    title: String,
    priceLabel: String,
    notes: String,
    seekingRoommate: Boolean,
    budgetMaxText: String,
    isActing: Boolean,
    error: String?,
    onNotesChange: (String) -> Unit,
    onSeekingRoommateChange: (Boolean) -> Unit,
    onBudgetMaxChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isActing) onDismiss() },
        title = {
            Text(
                text = "Request booking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Start date: $date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$priceLabel · about 1 month (landlord confirms exact stay)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Find a roommate",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Match with another seeker on this listing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = seekingRoommate,
                        onCheckedChange = onSeekingRoommateChange,
                        enabled = !isActing
                    )
                }
                if (seekingRoommate) {
                    OutlinedTextField(
                        value = budgetMaxText,
                        onValueChange = onBudgetMaxChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Max budget / month (optional)") },
                        placeholder = { Text("e.g. 15000") },
                        enabled = !isActing,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Note for landlord (optional)") },
                    enabled = !isActing,
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isActing && date.isNotBlank()) {
                Text(if (isActing) "Sending…" else "Send request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isActing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CalendarBody(
    calendar: ListingCalendar,
    selectedDate: String?,
    isLandlordOwner: Boolean,
    actionMessage: String?,
    myUserId: String?,
    joinableLiveTourId: String?,
    isPayingEscrow: Boolean,
    escrowMessage: String?,
    onSelectDate: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRequestLiveTour: () -> Unit,
    onJoinLiveTour: (String) -> Unit,
    onPayDeposit: (String, Double) -> Unit,
    onOpenRelease: (String) -> Unit,
    onViewEscrow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val myBookings = if (isLandlordOwner) {
        emptyList()
    } else {
        calendar.bookings.filter { b ->
            myUserId != null && b.guestUserId == myUserId
        }
    }
    val priceLabel = calendar.pricePerMonth?.let {
        "${calendar.currency} ${it.toInt()}/mo"
    } ?: "Price on request"

    // Tenants: calendar closed until they need a date. Landlords: open by default.
    var calendarExpanded by rememberSaveable(isLandlordOwner) {
        mutableStateOf(isLandlordOwner)
    }

    val metaParts = buildList {
        calendar.propertyType?.let { add(typeLabel(it)) }
        add("${calendar.unitsCount} unit${if (calendar.unitsCount == 1) "" else "s"}")
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = KejaSafeTheme.spacing.containerMarginMobile,
            end = KejaSafeTheme.spacing.containerMarginMobile,
            top = KejaSafeHeaderContentInset + 8.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = calendar.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = calendar.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = priceLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald700
                    )
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = metaParts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            if (calendar.fullyBooked) {
                FullyBookedBanner(
                    units = calendar.unitsCount,
                    active = calendar.activeBookings
                )
            } else {
                AvailabilityBanner(
                    openSlots = calendar.openSlots,
                    units = calendar.unitsCount,
                    active = calendar.activeBookings
                )
            }
        }

        if (!isLandlordOwner) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!joinableLiveTourId.isNullOrBlank()) {
                        KejaSafePrimaryButton(
                            text = "Join live tour",
                            onClick = { onJoinLiveTour(joinableLiveTourId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        KejaSafeOutlinedButton(
                            text = "Request live tour",
                            onClick = onRequestLiveTour,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!calendar.fullyBooked) {
                        DatePickerToggle(
                            selectedDate = selectedDate,
                            expanded = calendarExpanded,
                            onToggle = { calendarExpanded = !calendarExpanded }
                        )
                    }
                }
            }
        } else {
            item {
                DatePickerToggle(
                    selectedDate = selectedDate,
                    expanded = calendarExpanded,
                    labelWhenEmpty = "View availability calendar",
                    labelWhenSelected = selectedDate?.let { "Selected: $it" }.orEmpty(),
                    onToggle = { calendarExpanded = !calendarExpanded }
                )
            }
        }

        if (!actionMessage.isNullOrBlank()) {
            item {
                Text(
                    text = actionMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = calendarExpanded && (isLandlordOwner || !calendar.fullyBooked),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthHeader(
                        label = calendar.monthLabel,
                        onPrev = onPrevMonth,
                        onNext = onNextMonth
                    )
                    MonthGrid(
                        days = calendar.days,
                        selectedDate = selectedDate,
                        onSelect = { date ->
                            onSelectDate(date)
                            if (!isLandlordOwner) calendarExpanded = false
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendDot(color = Emerald700, label = "Open")
                        LegendDot(color = Gold600, label = "Partial")
                        LegendDot(color = MaterialTheme.colorScheme.error, label = "Full")
                    }
                }
            }
        }

        if (!isLandlordOwner) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!escrowMessage.isNullOrBlank()) {
                        Text(
                            text = escrowMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald700
                        )
                    }
                }
            }
            if (myBookings.isEmpty()) {
                item {
                    Text(
                        text = "No requests yet. Pick a start date to book.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myBookings, key = { it.id }) { booking ->
                    TenantBookingRow(
                        booking = booking,
                        isPaying = isPayingEscrow,
                        onPayDeposit = onPayDeposit,
                        onOpenRelease = onOpenRelease,
                        onViewEscrow = onViewEscrow
                    )
                }
            }
        }

        item {
            InsightCard(
                title = if (calendar.fullyBooked) "Fully booked" else "How booking works",
                message = when {
                    isLandlordOwner -> "Unit availability is shown here. Approve new booking requests from your home feed."
                    calendar.fullyBooked -> "All ${calendar.unitsCount} units are filled. Pick another house or try another date."
                    else -> "Choose a start date, send a request, then wait for landlord approval."
                }
            )
        }
    }
}

@Composable
private fun DatePickerToggle(
    selectedDate: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    labelWhenEmpty: String = "Pick a start date",
    labelWhenSelected: String = selectedDate.orEmpty()
) {
    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedDate.isNullOrBlank()) labelWhenEmpty else labelWhenSelected,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "Tap a day below" else "Tap to open calendar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse calendar" else "Expand calendar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FullyBookedBanner(units: Int, active: Int) {
    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = "Fully booked",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "$active / $units units occupied",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AvailabilityBanner(openSlots: Int, units: Int, active: Int) {
    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = Emerald700
            )
            Column {
                Text(
                    text = "$openSlots open unit${if (openSlots == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                )
                Text(
                    text = "$active of $units currently booked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<CalendarDaySlot>,
    selectedDate: String?,
    onSelect: (String) -> Unit
) {
    val weekLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekLabels.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            selected = day.date == selectedDate,
                            onClick = { if (day.inCurrentMonth) onSelect(day.date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDaySlot,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill = when {
        !day.inCurrentMonth -> Color.Transparent
        day.fullyBooked -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        day.bookedCount > 0 -> Gold600.copy(alpha = 0.18f)
        else -> Emerald700.copy(alpha = 0.10f)
    }
    val textColor = when {
        !day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        day.fullyBooked -> MaterialTheme.colorScheme.error
        day.isToday -> Emerald700
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, Emerald700, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = day.inCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || selected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (day.inCurrentMonth && day.bookedCount > 0) {
                Text(
                    text = "${day.bookedCount}/${day.unitsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TenantBookingRow(
    booking: ListingBooking,
    isPaying: Boolean,
    onPayDeposit: (String, Double) -> Unit,
    onOpenRelease: (String) -> Unit,
    onViewEscrow: (String) -> Unit
) {
    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatBookingRange(booking.startDateMillis, booking.endDateMillis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TenantStatusChip(booking.status)
            }

            if (booking.status == BookingStatus.CONFIRMED &&
                (booking.escrowState == null || booking.escrowState == EscrowState.PENDING_PAYMENT)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                KejaSafePrimaryButton(
                    text = when {
                        isPaying -> "Waiting for M-Pesa…"
                        else -> "Pay Deposit to Escrow"
                    },
                    onClick = { booking.pricePerMonth?.let { onPayDeposit(booking.id, it) } },
                    enabled = !isPaying,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (booking.escrowState == EscrowState.HELD_IN_ESCROW) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KejaSafePrimaryButton(
                        text = "Release Funds",
                        onClick = { booking.escrowTransactionId?.let { onOpenRelease(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    KejaSafeOutlinedButton(
                        text = "Details",
                        onClick = { booking.escrowTransactionId?.let { onViewEscrow(it) } },
                        modifier = Modifier.weight(0.6f)
                    )
                }
            } else if (booking.escrowState != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Escrow: ${booking.escrowState!!.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald700,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                KejaSafeOutlinedButton(
                    text = "View escrow details",
                    onClick = { booking.escrowTransactionId?.let { onViewEscrow(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = booking.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TenantStatusChip(status: BookingStatus) {
    val (bg, fg, label) = when (status) {
        BookingStatus.CONFIRMED -> Triple(Emerald700.copy(alpha = 0.14f), Emerald700, "Approved")
        BookingStatus.PENDING -> Triple(Gold600.copy(alpha = 0.16f), Gold600, "Awaiting approval")
        BookingStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            "Declined"
        )
        BookingStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Completed"
        )
    }
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.Bold
    )
}

private fun formatDay(millis: Long): String {
    val day = millis / 86_400_000L
    var days = day.toInt()
    var year = 1970
    while (true) {
        val diy = if (isLeap(year)) 366 else 365
        if (days < diy) break
        days -= diy
        year++
    }
    val monthDays = monthLengths(year)
    var month = 1
    for (len in monthDays) {
        if (days < len) break
        days -= len
        month++
    }
    val d = days + 1
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

private fun isLeap(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun monthLengths(year: Int): IntArray =
    intArrayOf(31, if (isLeap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private fun formatBookingRange(start: Long, end: Long): String =
    "${formatDay(start)} → ${formatDay(end)}"
