package com.rotd.havenpad.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.CallLogEntry
import com.rotd.havenpad.domain.chat.ApartmentChatMessage
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.domain.chat.Connection
import com.rotd.havenpad.domain.chat.ConnectionStatus
import com.rotd.havenpad.domain.chat.connectionIdFor
import com.rotd.havenpad.domain.chat.replySnippet
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.PickedImage
import com.rotd.havenpad.network.CallLogBus
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatInboxTab { MESSAGES, PEOPLE, CALLS }

/** Opens a conversation inside the web chat panel instead of a full-screen thread. */
object ChatPanelLaunch {
    private val _pending = MutableStateFlow<ApartmentChatPeer?>(null)
    val pending = _pending.asStateFlow()

    fun open(peer: ApartmentChatPeer) {
        _pending.value = peer
    }

    fun consume() {
        _pending.value = null
    }
}

/** Sidebar clicks and the chat tabs share this so the web panel highlights the open section. */
object ChatSidebarRequest {
    private val _tab = MutableStateFlow(ChatInboxTab.MESSAGES)
    val tab = _tab.asStateFlow()

    fun select(tab: ChatInboxTab) {
        _tab.value = tab
    }
}

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
    val callRows: List<CallLogEntry> = emptyList(),
    val callBusy: Boolean = false,
    val callError: String? = null,
    /** When set, UI navigates to LiveTourCall. */
    val navigateToLiveTourId: String? = null,
)

data class PendingChatFile(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String,
    val type: String,
)

