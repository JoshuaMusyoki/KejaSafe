package com.rotd.kejasafe.listing

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.rotd.kejasafe.RequestType
import com.rotd.kejasafe.components.IncomingRequestCard
import com.rotd.kejasafe.components.KejaSafeCard
import com.rotd.kejasafe.components.KejaSafeErrorState
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeHeaderContentInset
import com.rotd.kejasafe.components.KejaSafeOfflineBanner
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.ListingMetricShimmer
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.KejaSafeTheme
import org.koin.compose.viewmodel.koinViewModel

private enum class BookingsTab {
    REQUESTS,
    APPROVED
}

@Composable
fun LandlordBookingsScreen(
    onBack: () -> Unit,
    viewModel: LandlordHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(BookingsTab.REQUESTS) }
    val showShimmer = state.isLoading && state.requests.isEmpty() && state.confirmedBookings.isEmpty()

    LaunchedEffect(Unit) {
        viewModel.refresh(showLoading = state.requests.isEmpty() && state.confirmedBookings.isEmpty())
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
                    start = KejaSafeTheme.spacing.containerMarginMobile,
                    end = KejaSafeTheme.spacing.containerMarginMobile,
                    top = KejaSafeHeaderContentInset + 12.dp,
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
                        KejaSafeOfflineBanner(
                            message = state.syncWarning.orEmpty(),
                            onRetry = { viewModel.refresh(showLoading = false) }
                        )
                    }
                }

                if (state.error != null && state.requests.isEmpty() && state.confirmedBookings.isEmpty()) {
                    item {
                        KejaSafeErrorState(
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

                when (selectedTab) {
                    BookingsTab.REQUESTS -> {
                        if (showShimmer) {
                            item { ListingMetricShimmer() }
                            item { ListingMetricShimmer() }
                        } else {
                            items(state.requests, key = { it.id }) { request ->
                                val (primary, secondary) = when (request.type) {
                                    RequestType.LIVE_INSPECTION -> "Confirm" to "Reschedule"
                                    RequestType.ROOMMATE_SPLIT -> "View Profiles" to "Decline"
                                    RequestType.BOOKING -> "Accept" to "Decline"
                                    RequestType.GENERAL -> "Respond" to "Dismiss"
                                }
                                IncomingRequestCard(
                                    request = request,
                                    primaryActionLabel = primary,
                                    secondaryActionLabel = secondary,
                                    onPrimaryAction = {
                                        if (request.type == RequestType.BOOKING) {
                                            viewModel.approveRequest(request.id)
                                        }
                                    },
                                    onSecondaryAction = {
                                        if (request.type == RequestType.BOOKING) {
                                            viewModel.declineRequest(request.id)
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
                                KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
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
                                        Spacer(modifier = Modifier.height(10.dp))
                                        KejaSafePrimaryButton(
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

            KejaSafeHeader(
                title = "Bookings",
                onBackClick = onBack,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
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
