package com.rotd.kejasafe.listing

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.components.CenteredMarketplaceTileRow
import com.rotd.kejasafe.components.CompactListingCard
import com.rotd.kejasafe.components.InsightCard
import com.rotd.kejasafe.components.KejaSafeAppBar
import com.rotd.kejasafe.components.KejaSafeAppBarContentInset
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeDetailPopup
import com.rotd.kejasafe.components.KejaSafeErrorState
import com.rotd.kejasafe.components.KejaSafeFloatingNavBar
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.components.KejaSafeOfflineBanner
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.ListingPhoto
import com.rotd.kejasafe.components.ListingStatusBadge
import com.rotd.kejasafe.components.MarketplaceSectionHeader
import com.rotd.kejasafe.components.MarketplaceTileCard
import com.rotd.kejasafe.components.MarketplaceTileTone
import com.rotd.kejasafe.components.TenantListingsShimmer
import com.rotd.kejasafe.listing.viewModel.TenantHomeViewModel
import com.rotd.kejasafe.rememberLocationPermissionState
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.utils.calculateDistance
import org.koin.compose.viewmodel.koinViewModel

private enum class TenantHomePopup {
    INSIGHT,
    REQUEST_PROPERTY,
    LISTING
}

@Composable
fun TenantHomeScreen(
    selectedTab: KejaSafeNavTab = KejaSafeNavTab.HOME,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProperty: (String) -> Unit,
    onNavigateToSearch: (query: String, latitude: Double?, longitude: Double?) -> Unit,
    onNavigateToRequestProperty: () -> Unit,
    viewModel: TenantHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showShimmer = state.isLoading && state.listings.isEmpty()
    val showEmpty = !state.isLoading && state.listings.isEmpty() && state.error == null
    var activePopup by rememberSaveable { mutableStateOf<TenantHomePopup?>(null) }
    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }

    val openSearch = {
        onNavigateToSearch(state.searchQuery, state.userLatitude, state.userLongitude)
    }
    val locationPermission = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.requestDeviceLocation()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            KejaSafeFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = KejaSafeTheme.spacing.containerMarginMobile,
                    end = KejaSafeTheme.spacing.containerMarginMobile,
                    top = KejaSafeAppBarContentInset + 28.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Find your next keja",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        KejaSafeBody(
                            text = if (showShimmer) {
                                "Loading places near campus…"
                            } else {
                                "Verified student housing near campus."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

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
                    CenteredMarketplaceTileRow {
                        MarketplaceTileCard(
                            title = "Custom request",
                            value = "New",
                            subtitle = "Tell us what you need near campus",
                            actionLabel = "Request",
                            onAction = { activePopup = TenantHomePopup.REQUEST_PROPERTY },
                            tone = MarketplaceTileTone.PRIMARY,
                            icon = Icons.Default.Search
                        )
                        MarketplaceTileCard(
                            title = "Student tip",
                            value = "4×",
                            subtitle = "More inquiries within 10 min walk",
                            actionLabel = "Read",
                            onAction = { activePopup = TenantHomePopup.INSIGHT },
                            tone = MarketplaceTileTone.SECONDARY,
                            icon = Icons.Default.Lightbulb
                        )
                    }
                }

                item {
                    val categories = listOf("All", "Single Room", "Bedsitter", "1 Bedroom", "2 Bedroom")
                    var selectedCategory by remember { mutableStateOf("All") }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            val isSelected = selectedCategory == cat
                            Surface(
                                onClick = {
                                    selectedCategory = cat
                                    if (cat == "All") viewModel.onSearchChange("")
                                    else viewModel.onSearchChange(cat)
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = openSearch,
                        enabled = !showShimmer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.searchQuery.ifBlank {
                                    "Search by area, university, or style"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (state.searchQuery.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    MarketplaceSectionHeader(
                        title = if (state.userLocationName != null) {
                            "Houses around ${state.userLocationName}"
                        } else {
                            "Trending near you"
                        }
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
                        if (state.error != null) {
                            item {
                                Text(
                                    text = state.error.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

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

                        items(
                            items = state.visibleListings,
                            key = { it.id }
                        ) { listing ->
                            CompactListingCard(
                                listing = listing,
                                distanceLabel = distanceLabel(
                                    listing,
                                    state.userLatitude,
                                    state.userLongitude,
                                    state.distancesReady
                                ),
                                onView = {
                                    selectedListingId = listing.id
                                    activePopup = TenantHomePopup.LISTING
                                },
                                fullWidth = true,
                                modifier = Modifier.fillMaxWidth()
                            )
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

            KejaSafeAppBar(
                onTuneClick = {},
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

            when (activePopup) {
                TenantHomePopup.INSIGHT -> {
                    KejaSafeDetailPopup(
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
                    KejaSafeDetailPopup(
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
                            KejaSafePrimaryButton(
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
                TenantHomePopup.LISTING -> selectedListing?.let { listing ->
                    KejaSafeDetailPopup(
                        title = listing.title,
                        onDismiss = {
                            activePopup = null
                            selectedListingId = null
                        },
                        useDialog = useDialog,
                        content = {
                            TenantListingPopupContent(
                                listing = listing,
                                userLatitude = state.userLatitude,
                                userLongitude = state.userLongitude
                            )
                        },
                        footer = {
                            KejaSafePrimaryButton(
                                text = "Inspect listing",
                                onClick = {
                                    activePopup = null
                                    selectedListingId = null
                                    onNavigateToProperty(listing.id)
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

@Composable
private fun TenantListingPopupContent(
    listing: PropertyListing,
    userLatitude: Double?,
    userLongitude: Double?
) {
    ListingPhoto(
        imageUrl = listing.imageUrl,
        contentDescription = listing.title,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (listing.status == com.rotd.kejasafe.ListingStatus.VERIFIED) {
            ListingStatusBadge(
                status = listing.status,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    listing.pricePerMonth?.let { price ->
        Text(
            text = "${listing.currency} ${price.toInt()}/mo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = listing.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    val distance = distanceLabel(listing, userLatitude, userLongitude, distancesReady = true)
    val meta = listOfNotNull(listing.address, distance).joinToString(" · ")
    Text(
        text = meta,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
