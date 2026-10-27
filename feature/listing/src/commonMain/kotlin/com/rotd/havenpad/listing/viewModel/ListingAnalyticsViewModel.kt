package com.rotd.havenpad.listing.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.listing.uiState.ListingAnalyticsUiState
import com.rotd.havenpad.network.ListingClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListingAnalyticsViewModel(
    private val listingId: String
) : ViewModel() {
    private val _state = MutableStateFlow(ListingAnalyticsUiState())
    val state: StateFlow<ListingAnalyticsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            ListingClient.getListingAnalytics(listingId)
                .onSuccess { analytics ->
                    _state.update {
                        it.copy(isLoading = false, analytics = analytics, error = null)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.userMessage()
                        )
                    }
                }
        }
    }
}