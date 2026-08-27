package com.rotd.kejasafe.listing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.kejasafe.CampusProximity
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.components.KejaSafeAppBar
import com.rotd.kejasafe.components.KejaSafeAppBarContentInset
import com.rotd.kejasafe.components.KejaSafeCard
import com.rotd.kejasafe.components.KejaSafeErrorState
import com.rotd.kejasafe.components.KejaSafeFloatingNavBar
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.components.KejaSafeOfflineBanner
import com.rotd.kejasafe.components.LandlordListingCard
import com.rotd.kejasafe.components.LandlordListingsShimmer
import com.rotd.kejasafe.components.ListingMetricShimmer
import com.rotd.kejasafe.components.NewInquiriesBadge
import com.rotd.kejasafe.components.PerformanceMetricCard
import com.rotd.kejasafe.components.ViewsMetricBadge
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.Slate900
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LandlordHomeScreen(
    selectedTab: KejaSafeNavTab = KejaSafeNavTab.HOME,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit = {},
    onNavigateToAnalytics: (String) -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    viewModel: LandlordHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showShimmer = state.isLoading && state.listings.isEmpty()
    val showEmptyListings = !state.isLoading && state.listings.isEmpty() && state.error == null
    var performanceExpanded by rememberSaveable { mutableStateOf(false) }
    val pendingCount = state.requests.size
    val approvedCount = state.confirmedBookings.size

    LaunchedEffect(Unit) {
        viewModel.refresh(showLoading = state.listings.isEmpty())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            KejaSafeFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = onNavigateToCreateListing
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = KejaSafeTheme.spacing.containerMarginMobile,
                    end = KejaSafeTheme.spacing.containerMarginMobile,
                    top = KejaSafeAppBarContentInset + 12.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null && state.listings.isEmpty()) {
                    item {
                        KejaSafeErrorState(
                            message = state.error.orEmpty(),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                if (state.syncWarning != null && state.listings.isNotEmpty()) {
                    item {
                        KejaSafeOfflineBanner(
                            message = state.syncWarning.orEmpty(),
                            onRetry = { viewModel.refresh(showLoading = false) }
                        )
                    }
                }

                item {
                    CollapsiblePerformanceHeader(
                        expanded = performanceExpanded,
                        summary = performanceSummary(state.performance),
                        onToggle = { performanceExpanded = !performanceExpanded }
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = performanceExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (showShimmer) {
                                repeat(3) { ListingMetricShimmer() }
                            } else {
                                val perf = state.performance
                                val viewProgress = when {
                                    perf.totalViews <= 0 -> 0.05f
                                    else -> (perf.totalViews / 20f).coerceIn(0.08f, 1f)
                                }
                                PerformanceMetricCard(
                                    title = "Unique viewers",
                                    value = formatWithCommas(perf.totalViews),
                                    valueColor = Emerald700,
                                    badge = { ViewsMetricBadge(deltaPercent = perf.viewsDeltaPercent) },
                                    progress = viewProgress,
                                    footer = "People who viewed your listings"
                                )
                                PerformanceMetricCard(
                                    title = "Inquiries",
                                    value = perf.inquiries.toString(),
                                    valueColor = Gold600,
                                    accentColor = Gold600,
                                    badge = if (perf.hasNewInquiries) {
                                        { NewInquiriesBadge() }
                                    } else {
                                        null
                                    },
                                    footer = when {
                                        perf.inquiries <= 0 -> "People who sent a booking request"
                                        perf.hasNewInquiries -> "New interest this week"
                                        else -> "${perf.inquiries} unique tenant${if (perf.inquiries == 1) "" else "s"}"
                                    }
                                )
                                val units = perf.unitsCount.coerceAtLeast(1)
                                val segments = units.coerceAtMost(8)
                                val filled = (
                                    (perf.activeBookings.toFloat() / units) * segments
                                    )
                                    .toInt()
                                    .coerceIn(0, segments)
                                val occupancyPct = (
                                    (perf.activeBookings.toDouble() / units) * 100.0
                                    ).toInt().coerceIn(0, 100)
                                PerformanceMetricCard(
                                    title = "Active Bookings",
                                    value = perf.activeBookings.toString(),
                                    valueColor = Slate900,
                                    trailingLabel = "Units: ${perf.unitsCount}",
                                    segmentedProgress = filled to segments,
                                    footer = "$occupancyPct% occupancy across your units"
                                )
                            }
                        }
                    }
                }

                item {
                    BookingsEntryCard(
                        pendingCount = pendingCount,
                        approvedCount = approvedCount,
                        onClick = onNavigateToBookings
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Listings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onNavigateToCreateListing,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Listing")
                        }
                    }
                }

                when {
                    showShimmer -> {
                        item { LandlordListingsShimmer(count = 2) }
                    }
                    showEmptyListings -> {
                        item {
                            Text(
                                text = "No listings yet. Tap New Listing to add one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        items(state.listings, key = { it.id }) { listing ->
                            LandlordListingCard(
                                listing = listing,
                                onEdit = { onNavigateToEditListing(listing.id) },
                                onViewAnalytics = { onNavigateToAnalytics(listing.id) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            KejaSafeAppBar(
                onTuneClick = {},
                onProfileClick = onNavigateToProfile,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun CollapsiblePerformanceHeader(
    expanded: Boolean,
    summary: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Listing Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!expanded) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse performance" else "Expand performance",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BookingsEntryCard(
    pendingCount: Int,
    approvedCount: Int,
    onClick: () -> Unit
) {
    KejaSafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        pendingCount > 0 ->
                            "$pendingCount pending · $approvedCount approved"
                        approvedCount > 0 ->
                            "$approvedCount approved stay${if (approvedCount == 1) "" else "s"}"
                        else -> "Manage requests and approved stays"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelLarge,
                color = Emerald700,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun performanceSummary(perf: com.rotd.kejasafe.ListingPerformance): String {
    val viewers = "${perf.totalViews} viewer${if (perf.totalViews == 1) "" else "s"}"
    val inquiries = "${perf.inquiries} inquir${if (perf.inquiries == 1) "y" else "ies"}"
    val bookings = "${perf.activeBookings} active"
    return "$viewers · $inquiries · $bookings"
}

fun CampusProximity.displayLabel(): String = when (this) {
    CampusProximity.INSIDE_CAMPUS -> "Inside Campus"
    CampusProximity.WALKING_DISTANCE -> "Walking Distance"
    CampusProximity.SHORT_COMMUTE -> "Short Commute"
    CampusProximity.FAR -> "Far from Campus"
}

fun PropertyType.displayLabel(): String = when (this) {
    PropertyType.SINGLE_ROOM -> "Single room"
    PropertyType.BEDSITTER -> "Bedsitter"
    PropertyType.ONE_BEDROOM -> "1 bedroom"
    PropertyType.TWO_BEDROOM -> "2 bedroom"
    PropertyType.THREE_BEDROOM -> "3 bedroom"
    PropertyType.MAISONETTE -> "Maisonette"
}

private fun formatWithCommas(value: Int): String =
    value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
