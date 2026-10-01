package com.rotd.kejasafe.listing

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.kejasafe.CampusProximity
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.components.CompactListingCard
import com.rotd.kejasafe.components.KejaSafeBottomSheet
import com.rotd.kejasafe.components.KejaSafeErrorState
import com.rotd.kejasafe.components.KejaSafeOfflineBanner
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.KejaSafeTextField
import com.rotd.kejasafe.components.TenantListingsShimmer
import com.rotd.kejasafe.listing.uiState.DistanceFilter
import com.rotd.kejasafe.listing.uiState.ListingSortOption
import com.rotd.kejasafe.listing.uiState.PriceRangeFilter
import com.rotd.kejasafe.listing.viewModel.ListingSearchViewModel
import com.rotd.kejasafe.rememberLocationPermissionState
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.utils.calculateDistance
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListingSearchScreen(
    onBack: () -> Unit,
    onNavigateToProperty: (String) -> Unit,
    initialQuery: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    viewModel: ListingSearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val results = state.results
    val universities = state.universities
    val pullRefreshState = rememberPullToRefreshState()
    val locationPermission = rememberLocationPermissionState { granted ->
        if (granted) viewModel.requestDeviceLocation()
    }
    val useLocation = {
        if (locationPermission.isGranted) viewModel.requestDeviceLocation()
        else locationPermission.launchPermissionRequest()
    }

    LaunchedEffect(initialQuery, initialLatitude, initialLongitude) {
        viewModel.seed(initialQuery, initialLatitude, initialLongitude)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search houses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setFiltersVisible(true) }) {
                        BadgedBox(
                            badge = {
                                if (state.activeFilterCount > 0) {
                                    Badge { Text(state.activeFilterCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KejaSafeTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search area, university, or house type",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (state.query.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                QuickFilterChips(
                    priceRange = state.priceRange,
                    distance = state.distance,
                    verifiedOnly = state.verifiedOnly,
                    onPrice = viewModel::setPriceRange,
                    onDistance = viewModel::setDistance,
                    onVerified = viewModel::setVerifiedOnly,
                    onOpenAllFilters = { viewModel.setFiltersVisible(true) }
                )

                Text(
                    text = when {
                        state.isLoading && results.isEmpty() -> "Loading houses…"
                        results.isEmpty() -> "No houses match your search"
                        else -> "${results.size} house${if (results.size == 1) "" else "s"}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                state.error != null && state.listings.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        KejaSafeErrorState(
                            message = state.error.orEmpty(),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
                state.isLoading && state.listings.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
                    ) {
                        TenantListingsShimmer(count = 4)
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh(showLoading = false) },
                        state = pullRefreshState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    if (state.syncWarning != null) {
                        KejaSafeOfflineBanner(
                            message = state.syncWarning.orEmpty(),
                            onRetry = { viewModel.refresh(showLoading = false) },
                            modifier = Modifier.padding(
                                horizontal = KejaSafeTheme.spacing.containerMarginMobile,
                                vertical = 4.dp
                            )
                        )
                    }
                    if (state.distance != DistanceFilter.Any && state.userLatitude == null) {
                        LocationHint(
                            message = state.locationMessage
                                ?: "Turn on location to filter by how close a house is.",
                            onUseLocation = useLocation
                        )
                    } else if (state.locationMessage != null) {
                        LocationHint(
                            message = state.locationMessage.orEmpty(),
                            onUseLocation = useLocation
                        )
                    }

                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Try a different area, price, or distance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 240.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = KejaSafeTheme.spacing.containerMarginMobile,
                                end = KejaSafeTheme.spacing.containerMarginMobile,
                                bottom = 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results, key = { it.id }) { listing ->
                                CompactListingCard(
                                    listing = listing,
                                    distanceLabel = resultMeta(
                                        listing,
                                        state.userLatitude,
                                        state.userLongitude,
                                        state.distancesReady
                                    ),
                                    onView = { onNavigateToProperty(listing.id) },
                                    fullWidth = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    }
                    }
                }
            }
        }
    }

    if (state.showFilters) {
        SearchFiltersSheet(
            priceRange = state.priceRange,
            minPriceInput = state.minPriceInput,
            maxPriceInput = state.maxPriceInput,
            distance = state.distance,
            proximity = state.proximity,
            propertyType = state.propertyType,
            university = state.university,
            universities = universities,
            verifiedOnly = state.verifiedOnly,
            sort = state.sort,
            hasLocation = state.userLatitude != null && state.userLongitude != null,
            onPrice = viewModel::setPriceRange,
            onMinPrice = viewModel::setMinPriceInput,
            onMaxPrice = viewModel::setMaxPriceInput,
            onDistance = viewModel::setDistance,
            onProximity = viewModel::setProximity,
            onPropertyType = viewModel::setPropertyType,
            onUniversity = viewModel::setUniversity,
            onVerified = viewModel::setVerifiedOnly,
            onSort = viewModel::setSort,
            onUseLocation = useLocation,
            onClear = viewModel::clearFilters,
            onDismiss = { viewModel.setFiltersVisible(false) }
        )
    }
}

