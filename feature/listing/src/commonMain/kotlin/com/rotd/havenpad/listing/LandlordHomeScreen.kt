package com.rotd.havenpad.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.ListingPerformance
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.reviewRankLabel
import com.rotd.havenpad.components.CenteredListingCardRow
import com.rotd.havenpad.components.CenteredMarketplaceTileRow
import com.rotd.havenpad.components.CompactListingCard
import com.rotd.havenpad.components.HavenPadAppBar
import com.rotd.havenpad.components.ListingStatusBadge
import com.rotd.havenpad.components.HavenPadAppBarContentInset
import com.rotd.havenpad.components.HavenPadDetailPopup
import com.rotd.havenpad.components.HavenPadErrorState
import com.rotd.havenpad.components.HavenPadFloatingNavBar
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.HavenPadOfflineBanner
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.ListingMetricShimmer
import com.rotd.havenpad.components.MarketplaceSectionHeader
import com.rotd.havenpad.components.MarketplaceTileCard
import com.rotd.havenpad.components.MarketplaceTileTone
import com.rotd.havenpad.components.NewInquiriesBadge
import com.rotd.havenpad.components.PerformanceMetricCard
import com.rotd.havenpad.components.ViewsMetricBadge
import com.rotd.havenpad.listing.viewModel.LandlordHomeViewModel
import com.rotd.havenpad.theme.HavenPadTheme
import org.koin.compose.viewmodel.koinViewModel

