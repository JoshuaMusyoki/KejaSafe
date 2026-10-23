package com.rotd.havenpad.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.components.AirbnbCategoryRow
import com.rotd.havenpad.components.AirbnbFeaturedBanner
import com.rotd.havenpad.components.AirbnbListingCard
import com.rotd.havenpad.components.HavenPadAppBar
import com.rotd.havenpad.components.HavenPadAppBarContentInset
import com.rotd.havenpad.components.HavenPadDetailPopup
import com.rotd.havenpad.components.HavenPadErrorState
import com.rotd.havenpad.components.HavenPadFloatingNavBar
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.HavenPadOfflineBanner
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.InsightCard
import com.rotd.havenpad.components.ListingStatusBadge
import com.rotd.havenpad.components.MarketplaceSectionHeader
import com.rotd.havenpad.components.TenantListingsShimmer
import com.rotd.havenpad.listing.viewModel.TenantHomeViewModel
import com.rotd.havenpad.rememberLocationPermissionState
import com.rotd.havenpad.reviewRankLabel
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.utils.calculateDistance
import org.koin.compose.viewmodel.koinViewModel

private enum class TenantHomePopup {
    INSIGHT,
    REQUEST_PROPERTY
}

@Composable
fun TenantHomeScreen(
    selectedTab: HavenPadNavTab = HavenPadNavTab.HOME,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProperty: (String) -> Unit,
    onNavigateToSearch: (query: String, latitude: Double?, longitude: Double?) -> Unit,
    onNavigateToRequestProperty: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onTextLandlord: (PropertyListing) -> Unit = {},
    signedIn: Boolean = true,
    embedInDashboard: Boolean = false,
    viewModel: TenantHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showShimmer = state.isLoading && state.listings.isEmpty()
    val showEmpty = !state.isLoading && state.listings.isEmpty() && state.error == null
    var activePopup by rememberSaveable { mutableStateOf<TenantHomePopup?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoriteIds by rememberSaveable { mutableStateOf(com.rotd.havenpad.FavoriteStore.read()) }

    val openSearch = {
        viewModel.openSearchOverlay()
    }
    val locationPermission = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.requestDeviceLocation()
        }
    }
    LaunchedEffect(locationPermission.isGranted) {
        if (locationPermission.isGranted) viewModel.refreshDeviceLocation()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!embedInDashboard) {
                HavenPadFloatingNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val useDialog = maxWidth >= 640.dp

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
                        top = HavenPadAppBarContentInset + 16.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Airbnb Floating Search Bar
                    item {
                        GlassmorphicSearchField(
                            query = state.searchQuery,
                            placeholder = "Start your search",
                            onClick = openSearch,
                            enabled = !state.isRefreshing
                        )
                    }

                    // 2. Category Filter Chips Carousel
                    item {
                        AirbnbCategoryRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { item ->
                                selectedCategory = item.label
                                if (item.label == "All") viewModel.onSearchChange("")
                                else viewModel.onSearchChange(item.label)
                            }
                        )
                    }

                    // 3. Featured Highlight Banner Card ("Homes")
                    item {
                        AirbnbFeaturedBanner(
                            title = "Homes",
                            subtitle = "Complete places near campus",
                            details = "Verified student housing · M-Pesa escrow",
                            imageUrl = state.listings.firstOrNull()?.imageUrl,
                            onClick = openSearch
                        )
                    }

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

                    // 4. Recently Viewed Section
                    if (state.listings.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MarketplaceSectionHeader(
                                    title = "Recently viewed",
                                    onAction = openSearch
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(
                                        items = state.visibleListings.take(6),
                                        key = { "recent_${it.id}" }
                                    ) { listing ->
                                        AirbnbListingCard(
                                            listing = listing,
                                            distanceLabel = distanceLabel(
                                                listing,
                                                state.userLatitude,
                                                state.userLongitude,
                                                state.distancesReady
                                            ),
                                            isFavorite = favoriteIds.contains(listing.id),
                                            onToggleFavorite = {
                                                favoriteIds = if (favoriteIds.contains(listing.id)) {
                                                    favoriteIds - listing.id
                                                } else {
                                                    favoriteIds + listing.id
                                                }
                                                com.rotd.havenpad.FavoriteStore.save(favoriteIds)
                                            },
                                            onView = {
                                                onNavigateToProperty(listing.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Main Section Header ("Homes near campus with free cancellation")
                    item {
                        val locationName = state.userLocationName?.takeIf { it.isNotBlank() } ?: "Campus"
                        MarketplaceSectionHeader(
                            title = "$locationName homes with free cancellation",
                            onAction = openSearch
                        )
                    }

                    when {
                        showShimmer -> {
                            item { TenantListingsShimmer(count = 3) }
                        }
                        showEmpty -> {
                            item {
                                Text(
                                    text = "No houses available right now. Check back soon.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            if (state.showingDistantSuggestions) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "No listings found within 30km of your location.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Here are some suggestions around you:",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Horizontal scroll row matching Airbnb listings
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(
                                        items = state.visibleListings,
                                        key = { "free_cancel_${it.id}" }
                                    ) { listing ->
                                        AirbnbListingCard(
                                            listing = listing,
                                            distanceLabel = distanceLabel(
                                                listing,
                                                state.userLatitude,
                                                state.userLongitude,
                                                state.distancesReady
                                            ),
                                            isFavorite = favoriteIds.contains(listing.id),
                                            onToggleFavorite = {
                                                favoriteIds = if (favoriteIds.contains(listing.id)) {
                                                    favoriteIds - listing.id
                                                } else {
                                                    favoriteIds + listing.id
                                                }
                                                com.rotd.havenpad.FavoriteStore.save(favoriteIds)
                                            },
                                            onView = {
                                                onNavigateToProperty(listing.id)
                                            }
                                        )
                                    }
                                }
                            }

                            if (state.visibleListings.isEmpty() && state.searchQuery.isNotBlank()) {
                                item {
                                    Text(
                                        text = "No houses match “${state.searchQuery.trim()}”.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            FloatingActionButton(
                onClick = openSearch,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 16.dp)
                    .size(56.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search houses"
                )
            }

            // Airbnb Search Overlay Sheet (Opens on the same page!)
            AirbnbSearchOverlay(
                isOpen = state.isSearchOverlayOpen,
                onClose = viewModel::closeSearchOverlay,
                initialDestination = state.destinationQuery.ifBlank { state.searchQuery },
                initialDateText = state.moveInDateText,
                initialRoommates = state.roommatesCount,
                recentSearches = state.recentSearches,
                onApplySearch = { dest, dates, roommates ->
                    viewModel.applySearchFilters(dest, dates, roommates)
                },
                onClearAll = viewModel::clearSearchFilters
            )

            when (activePopup) {
                TenantHomePopup.INSIGHT -> {
                    HavenPadDetailPopup(
                        title = "Student Insight",
                        onDismiss = { activePopup = null },
                        useDialog = useDialog,
                        content = {
                            InsightCard(
                                title = "Student Insight",
                                message = "Listings within a 10-minute walk of campus receive 4x more inquiries from verified students."
                            )
                        }
                    )
                }
                TenantHomePopup.REQUEST_PROPERTY -> {
                    HavenPadDetailPopup(
                        title = "Request custom property",
                        onDismiss = { activePopup = null },
                        useDialog = useDialog,
                        content = {
                            Text(
                                text = "Secure your room near campus with M-Pesa escrow protection and verified landlord compliance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        footer = {
                            HavenPadPrimaryButton(
                                text = "Start request",
                                onClick = {
                                    activePopup = null
                                    onNavigateToRequestProperty()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
                null -> Unit
            }
        }

        if (state.showLocationPrompt) {
            LocationOnboardingSheet(
                onDismiss = viewModel::dismissLocationPrompt,
                onUseCurrentLocation = {
                    if (locationPermission.isGranted) {
                        viewModel.requestDeviceLocation()
                    } else {
                        locationPermission.launchPermissionRequest()
                    }
                },
                onPickManual = viewModel::setManualLocation
            )
        }
    }
}

private fun distanceLabel(
    listing: PropertyListing,
    userLatitude: Double?,
    userLongitude: Double?,
    distancesReady: Boolean
): String? {
    if (!distancesReady && listing.address.isNotBlank()) return null
    return userLatitude?.let { lat ->
        userLongitude?.let { lng ->
            val km = calculateDistance(lat, lng, listing.latitude, listing.longitude)
            if (km < 1.0) "${(km * 1000).toInt()}m away"
            else "${(km * 10).toInt() / 10.0}km away"
        }
    }
}
