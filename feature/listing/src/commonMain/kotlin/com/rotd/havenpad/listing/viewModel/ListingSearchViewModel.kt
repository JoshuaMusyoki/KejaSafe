package com.rotd.havenpad.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.ListingStatus
import com.rotd.havenpad.LocationProvider
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.data.ListingRepository
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.listing.BrowseLocationStore
import com.rotd.havenpad.listing.ListingCoordinateEnricher
import com.rotd.havenpad.listing.matchesHouseQuery
import com.rotd.havenpad.listing.uiState.DistanceFilter
import com.rotd.havenpad.listing.uiState.ListingSearchUiState
import com.rotd.havenpad.listing.uiState.ListingSortOption
import com.rotd.havenpad.listing.uiState.PriceRangeFilter
import com.rotd.havenpad.network.ConnectivityMonitor
import com.rotd.havenpad.network.ListingDataBus
import com.rotd.havenpad.utils.calculateDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListingSearchViewModel(
    private val listingRepository: ListingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ListingSearchUiState())
    val state: StateFlow<ListingSearchUiState> = _state.asStateFlow()
    private var appliedLaunchKey: String? = null
    private var enrichJob: Job? = null
    private var refreshGeneration = 0

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _state
                .map { it.filterInputs() }
                .distinctUntilChanged()
                .collect { inputs ->
                    val results = inputs.filtered()
                    val universities = inputs.listings
                        .mapNotNull { it.university?.trim()?.takeIf(String::isNotEmpty) }
                        .distinct()
                        .sorted()
                    _state.update { current ->
                        if (current.filterInputs() != inputs) current
                        else current.copy(results = results, universities = universities)
                    }
                }
        }
        viewModelScope.launch {
            ListingDataBus.listingsChanged.collect { refresh(showLoading = false) }
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect { refresh(showLoading = false) }
        }
    }

    fun seed(query: String, latitude: Double?, longitude: Double?) {
        val key = "$query|$latitude|$longitude"
        if (appliedLaunchKey == key) return
        val firstOpen = appliedLaunchKey == null
        appliedLaunchKey = key
        val saved = if (latitude == null || longitude == null) BrowseLocationStore.read() else null
        _state.update {
            it.copy(
                query = query,
                userLatitude = latitude ?: saved?.latitude ?: it.userLatitude,
                userLongitude = longitude ?: saved?.longitude ?: it.userLongitude
            )
        }
        if (firstOpen) refresh(showLoading = true)
    }

    fun refresh(showLoading: Boolean = true) {
        val generation = ++refreshGeneration
        viewModelScope.launch {
            val firstLoad = showLoading && _state.value.listings.isEmpty()
            _state.update {
                it.copy(
                    isLoading = firstLoad,
                    isRefreshing = !firstLoad,
                    error = null,
                    syncWarning = null
                )
            }
            listingRepository.getBrowseListings(
                lat = _state.value.userLatitude,
                lng = _state.value.userLongitude,
                onCacheHit = { cached ->
                    if (generation == refreshGeneration && _state.value.listings.isEmpty()) {
                        publishListings(cached, syncWarning = null, enrich = false)
                    }
                }
            )
                .onSuccess { offline ->
                    if (generation != refreshGeneration) return@onSuccess
                    publishListings(offline.value, offline.syncWarning, enrich = true)
                }
                .onFailure { e ->
                    if (generation != refreshGeneration) return@onFailure
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage(),
                            syncWarning = null
                        )
                    }
                    val shown = _state.value.listings
                    if (shown.isNotEmpty() && !_state.value.distancesReady) {
                        publishListings(shown, syncWarning = null, enrich = true)
                    }
                }
            if (generation == refreshGeneration) {
                _state.update { it.copy(isRefreshing = false, isLoading = false) }
            }
        }
    }

    fun requestDeviceLocation(quiet: Boolean = false) {
        viewModelScope.launch(Dispatchers.Default) {
            val coords = runCatching { LocationProvider().getCurrentLocation() }.getOrNull()
            if (coords != null) {
                BrowseLocationStore.saveCoordinates(
                    coords.latitude,
                    coords.longitude,
                    name = null,
                    fromDevice = true
                )
                _state.update {
                    it.copy(
                        userLatitude = coords.latitude,
                        userLongitude = coords.longitude,
                        locationMessage = null
                    )
                }
            } else if (!quiet) {
                _state.update {
                    it.copy(locationMessage = "Couldn't read your location. Distance filters need it.")
                }
            }
        }
    }

    private fun publishListings(
        listings: List<PropertyListing>,
        syncWarning: String?,
        enrich: Boolean
    ) {
        _state.update {
            it.copy(
                listings = listings,
                isLoading = false,
                error = null,
                syncWarning = syncWarning,
                distancesReady = listings.none { listing -> listing.address.isNotBlank() }
            )
        }
        if (!enrich) return
        if (listings.isEmpty()) {
            _state.update { it.copy(distancesReady = true) }
            return
        }
        enrichJob?.cancel()
        enrichJob = viewModelScope.launch(Dispatchers.Default) {
            val enriched = ListingCoordinateEnricher.enrich(listings)
            _state.update { current ->
                if (current.listings != listings) current
                else current.copy(listings = enriched, distancesReady = true)
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun setPriceRange(range: PriceRangeFilter) {
        _state.update { it.copy(priceRange = range, minPriceInput = "", maxPriceInput = "") }
    }

    fun setMinPriceInput(value: String) {
        _state.update { it.copy(minPriceInput = value.filter(Char::isDigit)) }
    }

    fun setMaxPriceInput(value: String) {
        _state.update { it.copy(maxPriceInput = value.filter(Char::isDigit)) }
    }

    fun setDistance(distance: DistanceFilter) {
        _state.update { it.copy(distance = distance) }
    }

    fun setProximity(proximity: CampusProximity?) {
        _state.update { it.copy(proximity = proximity) }
    }

    fun setPropertyType(type: PropertyType?) {
        _state.update { it.copy(propertyType = type) }
    }

    fun setUniversity(university: String?) {
        _state.update { it.copy(university = university) }
    }

    fun setVerifiedOnly(verified: Boolean) {
        _state.update { it.copy(verifiedOnly = verified) }
    }

    fun setSort(sort: ListingSortOption) {
        _state.update { it.copy(sort = sort) }
    }

    fun setFiltersVisible(visible: Boolean) {
        _state.update { it.copy(showFilters = visible) }
    }

    fun clearFilters() {
        _state.update {
            it.copy(
                priceRange = PriceRangeFilter.Any,
                minPriceInput = "",
                maxPriceInput = "",
                distance = DistanceFilter.Any,
                proximity = null,
                propertyType = null,
                university = null,
                verifiedOnly = false,
                sort = ListingSortOption.RELEVANCE
            )
        }
    }
}

private data class SearchFilterInputs(
    val listings: List<PropertyListing>,
    val query: String,
    val minPrice: Double?,
    val maxPrice: Double?,
    val distance: DistanceFilter,
    val proximity: CampusProximity?,
    val propertyType: PropertyType?,
    val university: String?,
    val verifiedOnly: Boolean,
    val sort: ListingSortOption,
    val userLatitude: Double?,
    val userLongitude: Double?
)

private fun ListingSearchUiState.filterInputs() = SearchFilterInputs(
    listings = listings,
    query = query,
    minPrice = effectiveMinPrice(),
    maxPrice = effectiveMaxPrice(),
    distance = distance,
    proximity = proximity,
    propertyType = propertyType,
    university = university,
    verifiedOnly = verifiedOnly,
    sort = sort,
    userLatitude = userLatitude,
    userLongitude = userLongitude
)

private fun SearchFilterInputs.filtered(): List<PropertyListing> {
    val filtered = listings.filter { listing ->
        val matchesQuery = listing.matchesHouseQuery(query)
        val price = listing.pricePerMonth
        val matchesPrice = when {
            minPrice == null && maxPrice == null -> true
            price == null -> false
            minPrice != null && price < minPrice -> false
            maxPrice != null && price > maxPrice -> false
            else -> true
        }
        val matchesDistance = matchesDistance(listing)
        val matchesProximity = proximity == null || listing.proximity == proximity
        val matchesType = propertyType == null || listing.propertyType == propertyType
        val matchesUniversity = university.isNullOrBlank() ||
            listing.university.equals(university, ignoreCase = true)
        val matchesVerified = !verifiedOnly || listing.status == ListingStatus.VERIFIED
        matchesQuery && matchesPrice && matchesDistance &&
            matchesProximity && matchesType && matchesUniversity && matchesVerified
    }
    return when (sort) {
        ListingSortOption.RELEVANCE -> filtered
        ListingSortOption.PRICE_LOW -> filtered.sortedBy { it.pricePerMonth ?: Double.MAX_VALUE }
        ListingSortOption.PRICE_HIGH -> filtered.sortedByDescending { it.pricePerMonth ?: Double.MIN_VALUE }
        ListingSortOption.DISTANCE -> {
            val lat = userLatitude
            val lng = userLongitude
            if (lat == null || lng == null) filtered
            else filtered.sortedBy { calculateDistance(lat, lng, it.latitude, it.longitude) }
        }
    }
}

private fun SearchFilterInputs.matchesDistance(listing: PropertyListing): Boolean {
    val maxKm = distance.maxKm ?: return true
    val lat = userLatitude ?: return true
    val lng = userLongitude ?: return true
    return calculateDistance(lat, lng, listing.latitude, listing.longitude) <= maxKm
}