@Composable
private fun LocationHint(
    message: String,
    onUseLocation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onUseLocation) {
            Text("Use my location", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickFilterChips(
    priceRange: PriceRangeFilter,
    distance: DistanceFilter,
    verifiedOnly: Boolean,
    onPrice: (PriceRangeFilter) -> Unit,
    onDistance: (DistanceFilter) -> Unit,
    onVerified: (Boolean) -> Unit,
    onOpenAllFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onOpenAllFilters,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Filters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PriceRangeFilter.presets.filter { it != PriceRangeFilter.Any }.forEach { range ->
            FilterChip(
                selected = priceRange == range,
                onClick = { onPrice(if (priceRange == range) PriceRangeFilter.Any else range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        DistanceFilter.presets.filter { it != DistanceFilter.Any }.forEach { range ->
            FilterChip(
                selected = distance == range,
                onClick = { onDistance(if (distance == range) DistanceFilter.Any else range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        FilterChip(
            selected = verifiedOnly,
            onClick = { onVerified(!verifiedOnly) },
            label = { Text("Verified") }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SearchFiltersSheet(
    priceRange: PriceRangeFilter,
    minPriceInput: String,
    maxPriceInput: String,
    distance: DistanceFilter,
    proximity: CampusProximity?,
    propertyType: PropertyType?,
    university: String?,
    universities: List<String>,
    verifiedOnly: Boolean,
    sort: ListingSortOption,
    hasLocation: Boolean,
    onPrice: (PriceRangeFilter) -> Unit,
    onMinPrice: (String) -> Unit,
    onMaxPrice: (String) -> Unit,
    onDistance: (DistanceFilter) -> Unit,
    onProximity: (CampusProximity?) -> Unit,
    onPropertyType: (PropertyType?) -> Unit,
    onUniversity: (String?) -> Unit,
    onVerified: (Boolean) -> Unit,
    onSort: (ListingSortOption) -> Unit,
    onUseLocation: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    KejaSafeBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Narrow houses by budget, distance, campus, and type.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            FilterSectionTitle("Price per month")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceRangeFilter.presets.forEach { range ->
                    FilterChip(
                        selected = priceRange == range && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                        onClick = { onPrice(range) },
                        label = { Text(range.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KejaSafeTextField(
                    value = minPriceInput,
                    onValueChange = onMinPrice,
                    placeholder = "Min KES",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                KejaSafeTextField(
                    value = maxPriceInput,
                    onValueChange = onMaxPrice,
                    placeholder = "Max KES",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterSectionTitle("Distance from you")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistanceFilter.presets.forEach { range ->
                    FilterChip(
                        selected = distance == range,
                        onClick = { onDistance(range) },
                        label = { Text(range.label) }
                    )
                }
            }
            if (!hasLocation) {
                TextButton(onClick = onUseLocation) {
                    Text("Use my location")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterSectionTitle("Campus proximity")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = proximity == null,
                    onClick = { onProximity(null) },
                    label = { Text("Any") }
                )
                CampusProximity.entries.forEach { value ->
                    FilterChip(
                        selected = proximity == value,
                        onClick = { onProximity(value) },
                        label = { Text(value.displayLabel()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterSectionTitle("House type")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = propertyType == null,
                    onClick = { onPropertyType(null) },
                    label = { Text("Any") }
                )
                PropertyType.entries.forEach { type ->
                    FilterChip(
                        selected = propertyType == type,
                        onClick = { onPropertyType(type) },
                        label = { Text(type.displayLabel()) }
                    )
                }
            }

            if (universities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FilterSectionTitle("University")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = university == null,
                        onClick = { onUniversity(null) },
                        label = { Text("Any") }
                    )
                    universities.forEach { name ->
                        FilterChip(
                            selected = university.equals(name, ignoreCase = true),
                            onClick = {
                                onUniversity(if (university.equals(name, ignoreCase = true)) null else name)
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterSectionTitle("Listing")
            FilterChip(
                selected = verifiedOnly,
                onClick = { onVerified(!verifiedOnly) },
                label = { Text("Verified only") }
            )

            Spacer(modifier = Modifier.height(16.dp))
            FilterSectionTitle("Sort by")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("Relevance", ListingSortOption.RELEVANCE, sort, onSort)
                SortChip("Price ↑", ListingSortOption.PRICE_LOW, sort, onSort)
                SortChip("Price ↓", ListingSortOption.PRICE_HIGH, sort, onSort)
                SortChip("Nearest", ListingSortOption.DISTANCE, sort, onSort)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear all")
                }
                KejaSafePrimaryButton(
                    text = "Show results",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SortChip(
    label: String,
    option: ListingSortOption,
    selected: ListingSortOption,
    onSelect: (ListingSortOption) -> Unit
) {
    FilterChip(
        selected = selected == option,
        onClick = { onSelect(option) },
        label = { Text(label) }
    )
}

private fun resultMeta(
    listing: PropertyListing,
    userLatitude: Double?,
    userLongitude: Double?,
    distancesReady: Boolean
): String? {
    val distance = if (!distancesReady && listing.address.isNotBlank()) {
        null
    } else {
        userLatitude?.let { lat ->
            userLongitude?.let { lng ->
                val km = calculateDistance(lat, lng, listing.latitude, listing.longitude)
                if (km < 1.0) "${(km * 1000).toInt()}m away"
                else "${(km * 10).toInt() / 10.0}km away"
            }
        }
    }
    val area = listing.address.takeIf { it.isNotBlank() }
    return listOfNotNull(area, distance).joinToString(" · ").ifBlank { null }
}
