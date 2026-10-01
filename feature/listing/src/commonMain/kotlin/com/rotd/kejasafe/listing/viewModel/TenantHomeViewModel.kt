package com.rotd.kejasafe.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.LocationProvider
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.listing.BrowseLocationStore
import com.rotd.kejasafe.listing.ListingCoordinateEnricher
import com.rotd.kejasafe.listing.matchesHouseQuery
import com.rotd.kejasafe.listing.uiState.TenantHomeState
import com.rotd.kejasafe.network.ConnectivityMonitor
import com.rotd.kejasafe.network.ListingDataBus
import com.rotd.kejasafe.utils.calculateDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TenantHomeViewModel(
    private val listingRepository: ListingRepository
) : ViewModel() {
    private val savedLocation = BrowseLocationStore.read()
    private val _state = MutableStateFlow(
        TenantHomeState(
            isLoading = true,
            showLocationPrompt = false,
            userLatitude = savedLocation?.latitude,
            userLongitude = savedLocation?.longitude,
            userLocationName = savedLocation?.name
        )
    )
    val state: StateFlow<TenantHomeState> = _state.asStateFlow()
    private var enrichJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _state
                .map { it.visibleInputs() }
                .distinctUntilChanged()
                .collect { inputs ->
                    val visible = inputs.visibleHouses()
                    _state.update { current ->
                        if (current.visibleInputs() != inputs) current
                        else current.copy(
                            visibleListings = visible.listings,
                            showingDistantSuggestions = visible.showingDistantSuggestions
                        )
                    }
                }
        }
        refresh(showLoading = true)
        resolveSavedPlace()
        viewModelScope.launch {
            ListingDataBus.listingsChanged.collect {
                refresh(showLoading = false)
            }
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                refresh(showLoading = false)
            }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _state.value.listings.isEmpty()) {
                _state.update { it.copy(isLoading = true, error = null, syncWarning = null) }
            } else {
                _state.update { it.copy(error = null, syncWarning = null) }
            }
            listingRepository.getBrowseListings(
                lat = _state.value.userLatitude,
                lng = _state.value.userLongitude,
                onCacheHit = { cached ->
                    publishListings(cached, syncWarning = null, enrich = false)
                }
            )
                .onSuccess { offline ->
                    publishListings(offline.value, offline.syncWarning, enrich = true)
                    offerLocationPromptIfNeeded()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage(),
                            syncWarning = null
                        )
                    }
                    val shown = _state.value.listings
                    if (shown.isNotEmpty() && ! _state.value.distancesReady) {
                        publishListings(shown, syncWarning = null, enrich = true)
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

    private fun resolveSavedPlace() {
        val saved = savedLocation ?: return
        val name = saved.name?.trim().orEmpty()
        if (saved.fromDevice || name.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            val coords = runCatching { LocationProvider().getCoordinatesFromAddress(name) }.getOrNull()
                ?: return@launch
            BrowseLocationStore.saveCoordinates(
                coords.latitude,
                coords.longitude,
                name,
                fromDevice = false
            )
            _state.update {
                it.copy(
                    userLatitude = coords.latitude,
                    userLongitude = coords.longitude,
                    userLocationName = name
                )
            }
        }
    }

    private fun offerLocationPromptIfNeeded() {
        if (BrowseLocationStore.hasChoice()) return
        if (_state.value.listings.isEmpty()) return
        _state.update { it.copy(showLocationPrompt = true) }
    }

    fun dismissLocationPrompt() {
        BrowseLocationStore.saveSkipped()
        _state.update { it.copy(showLocationPrompt = false) }
    }

    fun setManualLocation(lat: Double, lng: Double, name: String? = null) {
        BrowseLocationStore.saveCoordinates(lat, lng, name, fromDevice = false)
        _state.update {
            it.copy(
                userLatitude = lat,
                userLongitude = lng,
                userLocationName = name ?: it.userLocationName,
                showLocationPrompt = false
            )
        }
        refresh(showLoading = false)

        if (name == null) {
            viewModelScope.launch(Dispatchers.Default) {
                val placeName = runCatching { LocationProvider().getPlaceName(lat, lng) }.getOrNull()
                if (placeName != null) {
                    BrowseLocationStore.saveCoordinates(lat, lng, placeName, fromDevice = false)
                    _state.update { it.copy(userLocationName = placeName) }
                }
            }
        }
    }

    fun requestDeviceLocation() {
        viewModelScope.launch(Dispatchers.Default) {
            val provider = LocationProvider()
            val coords = runCatching { provider.getCurrentLocation() }.getOrNull()
            if (coords != null) {
                val name = runCatching {
                    provider.getPlaceName(coords.latitude, coords.longitude)
                }.getOrNull()
                BrowseLocationStore.saveCoordinates(
                    coords.latitude,
                    coords.longitude,
                    name,
                    fromDevice = true
                )
                _state.update {
                    it.copy(
                        userLatitude = coords.latitude,
                        userLongitude = coords.longitude,
                        userLocationName = name,
                        showLocationPrompt = false,
                        error = null
                    )
                }
                refresh(showLoading = false)
            } else {
                _state.update {
                    it.copy(error = "Could not fetch device GPS. Please pick a center manually.")
                }
            }
        }
    }

    fun onSearchChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}

private data class VisibleInputs(
    val listings: List<PropertyListing>,
    val searchQuery: String,
    val userLatitude: Double?,
    val userLongitude: Double?
)

private data class VisibleHouses(
    val listings: List<PropertyListing>,
    val showingDistantSuggestions: Boolean
)

private fun TenantHomeState.visibleInputs() = VisibleInputs(
    listings = listings,
    searchQuery = searchQuery,
    userLatitude = userLatitude,
    userLongitude = userLongitude
)

private fun VisibleInputs.visibleHouses(): VisibleHouses {
    val matched = if (searchQuery.isBlank()) {
        listings
    } else {
        listings.filter { it.matchesHouseQuery(searchQuery) }
    }
    val lat = userLatitude
    val lng = userLongitude
    if (lat == null || lng == null) {
        return VisibleHouses(matched, showingDistantSuggestions = false)
    }
    val nearby = matched.filter { listing ->
        calculateDistance(lat, lng, listing.latitude, listing.longitude) <= 30.0
    }
    return if (nearby.isEmpty() && matched.isNotEmpty()) {
        VisibleHouses(matched, showingDistantSuggestions = true)
    } else {
        VisibleHouses(nearby, showingDistantSuggestions = false)
    }
}
