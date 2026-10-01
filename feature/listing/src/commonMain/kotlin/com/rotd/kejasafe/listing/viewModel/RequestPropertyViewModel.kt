package com.rotd.kejasafe.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.LocationProvider
import com.rotd.kejasafe.data.PropertyRequestRepository
import com.rotd.kejasafe.domain.listing.DistancePreference
import com.rotd.kejasafe.domain.listing.HouseSize
import com.rotd.kejasafe.domain.listing.HouseType
import com.rotd.kejasafe.domain.listing.ListingType
import com.rotd.kejasafe.domain.listing.PropertyRequest
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.listing.uiState.RequestPropertyUiState
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestPropertyViewModel(
    private val repository: PropertyRequestRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RequestPropertyUiState())
    val state: StateFlow<RequestPropertyUiState> = _state.asStateFlow()

    init {
        // Pre-fill phone if available in session
        SessionManager.currentUser.value?.phone?.let { phone ->
            _state.update { it.copy(phoneNumber = phone) }
        }
    }

    fun updateSize(size: HouseSize) = _state.update { it.copy(size = size) }
    fun updateHouseType(type: HouseType?) = _state.update { it.copy(houseType = type) }
    fun updateListingType(type: ListingType) = _state.update { it.copy(listingType = type) }

    fun updateArea(area: String) {
        _state.update { it.copy(area = area) }
        updateEstateSuggestions(area)
    }

    private fun updateEstateSuggestions(area: String) {
        val q = area.trim()
        if (q.length < 3) {
            _state.update { it.copy(estateSuggestions = emptyList()) }
            return
        }

        viewModelScope.launch {
            val suggestions = runCatching { LocationProvider().searchPlaces(q) }.getOrNull() ?: emptyList()
            _state.update { it.copy(estateSuggestions = suggestions) }
        }
    }

    fun selectEstate(estate: String) {
        _state.update { it.copy(area = estate, estateSuggestions = emptyList()) }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            val coords = try { LocationProvider().getCurrentLocation() } catch (e: Exception) { null }
            if (coords != null) {
                val name = runCatching { LocationProvider().getPlaceName(coords.latitude, coords.longitude) }.getOrNull()
                if (name != null) {
                    updateArea(name)
                }
            }
        }
    }

    fun updateDistance(dist: DistancePreference) = _state.update { it.copy(distance = dist) }
    fun updateBudgetFrom(from: String) = _state.update { it.copy(budgetFrom = from.filter { it.isDigit() }) }
    fun updateBudgetTo(to: String) = _state.update { it.copy(budgetTo = to.filter { it.isDigit() }) }
    fun updateAdditionalInfo(info: String) = _state.update { it.copy(additionalInfo = info) }
    fun updateAgentsCanCall(canCall: Boolean) = _state.update { it.copy(agentsCanCall = canCall) }
    fun updatePhoneNumber(phone: String) = _state.update { it.copy(phoneNumber = phone) }

    fun submit() {
        val s = _state.value
        if (s.area.isBlank()) {
            _state.update { it.copy(error = "Please specify the area") }
            return
        }
        if (s.phoneNumber.isBlank()) {
            _state.update { it.copy(error = "M-Pesa phone number is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val request = PropertyRequest(
                userId = SessionManager.currentUser.value?.id ?: "",
                size = s.size,
                houseType = s.houseType,
                listingType = s.listingType,
                area = s.area,
                distance = s.distance,
                budgetFrom = s.budgetFrom.toLongOrNull() ?: 0,
                budgetTo = s.budgetTo.toLongOrNull() ?: 0,
                additionalInfo = s.additionalInfo.takeIf { it.isNotBlank() },
                agentsCanCall = s.agentsCanCall
            )

            repository.submitRequest(s.phoneNumber, request)
                .onSuccess { requestId ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            completed = true,
                            successMessage = "Request placed! Check your phone for M-Pesa STK push."
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.userMessage()) }
                }
        }
    }
}