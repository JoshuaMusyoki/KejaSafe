package com.rotd.havenpad.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.LiveTourSession
import com.rotd.havenpad.LiveTourStatus
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTourCallUiState(
    val isLoading: Boolean = true,
    val isJoining: Boolean = false,
    val session: LiveTourSession? = null,
    val listingTitle: String = "",
    val mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO,
    val error: String? = null,
    val ended: Boolean = false,
    val viewerName: String = "",
    val viewerId: String = "",
)

class LiveTourCallViewModel(
    private val sessionId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveTourCallUiState())
    val state: StateFlow<LiveTourCallUiState> = _state.asStateFlow()

    init {
        val user = SessionManager.currentUser.value
        _state.update {
            it.copy(
                viewerName = user?.fullName.orEmpty(),
                viewerId = user?.id.orEmpty(),
            )
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            LiveTourClient.getTour(sessionId)
                .onSuccess { session ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            listingTitle = session.listingTitle,
                            mediaType = session.mediaType,
                            ended = session.status == LiveTourStatus.ENDED,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    fun join(session: LiveTourMediaSession) {
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, error = null) }
            LiveTourClient.fetchToken(sessionId)
                .onSuccess { details ->
                    val video = details.mediaType == LiveTourMediaType.VIDEO
                    session.connect(
                        serverUrl = details.serverUrl,
                        token = details.participantToken,
                        enableCamera = video,
                        enableMic = true,
                    )
                    _state.update {
                        it.copy(
                            isJoining = false,
                            listingTitle = details.listingTitle.ifBlank { it.listingTitle },
                            mediaType = details.mediaType,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isJoining = false, error = e.userMessage())
                    }
                }
        }
    }

    fun endCall(session: LiveTourMediaSession) {
        viewModelScope.launch {
            session.disconnect()
            LiveTourClient.endTour(sessionId)
            _state.update { it.copy(ended = true) }
        }
    }
}
