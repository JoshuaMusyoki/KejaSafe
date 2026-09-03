package com.rotd.kejasafe.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.kejasafe.chat.components.ApartmentMessageActionSheet
import com.rotd.kejasafe.chat.components.ChatPeopleSearchField
import com.rotd.kejasafe.chat.components.MessageReceiptTicks
import com.rotd.kejasafe.chat.components.PeerProfileBottomSheet
import com.rotd.kejasafe.chat.components.formatJoinedDate
import com.rotd.kejasafe.components.KejaSafeFloatingNavBar
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.ChatTimelineItem
import com.rotd.kejasafe.domain.chat.buildChatTimeline
import com.rotd.kejasafe.media.PdfReaderScreen
import com.rotd.kejasafe.media.decodeToImageBitmap
import com.rotd.kejasafe.media.renderPdfFirstPage
import com.rotd.kejasafe.media.rememberImagePickerLauncher
import com.rotd.kejasafe.media.rememberPdfPickerLauncher
import com.rotd.kejasafe.media.rememberPdfShareHandler
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatInboxScreen(
    selectedTab: KejaSafeNavTab = KejaSafeNavTab.CHAT,
    onTabSelected: (KejaSafeNavTab) -> Unit = {},
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenLiveTour: (sessionId: String) -> Unit = {},
    onCreateListingClick: (() -> Unit)? = null,
    viewModel: ChatInboxViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val role = SessionManager.currentUser.value?.role
    val isLandlord = role == com.rotd.kejasafe.UserRole.LANDLORD ||
        role == com.rotd.kejasafe.UserRole.ADMIN
    val peopleTabLabel = if (isLandlord) "Tenants" else "People"

    LaunchedEffect(SessionManager.currentUser.value?.id) {
        viewModel.refresh()
    }

    LaunchedEffect(state.navigateToLiveTourId) {
        val id = state.navigateToLiveTourId ?: return@LaunchedEffect
        viewModel.consumeLiveTourNavigation()
        onOpenLiveTour(id)
    }

    val profileRow = state.profilePeer?.let { peer ->
        state.peopleRows.firstOrNull {
            it.peer.userId == peer.userId && it.peer.listingId == peer.listingId
        } ?: InboxPeerRow(peer = peer)
    }
    profileRow?.let { row ->
        PeerProfileBottomSheet(
            row = row,
            myUserId = state.myUserId,
            onDismiss = viewModel::dismissProfile,
            onConnect = {
                viewModel.connect(row.peer.userId)
                viewModel.dismissProfile()
            },
            onAccept = {
                viewModel.accept(row.peer.userId)
                viewModel.dismissProfile()
            },
            onReject = {
                viewModel.reject(row.peer.userId)
                viewModel.dismissProfile()
            },
            onMessage = {
                viewModel.dismissProfile()
                onOpenThread(row.peer)
            },
            onCall = viewModel::startCallWithProfilePeer,
            callBusy = state.callBusy,
            callError = state.callError,
        )
    }

    Scaffold(
        bottomBar = {
            KejaSafeFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = onCreateListingClick.takeIf { isLandlord },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Refresh",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = viewModel::refresh)
                )
            }

            TabRow(
                selectedTabIndex = if (state.selectedTab == ChatInboxTab.MESSAGES) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(
                    selected = state.selectedTab == ChatInboxTab.MESSAGES,
                    onClick = { viewModel.selectTab(ChatInboxTab.MESSAGES) },
                    text = { Text("Messages") }
                )
                Tab(
                    selected = state.selectedTab == ChatInboxTab.PEOPLE,
                    onClick = { viewModel.selectTab(ChatInboxTab.PEOPLE) },
                    text = { Text(peopleTabLabel) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state.selectedTab) {
                ChatInboxTab.MESSAGES -> {
                    Text(
                        text = "Connected apartment chats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    MessagesTabContent(
                        state = state,
                        onOpenThread = onOpenThread,
                        onOpenProfile = viewModel::openProfile,
                        onRetry = viewModel::refresh,
                        loadAttachment = viewModel::loadAttachment,
                    )
                }
                ChatInboxTab.PEOPLE -> {
                    ChatPeopleSearchField(
                        query = state.searchQuery,
                        onQueryChange = viewModel::updateSearch,
                        placeholder = if (isLandlord) "Search tenants…" else "Search people…",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Your landlord is connected by default. Other tenants need a request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    PeopleTabContent(
                        state = state,
                        onOpenThread = onOpenThread,
                        onOpenProfile = viewModel::openProfile,
                        onConnect = viewModel::connect,
                        onAccept = viewModel::accept,
                        onRetry = viewModel::refresh,
                        loadAttachment = viewModel::loadAttachment,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessagesTabContent(
    state: ChatInboxUiState,
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenProfile: (ApartmentChatPeer) -> Unit,
    onRetry: () -> Unit,
    loadAttachment: (String) -> ByteArray?,
) {
    when {
        state.isLoading && state.messageRows.isEmpty() -> LoadingBlock()
        state.messageRows.isEmpty() -> EmptyApartmentChat(
            hint = state.error ?: state.emptyMessagesHint,
            onRetry = onRetry
        )
        else -> {
            state.error?.let { ErrorLine(it) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    state.messageRows,
                    key = { "${it.peer.listingId}_${it.peer.userId}" }
                ) { row ->
                    PeerRow(
                        row = row,
                        myUserId = state.myUserId,
                        showConnectActions = false,
                        onClick = { onOpenThread(row.peer) },
                        onProfileClick = { onOpenProfile(row.peer) },
                        loadAttachment = loadAttachment,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeopleTabContent(
    state: ChatInboxUiState,
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenProfile: (ApartmentChatPeer) -> Unit,
    onConnect: (String) -> Unit,
    onAccept: (String) -> Unit,
    onRetry: () -> Unit,
    loadAttachment: (String) -> ByteArray?,
) {
    when {
        state.isLoading && state.peopleRows.isEmpty() -> LoadingBlock()
        state.peopleRows.isEmpty() -> EmptyApartmentChat(
            hint = state.error ?: state.emptyPeopleHint,
            onRetry = onRetry
        )
        else -> {
            state.error?.let { ErrorLine(it) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    state.peopleRows,
                    key = { "people_${it.peer.listingId}_${it.peer.userId}" }
                ) { row ->
                    PeerRow(
                        row = row,
                        myUserId = state.myUserId,
                        showConnectActions = true,
                        onClick = {
                            when (row.actionFor(state.myUserId)) {
                                PeerConnectAction.MESSAGE -> onOpenThread(row.peer)
                                else -> onOpenProfile(row.peer)
                            }
                        },
                        onProfileClick = { onOpenProfile(row.peer) },
                        onConnect = { onConnect(row.peer.userId) },
                        onAccept = { onAccept(row.peer.userId) },
                        loadAttachment = loadAttachment,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorLine(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun PeerRow(
    row: InboxPeerRow,
    myUserId: String?,
    showConnectActions: Boolean,
    onClick: () -> Unit,
    onProfileClick: () -> Unit,
    onConnect: (() -> Unit)? = null,
    onAccept: (() -> Unit)? = null,
    loadAttachment: (String) -> ByteArray? = { null },
) {
    val peer = row.peer
    val action = row.actionFor(myUserId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (peer.peerIsLandlord) Icons.Default.Home else Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = peer.listingTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (peer.joinedAtMillis > 0L) {
                Text(
                    text = "Joined ${formatJoinedDate(peer.joinedAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val last = row.lastMessage
                if (last != null && last.senderId == myUserId) {
                    MessageReceiptTicks(status = last.status)
                }
                if (last != null && last.isImage()) {
                    InboxImageThumb(
                        attachmentId = last.attachmentId,
                        loadAttachment = loadAttachment,
                    )
                }
                Text(
                    text = when {
                        last != null -> last.previewText()
                        action == PeerConnectAction.PENDING_OUTGOING -> "Request pending"
                        action == PeerConnectAction.ACCEPT -> "Wants to connect"
                        action == PeerConnectAction.CONNECT -> "Tap to connect"
                        peer.peerIsLandlord -> "Your landlord"
                        else -> "Your tenant"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (action) {
                        PeerConnectAction.ACCEPT, PeerConnectAction.CONNECT ->
                            MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        if (showConnectActions) {
            when (action) {
                PeerConnectAction.CONNECT -> {
                    TextButton(onClick = { onConnect?.invoke() }) {
                        Text("Connect", fontWeight = FontWeight.SemiBold)
                    }
                }
                PeerConnectAction.ACCEPT -> {
                    TextButton(onClick = { onAccept?.invoke() }) {
                        Text("Accept", fontWeight = FontWeight.SemiBold)
                    }
                }
                PeerConnectAction.MESSAGE -> {
                    IconButton(onClick = onClick) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message")
                    }
                }
                else -> Unit
            }
        }
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "View profile",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InboxImageThumb(
    attachmentId: String?,
    loadAttachment: (String) -> ByteArray?,
) {
    var bitmap by remember(attachmentId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(attachmentId) {
        bitmap = attachmentId
            ?.let(loadAttachment)
            ?.decodeToImageBitmap()
    }
    val image = bitmap
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptyApartmentChat(hint: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Refresh",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    peer: ApartmentChatPeer,
    onBack: () -> Unit,
    viewModel: ChatThreadViewModel = koinViewModel(key = "${peer.listingId}_${peer.userId}") {
        parametersOf(peer)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var attachMenuOpen by remember { mutableStateOf(false) }
    var openPdf by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    var openImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val sharePdf = rememberPdfShareHandler()
    val pickImages = rememberImagePickerLauncher { files ->
        viewModel.sendAttachments(files, ApartmentChatMessage.TYPE_IMAGE)
    }
    val pickPdfs = rememberPdfPickerLauncher { files ->
        viewModel.sendAttachments(files, ApartmentChatMessage.TYPE_PDF)
    }
    val timeline = remember(state.messages) {
        buildChatTimeline(state.messages, nowMillis = System.currentTimeMillis())
    }

    LaunchedEffect(peer.listingId, peer.userId) {
        viewModel.onThreadVisible()
    }

    LaunchedEffect(timeline.size) {
        if (timeline.isNotEmpty()) {
            listState.animateScrollToItem(timeline.lastIndex)
        }
    }

    openPdf?.let { (name, bytes) ->
        PdfReaderScreen(
            fileName = name,
            bytes = bytes,
            onBack = { openPdf = null },
            onShare = sharePdf,
        )
        return
    }

    openImage?.let { bitmap ->
        Dialog(
            onDismissRequest = { openImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
                    .clickable { openImage = null },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Shared photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    state.selectedMessage?.let { selected ->
        ApartmentMessageActionSheet(
            message = selected,
            isMine = selected.senderId == state.myUserId,
            onDismiss = { viewModel.selectMessage(null) },
            onCopy = {
                clipboard.setText(AnnotatedString(selected.previewText()))
            },
            onDelete = {
                scope.launch { viewModel.deleteSelectedMessage() }
            },
            onOpen = selected.attachmentId?.takeIf { selected.isPdf() }?.let { attachmentId ->
                {
                    val bytes = viewModel.loadAttachment(attachmentId)
                    if (bytes != null) {
                        openPdf = (selected.fileName ?: "document.pdf") to bytes
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.peer?.displayName ?: peer.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            peer.listingTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                state.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { attachMenuOpen = true },
                            enabled = !state.isSending,
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                        }
                        DropdownMenu(
                            expanded = attachMenuOpen,
                            onDismissRequest = { attachMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Photo") },
                                onClick = {
                                    attachMenuOpen = false
                                    pickImages()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("PDF") },
                                onClick = {
                                    attachMenuOpen = false
                                    pickPdfs()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::updateDraft,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 96.dp),
                        placeholder = {
                            Text(
                                text = "Message…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (state.draft.isNotBlank() && !state.isSending) {
                                    viewModel.send()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )
                    IconButton(
                        onClick = viewModel::send,
                        enabled = state.draft.isNotBlank() && !state.isSending
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(
                items = timeline,
                key = { item ->
                    when (item) {
                        is ChatTimelineItem.DayHeader -> "day_${item.dayKey}"
                        is ChatTimelineItem.Bubble -> item.message.id
                    }
                },
            ) { item ->
                when (item) {
                    is ChatTimelineItem.DayHeader -> DateHeaderChip(item.label)
                    is ChatTimelineItem.Bubble -> MessageBubble(
                        message = item.message,
                        isMine = item.message.senderId == state.myUserId,
                        loadAttachment = viewModel::loadAttachment,
                        onLongPress = { viewModel.selectMessage(item.message) },
                        onOpenImage = { openImage = it },
                        onOpenPdf = { name, bytes -> openPdf = name to bytes },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ApartmentChatMessage,
    isMine: Boolean,
    loadAttachment: (String) -> ByteArray?,
    onLongPress: () -> Unit,
    onOpenImage: (ImageBitmap) -> Unit,
    onOpenPdf: (String, ByteArray) -> Unit,
) {
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .combinedClickable(
                    onClick = {
                        val id = message.attachmentId ?: return@combinedClickable
                        val bytes = loadAttachment(id) ?: return@combinedClickable
                        when {
                            message.isImage() -> {
                                bytes.decodeToImageBitmap()?.let(onOpenImage)
                            }
                            message.isPdf() -> {
                                onOpenPdf(message.fileName ?: "document.pdf", bytes)
                            }
                        }
                    },
                    onLongClick = onLongPress
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when {
                message.isImage() -> {
                    ChatImageThumb(
                        attachmentId = message.attachmentId,
                        loadAttachment = loadAttachment,
                    )
                    if (message.content.isNotBlank() &&
                        message.content != "Photo" &&
                        message.content != "Image"
                    ) {
                        Text(
                            text = message.content,
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                message.isPdf() -> {
                    ChatPdfThumb(
                        attachmentId = message.attachmentId,
                        fileName = message.fileName ?: "PDF",
                        textColor = textColor,
                        loadAttachment = loadAttachment,
                    )
                    if (message.content.isNotBlank() &&
                        message.content != "PDF" &&
                        message.content != message.fileName
                    ) {
                        Text(
                            text = message.content,
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                else -> {
                    Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = formatChatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            if (isMine) {
                MessageReceiptTicks(status = message.status)
            }
        }
    }
}

@Composable
private fun DateHeaderChip(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ChatPdfThumb(
    attachmentId: String?,
    fileName: String,
    textColor: Color,
    loadAttachment: (String) -> ByteArray?,
) {
    var bitmap by remember(attachmentId) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(attachmentId) { mutableStateOf(false) }
    LaunchedEffect(attachmentId) {
        val bytes = attachmentId?.let(loadAttachment)
        bitmap = bytes?.let {
            withContext(Dispatchers.Default) {
                renderPdfFirstPage(it, maxWidthPx = 720, maxHeightPx = 960)
            }
        }
        failed = bitmap == null
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 240.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val preview = bitmap
            when {
                preview != null -> Image(
                    bitmap = preview,
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    contentScale = ContentScale.Fit,
                )
                failed -> Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                else -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = fileName,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Tap to open",
            color = textColor.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ChatImageThumb(
    attachmentId: String?,
    loadAttachment: (String) -> ByteArray?,
) {
    var bitmap by remember(attachmentId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(attachmentId) {
        bitmap = attachmentId
            ?.let(loadAttachment)
            ?.decodeToImageBitmap()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 220.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Shared photo",
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun formatChatTime(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = ((totalMinutes / 60) % 24).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
