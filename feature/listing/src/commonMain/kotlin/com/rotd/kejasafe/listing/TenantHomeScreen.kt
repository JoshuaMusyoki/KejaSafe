package com.rotd.kejasafe.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.kejasafe.components.KejaSafeAppBar
import com.rotd.kejasafe.components.KejaSafeAppBarContentInset
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeErrorState
import com.rotd.kejasafe.components.KejaSafeFloatingNavBar
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.components.KejaSafeOfflineBanner
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.KejaSafeTextField
import com.rotd.kejasafe.components.StudentInsightCard
import com.rotd.kejasafe.components.TenantListingsShimmer
import com.rotd.kejasafe.components.TenantPropertyCard
import com.rotd.kejasafe.rememberLocationPermissionState
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.utils.calculateDistance

@Composable
fun TenantHomeScreen(
    selectedTab: KejaSafeNavTab = KejaSafeNavTab.HOME,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProperty: (String) -> Unit,
    onNavigateToRequestProperty: () -> Unit,
    viewModel: TenantHomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listings = viewModel.filteredListings()
    val showShimmer = state.isLoading && state.listings.isEmpty()
    val showEmpty = !state.isLoading && state.listings.isEmpty() && state.error == null

    val locationPermission = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.requestDeviceLocation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh(showLoading = state.listings.isEmpty())
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
                    // Extra room under frosted app bar so the hero isn’t cramped into the overlay.
                    top = KejaSafeAppBarContentInset + 28.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    KejaSafeTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        placeholder = "Search by area, university, or style",
                        enabled = !showShimmer,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { StudentInsightCard() }

                item {
                    Text(
                        text = if (state.userLocationName != null) {
                            "Houses around ${state.userLocationName}"
                        } else {
                            "Nearby Listings"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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

                        val within30km = listings.filter { listing ->
                            state.userLatitude?.let { lat ->
                                state.userLongitude?.let { lng ->
                                    calculateDistance(
                                        lat, lng, 
                                        listing.latitude, listing.longitude
                                    ) <= 30.0
                                }
                            } ?: true
                        }

                        if (within30km.isEmpty() && state.userLatitude != null) {
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

                        val displayedListings = if (within30km.isEmpty() && state.userLatitude != null) listings else within30km

                        items(displayedListings, key = { it.id }) { listing ->
                            val distance = state.userLatitude?.let { lat ->
                                state.userLongitude?.let { lng ->
                                    calculateDistance(
                                        lat, lng, 
                                        listing.latitude, listing.longitude
                                    )
                                }
                            }
                            TenantPropertyCard(
                                listing = listing,
                                distanceKm = distance,
                                onClick = { onNavigateToProperty(listing.id) }
                            )
                        }
                        if (displayedListings.isEmpty() && state.searchQuery.isNotBlank()) {
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
                onClick = onNavigateToRequestProperty,
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
                    contentDescription = "Request a property"
                )
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