private enum class LandlordHomePopup {
    VIEWERS,
    INQUIRIES,
    BOOKINGS,
    LISTING
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LandlordHomeScreen(
    selectedTab: HavenPadNavTab = HavenPadNavTab.HOME,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit = {},
    onNavigateToAnalytics: (String) -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: LandlordHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showShimmer = state.isLoading && state.listings.isEmpty()
    val showEmptyListings = !state.isLoading && state.listings.isEmpty() && state.error == null
    val pendingCount = state.requests.size
    val approvedCount = state.confirmedBookings.size
    var activePopup by rememberSaveable { mutableStateOf<LandlordHomePopup?>(null) }
    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh(showLoading = state.listings.isEmpty())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            HavenPadFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = onNavigateToCreateListing
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val useDialog = maxWidth >= 640.dp
            val selectedListing = selectedListingId?.let { id ->
                state.listings.firstOrNull { it.id == id }
            }

            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(showLoading = false) },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HavenPadTheme.spacing.containerMarginMobile,
                    end = HavenPadTheme.spacing.containerMarginMobile,
                    top = HavenPadAppBarContentInset + 12.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null && state.listings.isEmpty()) {
                    item {
                        HavenPadErrorState(
                            message = state.error.orEmpty(),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                if (state.syncWarning != null && state.listings.isNotEmpty()) {
                    item {
                        HavenPadOfflineBanner(
                            message = state.syncWarning.orEmpty(),
                            onRetry = { viewModel.refresh(showLoading = false) }
                        )
                    }
                }

                item {
                    MarketplaceSectionHeader(title = "Listing Performance")
                }

                item {
                    if (showShimmer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            repeat(3) { ListingMetricShimmer() }
                        }
                    } else {
                        LandlordPerformanceTiles(
                            performance = state.performance,
                            pendingCount = pendingCount,
                            approvedCount = approvedCount,
                            onViewers = { activePopup = LandlordHomePopup.VIEWERS },
                            onInquiries = { activePopup = LandlordHomePopup.INQUIRIES },
                            onBookings = { activePopup = LandlordHomePopup.BOOKINGS }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MarketplaceSectionHeader(title = "Your Listings")
                        Button(
                            onClick = onNavigateToCreateListing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
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
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                            ) {
                                repeat(2) {
                                    Box(modifier = Modifier.width(240.dp)) {
                                        ListingMetricShimmer()
                                    }
                                }
                            }
                        }
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
                        item {
                            CenteredListingCardRow(
                                items = state.listings,
                                key = { it.id }
                            ) { listing ->
                                CompactListingCard(
                                    listing = listing,
                                    onView = {
                                        selectedListingId = listing.id
                                        activePopup = LandlordHomePopup.LISTING
                                    },
                                    trailingBadge = {
                                        ListingStatusBadge(status = listing.status)
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            }

            HavenPadAppBar(
                onNotificationClick = onNavigateToNotifications,
                onProfileClick = onNavigateToProfile,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            when (activePopup) {
                LandlordHomePopup.VIEWERS -> {
                    HavenPadDetailPopup(
                        title = "Unique viewers",
                        onDismiss = { activePopup = null },
                        useDialog = useDialog,
                        content = {
                            PerformanceMetricCard(
                                title = "Unique viewers",
                                value = formatWithCommas(state.performance.totalViews),
                                valueColor = MaterialTheme.colorScheme.primary,
                                badge = { ViewsMetricBadge(deltaPercent = state.performance.viewsDeltaPercent) },
                                progress = viewerProgress(state.performance),
                                footer = "People who viewed your listings"
                            )
                        }
                    )
                }
                LandlordHomePopup.INQUIRIES -> {
                    HavenPadDetailPopup(
                        title = "Inquiries",
                        onDismiss = { activePopup = null },
                        useDialog = useDialog,
                        content = {
                            PerformanceMetricCard(
                                title = "Inquiries",
                                value = state.performance.inquiries.toString(),
                                valueColor = MaterialTheme.colorScheme.secondary,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                badge = if (state.performance.hasNewInquiries) {
                                    { NewInquiriesBadge() }
                                } else {
                                    null
                                },
                                footer = inquiryFooter(state.performance)
                            )
                        }
                    )
                }
                LandlordHomePopup.BOOKINGS -> {
                    HavenPadDetailPopup(
                        title = "Bookings",
                        onDismiss = { activePopup = null },
                        useDialog = useDialog,
                        content = {
                            BookingsPopupContent(
                                performance = state.performance,
                                pendingCount = pendingCount,
                                approvedCount = approvedCount
                            )
                        },
                        footer = {
                            HavenPadPrimaryButton(
                                text = "Open bookings",
                                onClick = {
                                    activePopup = null
                                    onNavigateToBookings()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                LandlordHomePopup.LISTING -> selectedListing?.let { listing ->
                    HavenPadDetailPopup(
                        title = listing.title,
                        onDismiss = {
                            activePopup = null
                            selectedListingId = null
                        },
                        useDialog = useDialog,
                        content = {
                            ListingPopupContent(listing = listing)
                        },
                        footer = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HavenPadPrimaryButton(
                                    text = "Edit listing",
                                    onClick = {
                                        activePopup = null
                                        selectedListingId = null
                                        onNavigateToEditListing(listing.id)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        activePopup = null
                                        selectedListingId = null
                                        onNavigateToAnalytics(listing.id)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text("Analytics")
                                }
                            }
                        }
                    )
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun LandlordPerformanceTiles(
    performance: ListingPerformance,
    pendingCount: Int,
    approvedCount: Int,
    onViewers: () -> Unit,
    onInquiries: () -> Unit,
    onBookings: () -> Unit
) {
    CenteredMarketplaceTileRow {
        MarketplaceTileCard(
            title = "Viewers",
            value = formatWithCommas(performance.totalViews),
            subtitle = "+${performance.viewsDeltaPercent}% this week",
            actionLabel = "Details",
            onAction = onViewers,
            tone = MarketplaceTileTone.PRIMARY,
            icon = Icons.Default.Visibility,
            badge = { ViewsMetricBadge(deltaPercent = performance.viewsDeltaPercent) }
        )
        MarketplaceTileCard(
            title = "Inquiries",
            value = performance.inquiries.toString(),
            subtitle = inquiryFooter(performance),
            actionLabel = "Details",
            onAction = onInquiries,
            tone = MarketplaceTileTone.SECONDARY,
            icon = Icons.Default.Groups,
            badge = if (performance.hasNewInquiries) {
                { NewInquiriesBadge() }
            } else {
                null
            }
        )
        MarketplaceTileCard(
            title = "Bookings",
            value = performance.activeBookings.toString(),
            subtitle = when {
                pendingCount > 0 -> "$pendingCount pending · $approvedCount approved"
                approvedCount > 0 -> "$approvedCount approved stay${if (approvedCount == 1) "" else "s"}"
                else -> "Manage requests and stays"
            },
            actionLabel = "Open",
            onAction = onBookings,
            tone = MarketplaceTileTone.NEUTRAL,
            icon = Icons.Default.Event
        )
    }
}

@Composable
private fun BookingsPopupContent(
    performance: ListingPerformance,
    pendingCount: Int,
    approvedCount: Int
) {
    val units = performance.unitsCount.coerceAtLeast(1)
    val segments = units.coerceAtMost(8)
    val filled = ((performance.activeBookings.toFloat() / units) * segments)
        .toInt()
        .coerceIn(0, segments)
    val occupancyPct = ((performance.activeBookings.toDouble() / units) * 100.0)
        .toInt()
        .coerceIn(0, 100)

    PerformanceMetricCard(
        title = "Active Bookings",
        value = performance.activeBookings.toString(),
        valueColor = MaterialTheme.colorScheme.onSurface,
        trailingLabel = "Units: ${performance.unitsCount}",
        segmentedProgress = filled to segments,
        footer = "$occupancyPct% occupancy across your units"
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = when {
            pendingCount > 0 -> "$pendingCount request${if (pendingCount == 1) "" else "s"} waiting for your response."
            approvedCount > 0 -> "$approvedCount approved booking${if (approvedCount == 1) "" else "s"} on the calendar."
            else -> "No pending requests right now."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ListingPopupContent(listing: PropertyListing) {
    Box {
        HousePhotoGallery(listing = listing, heroHeight = 200.dp)
        ListingStatusBadge(
            status = listing.status,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = listing.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = listing.address,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    listing.pricePerMonth?.let { price ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${listing.currency} ${price.toInt()}/mo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Text(
        text = listing.reviewRankLabel(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
    ListingReviewsPanel(listingId = listing.id)
}

private fun viewerProgress(performance: ListingPerformance): Float = when {
    performance.totalViews <= 0 -> 0.05f
    else -> (performance.totalViews / 20f).coerceIn(0.08f, 1f)
}

private fun inquiryFooter(performance: ListingPerformance): String = when {
    performance.inquiries <= 0 -> "People who sent a booking request"
    performance.hasNewInquiries -> "New interest this week"
    else -> "${performance.inquiries} unique tenant${if (performance.inquiries == 1) "" else "s"}"
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