data class ChatThreadUiState(
    val peer: ApartmentChatPeer? = null,
    val myUserId: String? = null,
    val messages: List<ApartmentChatMessage> = emptyList(),
    val draft: String = "",
    val pendingFiles: List<PendingChatFile> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
    val selectedMessage: ApartmentChatMessage? = null,
    val replyingTo: ApartmentChatMessage? = null,
    val pinnedMessage: ApartmentChatMessage? = null,
    val showForwardPicker: Boolean = false,
    val forwardPeers: List<ApartmentChatPeer> = emptyList(),
    /** True until the first page of messages is ready to show. */
    val isInitialLoading: Boolean = true,
    /** True while older history is being revealed. */
    val isLoadingOlder: Boolean = false,
    val hasMoreOlder: Boolean = false,
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
        viewModelScope.launch {
            CallLogBus.tick.collect {
                if (CallLogBus.consumeFocus()) {
                    selectTab(ChatInboxTab.CALLS)
                }
                loadCallHistory()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(5_000)
                if (SessionManager.isLogged() && _state.value.selectedTab == ChatInboxTab.CALLS) {
                    loadCallHistory()
                }
            }
        }
    }

    fun loadCallHistory() {
        viewModelScope.launch {
            LiveTourClient.callHistory()
                .onSuccess { rows -> _state.update { it.copy(callRows = rows) } }
        }
    }

    fun deleteCall(entry: CallLogEntry) {
        _state.update { state ->
            state.copy(callRows = state.callRows.filterNot { it.sessionId == entry.sessionId })
        }
        viewModelScope.launch {
            LiveTourClient.hideCall(entry.sessionId).onFailure {
                loadCallHistory()
            }
        }
    }

    fun callLogPeer(entry: CallLogEntry) {
        viewModelScope.launch {
            _state.update { it.copy(callBusy = true, callError = null) }
            LiveTourClient.startDirectTour(
                listingId = entry.listingId,
                peerUserId = entry.peerUserId,
            ).onSuccess { session ->
                _state.update {
                    it.copy(callBusy = false, navigateToLiveTourId = session.id)
                }
            }.onFailure { e ->
                _state.update { it.copy(callBusy = false, callError = e.userMessage()) }
            }
        }
    }

    fun selectTab(tab: ChatInboxTab) {
        _state.update { it.copy(selectedTab = tab) }
        ChatSidebarRequest.select(tab)
        if (tab == ChatInboxTab.CALLS) loadCallHistory()
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun openProfile(peer: ApartmentChatPeer) {
        _state.update { it.copy(profilePeer = peer) }
    }

    fun dismissProfile() {
        _state.update {
            it.copy(profilePeer = null, callError = null, callBusy = false)
        }
    }

    fun startCallWithProfilePeer() {
        val peer = _state.value.profilePeer ?: return
        viewModelScope.launch {
            _state.update { it.copy(callBusy = true, callError = null) }
            LiveTourClient.startDirectTour(
                listingId = peer.listingId,
                peerUserId = peer.userId,
            ).onSuccess { session ->
                _state.update {
                    it.copy(
                        callBusy = false,
                        profilePeer = null,
                        navigateToLiveTourId = session.id,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(callBusy = false, callError = e.userMessage())
                }
            }
        }
    }

    fun consumeLiveTourNavigation() {
        _state.update { it.copy(navigateToLiveTourId = null) }
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
        loadCallHistory()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            chatRepository.refreshEligiblePeers()
                .onSuccess {
                    // Always clear loading here. If the peers Flow emits the same empty
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.userMessage())
                    }
                }
        }
    }

    fun loadAttachment(attachmentId: String): ByteArray? =
        chatRepository.loadAttachment(attachmentId)

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
    private var allMessages: List<ApartmentChatMessage> = emptyList()
    /** How many newest messages are currently visible. */
    private var visibleCount: Int = 0
    private var initialRevealDone: Boolean = false

    init {
        viewModelScope.launch {
            SessionManager.currentUser.collect { user ->
                _state.update { it.copy(myUserId = user?.id) }
            }
        }
        observeJob = viewModelScope.launch {
            chatRepository.syncConversation(peer.listingId, peer.userId)
            chatRepository.observeMessages(peer.listingId, peer.userId).collect { messages ->
                allMessages = messages.sortedBy { it.timestamp }
                if (!initialRevealDone) {
                    revealInitialPage()
                } else {
                    // Keep the user's history window; always include the newest messages.
                    val minWindow = PAGE_SIZE.coerceAtMost(allMessages.size)
                    if (visibleCount < minWindow) visibleCount = minWindow
                    if (visibleCount > allMessages.size) visibleCount = allMessages.size
                    publishVisible(isInitialLoading = false)
                }
            }
        }
        viewModelScope.launch {
            chatRepository.observeEligiblePeers().collect { peers ->
                _state.update { current ->
                    current.copy(
                        forwardPeers = peers.filter { candidate ->
                            candidate.userId != peer.userId || candidate.listingId != peer.listingId
                        }
                    )
                }
            }
        }
        // Poll so messages from the other party appear without leaving the thread.
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4_000)
                chatRepository.syncConversation(peer.listingId, peer.userId)
            }
        }
    }

    fun loadOlderMessages() {
        if (_state.value.isLoadingOlder || !_state.value.hasMoreOlder) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingOlder = true) }
            // Short delay so the top shimmer is visible before history expands.
            kotlinx.coroutines.delay(220)
            visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(allMessages.size)
            publishVisible(isInitialLoading = false, isLoadingOlder = false)
        }
    }

    private suspend fun revealInitialPage() {
        _state.update { it.copy(isInitialLoading = true) }
        // Brief skeleton so heavy threads don't flash a full dump.
        kotlinx.coroutines.delay(280)
        visibleCount = PAGE_SIZE.coerceAtMost(allMessages.size)
        initialRevealDone = true
        publishVisible(isInitialLoading = false)
    }

    private fun publishVisible(
        isInitialLoading: Boolean,
        isLoadingOlder: Boolean = false,
    ) {
        val visible = if (visibleCount <= 0) {
            emptyList()
        } else {
            allMessages.takeLast(visibleCount)
        }
        val pinned = allMessages.filter { it.pinnedAt > 0L }.maxByOrNull { it.pinnedAt }
        _state.update {
            it.copy(
                messages = visible,
                isInitialLoading = isInitialLoading,
                isLoadingOlder = isLoadingOlder,
                hasMoreOlder = visibleCount < allMessages.size,
                pinnedMessage = pinned,
            )
        }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value, error = null) }
    }

    fun selectMessage(message: ApartmentChatMessage?) {
        if (message?.deleted == true) return
        _state.update { it.copy(selectedMessage = message, showForwardPicker = false) }
    }

    fun startReply(message: ApartmentChatMessage) {
        if (message.deleted) return
        _state.update {
            it.copy(replyingTo = message, selectedMessage = null, showForwardPicker = false)
        }
    }

    fun cancelReply() {
        _state.update { it.copy(replyingTo = null) }
    }

    fun openForwardPicker() {
        _state.update { it.copy(showForwardPicker = true) }
    }

    fun dismissForwardPicker() {
        _state.update { it.copy(showForwardPicker = false, selectedMessage = null) }
    }

    fun stageAttachments(files: List<PickedImage>, type: String) {
        if (files.isEmpty() || _state.value.isSending) return
        _state.update {
            it.copy(
                pendingFiles = files.map { file ->
                    PendingChatFile(
                        fileName = file.fileName,
                        bytes = file.bytes,
                        mimeType = file.mimeType,
                        type = type,
                    )
                },
                error = null,
            )
        }
    }

    fun removePendingFile(index: Int) {
        _state.update { state ->
            state.copy(pendingFiles = state.pendingFiles.filterIndexed { i, _ -> i != index })
        }
    }

    fun reportAttachError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun send() {
        val snapshot = _state.value
        if (snapshot.isSending) return
        val content = snapshot.draft.trim()
        val pending = snapshot.pendingFiles
        if (content.isEmpty() && pending.isEmpty()) return
        val reply = snapshot.replyingTo
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            if (pending.isEmpty()) {
                chatRepository.sendMessage(
                    listingId = peer.listingId,
                    bookingId = peer.bookingId,
                    peerUserId = peer.userId,
                    content = content,
                    replyToId = reply?.id,
                    replyToPreview = reply?.replySnippet().orEmpty(),
                ).onSuccess {
                    _state.update { it.copy(draft = "", isSending = false, replyingTo = null) }
                }.onFailure { e ->
                    _state.update { it.copy(isSending = false, error = e.userMessage()) }
                }
                return@launch
            }
            var lastError: String? = null
            pending.forEachIndexed { index, file ->
                val caption = if (index == pending.lastIndex) content else ""
                chatRepository.sendMessage(
                    listingId = peer.listingId,
                    bookingId = peer.bookingId,
                    peerUserId = peer.userId,
                    content = caption,
                    type = file.type,
                    fileName = file.fileName,
                    mimeType = file.mimeType,
                    attachmentBytes = file.bytes,
                    replyToId = if (index == 0) reply?.id else null,
                    replyToPreview = if (index == 0) reply?.replySnippet().orEmpty() else "",
                ).onFailure { e ->
                    lastError = e.userMessage()
                }
            }
            _state.update {
                it.copy(
                    draft = if (lastError == null) "" else it.draft,
                    pendingFiles = if (lastError == null) emptyList() else it.pendingFiles,
                    isSending = false,
                    error = lastError,
                    replyingTo = if (lastError == null) null else it.replyingTo,
                )
            }
        }
    }

    fun reactToSelected(emoji: String) {
        val message = _state.value.selectedMessage ?: return
        viewModelScope.launch {
            chatRepository.reactToMessage(peer.listingId, peer.userId, message.id, emoji)
                .onSuccess {
                    _state.update { it.copy(selectedMessage = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(selectedMessage = null, error = e.userMessage()) }
                }
        }
    }

    fun pinSelectedMessage() {
        val message = _state.value.selectedMessage ?: return
        viewModelScope.launch {
            chatRepository.setMessagePinned(
                listingId = peer.listingId,
                peerUserId = peer.userId,
                messageId = message.id,
                pinned = message.pinnedAt <= 0L,
            ).onSuccess {
                _state.update { it.copy(selectedMessage = null) }
            }.onFailure { e ->
                _state.update { it.copy(selectedMessage = null, error = e.userMessage()) }
            }
        }
    }

    fun unpinMessage(message: ApartmentChatMessage) {
        viewModelScope.launch {
            chatRepository.setMessagePinned(
                listingId = peer.listingId,
                peerUserId = peer.userId,
                messageId = message.id,
                pinned = false,
            )
        }
    }

    fun forwardSelectedTo(target: ApartmentChatPeer) {
        val message = _state.value.selectedMessage ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null, showForwardPicker = false) }
            val bytes = message.attachmentId?.let { chatRepository.loadAttachment(it) }
            val canAttach = bytes != null && (message.isImage() || message.isPdf())
            chatRepository.sendMessage(
                listingId = target.listingId,
                bookingId = target.bookingId,
                peerUserId = target.userId,
                content = if (canAttach) message.content else message.previewText(),
                type = if (canAttach) message.type else ApartmentChatMessage.TYPE_TEXT,
                fileName = if (canAttach) message.fileName else null,
                mimeType = if (canAttach) message.mimeType else null,
                attachmentBytes = if (canAttach) bytes else null,
                forwarded = true,
            ).onSuccess {
                _state.update { it.copy(isSending = false, selectedMessage = null) }
            }.onFailure { e ->
                _state.update {
                    it.copy(isSending = false, selectedMessage = null, error = e.userMessage())
                }
            }
        }
    }

    fun loadAttachment(attachmentId: String): ByteArray? =
        chatRepository.loadAttachment(attachmentId)

    fun onThreadVisible() {
        viewModelScope.launch {
            chatRepository.markMessagesRead(peer.listingId, peer.userId)
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

    companion object {
        private const val PAGE_SIZE = 18
    }
}
