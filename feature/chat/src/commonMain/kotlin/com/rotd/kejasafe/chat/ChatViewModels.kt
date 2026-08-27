package com.rotd.kejasafe.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.Connection
import com.rotd.kejasafe.domain.chat.ConnectionStatus
import com.rotd.kejasafe.domain.chat.connectionIdFor
import com.rotd.kejasafe.domain.repository.ApartmentChatRepository
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatInboxTab { MESSAGES, PEOPLE }

enum class PeerConnectAction {
    NONE,
    CONNECT,
    PENDING_OUTGOING,
    ACCEPT,
    MESSAGE,
}

data class InboxPeerRow(
    val peer: ApartmentChatPeer,
    val lastMessage: ApartmentChatMessage? = null,
    val connection: Connection? = null,
) {
    fun actionFor(myUserId: String?): PeerConnectAction {
        if (myUserId.isNullOrBlank()) return PeerConnectAction.NONE
        return when (connection?.status) {
            ConnectionStatus.ACCEPTED -> PeerConnectAction.MESSAGE
            ConnectionStatus.PENDING -> {
                if (connection.receiverId == myUserId) PeerConnectAction.ACCEPT
                else PeerConnectAction.PENDING_OUTGOING
            }
            ConnectionStatus.REJECTED, null -> PeerConnectAction.CONNECT
        }
    }
}

data class ChatInboxUiState(
    val isLoading: Boolean = true,
    val selectedTab: ChatInboxTab = ChatInboxTab.MESSAGES,
    val searchQuery: String = "",
    /** Connected peers with optional last message (Messages tab). */
    val messageRows: List<InboxPeerRow> = emptyList(),
    /** All booking-eligible peers (People tab), filtered by [searchQuery]. */
    val peopleRows: List<InboxPeerRow> = emptyList(),
    val profilePeer: ApartmentChatPeer? = null,
    val myUserId: String? = null,
    val error: String? = null,
    val emptyMessagesHint: String =
        "Connected chats appear here. Your landlord is ready to message; connect with other tenants from People.",
    val emptyPeopleHint: String =
        "Confirmed stays appear here. Your landlord is connected automatically — other tenants need a connect request.",
)

data class ChatThreadUiState(
    val peer: ApartmentChatPeer? = null,
    val myUserId: String? = null,
    val messages: List<ApartmentChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val selectedMessage: ApartmentChatMessage? = null,
)

class ChatInboxViewModel(
    private val chatRepository: ApartmentChatRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatInboxUiState())
    val state: StateFlow<ChatInboxUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            SessionManager.currentUser
                .map { it?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    _state.update { it.copy(myUserId = userId) }
                    if (userId == null) {
                        chatRepository.clearPeers()
                        _state.value = ChatInboxUiState(isLoading = false, myUserId = null)
                    } else {
                        refresh()
                    }
                }
        }
        viewModelScope.launch {
            combine(
                chatRepository.observeEligiblePeers(),
                chatRepository.observeMessageEpoch(),
                chatRepository.observeConnections(),
                searchQuery,
            ) { peers, _, connections, query ->
                rebuildRows(peers, connections, query)
            }.collect { (messageRows, peopleRows) ->
                _state.update {
                    it.copy(
                        messageRows = messageRows,
                        peopleRows = peopleRows,
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    fun selectTab(tab: ChatInboxTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun openProfile(peer: ApartmentChatPeer) {
        _state.update { it.copy(profilePeer = peer) }
    }

    fun dismissProfile() {
        _state.update { it.copy(profilePeer = null) }
    }

    fun connect(peerUserId: String) {
        viewModelScope.launch {
            chatRepository.sendConnectionRequest(peerUserId)
                .onFailure { e ->
                    _state.update { it.copy(error = e.userMessage()) }
                }
        }
    }

    fun accept(peerUserId: String) {
        viewModelScope.launch {
            chatRepository.acceptConnectionRequest(peerUserId)
                .onFailure { e ->
                    _state.update { it.copy(error = e.userMessage()) }
                }
        }
    }

    fun reject(peerUserId: String) {
        viewModelScope.launch {
            chatRepository.rejectConnectionRequest(peerUserId)
                .onFailure { e ->
                    _state.update { it.copy(error = e.userMessage()) }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            chatRepository.refreshEligiblePeers()
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    private suspend fun rebuildRows(
        peers: List<ApartmentChatPeer>,
        connections: List<Connection>,
        query: String,
    ): Pair<List<InboxPeerRow>, List<InboxPeerRow>> {
        val me = SessionManager.currentUser.value?.id.orEmpty()
        val allRows = peers.map { peer ->
            val connection = connections.firstOrNull { conn ->
                conn.id == connectionIdFor(me, peer.userId) ||
                    (conn.senderId == me && conn.receiverId == peer.userId) ||
                    (conn.receiverId == me && conn.senderId == peer.userId)
            }
            InboxPeerRow(
                peer = peer,
                lastMessage = chatRepository.latestMessage(peer.listingId, peer.userId),
                connection = connection,
            )
        }
        val needle = query.trim().lowercase()
        val people = if (needle.isEmpty()) {
            allRows
        } else {
            allRows.filter { row ->
                row.peer.displayName.lowercase().contains(needle) ||
                    row.peer.listingTitle.lowercase().contains(needle)
            }
        }.sortedBy { it.peer.displayName.lowercase() }

        val messages = allRows
            .filter { it.connection?.status == ConnectionStatus.ACCEPTED }
            .sortedByDescending { it.lastMessage?.timestamp ?: it.connection?.timestamp ?: 0L }

        return messages to people
    }
}

class ChatThreadViewModel(
    private val chatRepository: ApartmentChatRepository,
    private val peer: ApartmentChatPeer,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ChatThreadUiState(
            peer = peer,
            myUserId = SessionManager.currentUser.value?.id,
        )
    )
    val state: StateFlow<ChatThreadUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeJob = viewModelScope.launch {
            chatRepository.observeMessages(peer.listingId, peer.userId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value, error = null) }
    }

    fun selectMessage(message: ApartmentChatMessage?) {
        _state.update { it.copy(selectedMessage = message) }
    }

    fun send() {
        val content = _state.value.draft
        if (content.isBlank() || _state.value.isSending) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            chatRepository.sendMessage(
                listingId = peer.listingId,
                bookingId = peer.bookingId,
                peerUserId = peer.userId,
                content = content,
            ).onSuccess {
                _state.update { it.copy(draft = "", isSending = false) }
            }.onFailure { e ->
                _state.update { it.copy(isSending = false, error = e.userMessage()) }
            }
        }
    }

    fun deleteSelectedMessage() {
        val message = _state.value.selectedMessage ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(peer.listingId, peer.userId, message.id)
                .onSuccess {
                    _state.update { it.copy(selectedMessage = null) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(selectedMessage = null, error = e.userMessage())
                    }
                }
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
