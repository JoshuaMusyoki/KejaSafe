package com.rotd.kejasafe.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.data.platformEpochMillis
import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.ChatTimelineItem
import com.rotd.kejasafe.domain.chat.buildChatTimeline
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.theme.KejaSafeDashboardColors as KejaSafeWebChatColors
import com.rotd.kejasafe.theme.LocalEmojiFontFamily
import com.rotd.kejasafe.theme.ProvideEmojiFont

/**
 * Modern 3-Column Split Web Layout for KejaSafe Chat reusing brand theme colors.
 */
@Composable
fun KejaSafeWebChatLayout(
    inboxState: ChatInboxUiState,
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenProfile: (ApartmentChatPeer) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (ChatInboxTab) -> Unit,
    loadAttachment: (String) -> ByteArray?,
    selectedPeer: ApartmentChatPeer?,
    threadState: ChatThreadUiState?,
    threadDraft: String,
    onThreadDraftChange: (String) -> Unit,
    onSendThreadMessage: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
) {
    ProvideEmojiFont {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(KejaSafeWebChatColors.ThreadBg)
    ) {
        // Column 2: Middle Conversations / Tenant List
        KejaSafeMiddleChatroomsList(
            modifier = Modifier
                .width(350.dp)
                .fillMaxHeight(),
            state = inboxState,
            selectedPeer = selectedPeer,
            onOpenThread = onOpenThread,
            onOpenProfile = onOpenProfile,
            onSearchQueryChange = onSearchQueryChange,
            onSelectTab = onSelectTab,
            loadAttachment = loadAttachment
        )

        // Column 3: Right Active Live Chat Thread Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(KejaSafeWebChatColors.ThreadBg)
        ) {
            if (selectedPeer != null && threadState != null) {
                KejaSafeThreadPanel(
                    peer = selectedPeer,
                    state = threadState,
                    draft = threadDraft,
                    onDraftChange = onThreadDraftChange,
                    onSend = onSendThreadMessage,
                    onPickImages = onPickImages,
                    onPickPdfs = onPickPdfs,
                    loadAttachment = loadAttachment
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = KejaSafeWebChatColors.TextGrey.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a conversation to start messaging",
                            style = MaterialTheme.typography.titleMedium,
                            color = KejaSafeWebChatColors.TextGrey
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
fun KejaSafeWebDashboardShell(
    currentTab: KejaSafeNavTab,
    currentChatTab: ChatInboxTab = ChatInboxTab.MESSAGES,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    onChatTabSelected: (ChatInboxTab) -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(KejaSafeWebChatColors.BackgroundDark)
    ) {
        KejaSafeWebSidebar(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight(),
            currentTab = currentTab,
            currentChatTab = currentChatTab,
            onNavigateHome = { onTabSelected(KejaSafeNavTab.HOME) },
            onNavigateChat = {
                onTabSelected(KejaSafeNavTab.CHAT)
                onChatTabSelected(ChatInboxTab.MESSAGES)
            },
            onNavigateTenants = {
                onTabSelected(KejaSafeNavTab.CHAT)
                onChatTabSelected(ChatInboxTab.PEOPLE)
            },
            onNavigateBrowseListings = { onTabSelected(KejaSafeNavTab.HOME) },
            onNavigateRent = { onTabSelected(KejaSafeNavTab.BILLS) },
            onNavigateLiveTours = { onTabSelected(KejaSafeNavTab.BILLS) },
            onNavigateProfile = { onTabSelected(KejaSafeNavTab.PROFILE) },
            onNavigateBadges = { onTabSelected(KejaSafeNavTab.PROFILE) },
            onLogout = onLogout
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(KejaSafeWebChatColors.ThreadBg)
        ) {
            content()
        }
    }
}

@Composable
fun KejaSafeWebSidebar(
    modifier: Modifier = Modifier,
    currentTab: KejaSafeNavTab = KejaSafeNavTab.CHAT,
    currentChatTab: ChatInboxTab = ChatInboxTab.MESSAGES,
    onNavigateHome: () -> Unit = {},
    onNavigateChat: () -> Unit = {},
    onNavigateTenants: () -> Unit = {},
    onNavigateBrowseListings: () -> Unit = {},
    onNavigateRent: () -> Unit = {},
    onNavigateLiveTours: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateBadges: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val user = SessionManager.currentUser.value
    val userName = user?.fullName?.ifBlank { null } ?: user?.email ?: "KejaSafe User"
    val userRole = user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "User"
    val initial = userName.firstOrNull()?.uppercase() ?: "K"

    Column(
        modifier = modifier
            .background(KejaSafeWebChatColors.SidebarBg)
            .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder))
            .padding(vertical = 18.dp, horizontal = 14.dp)
    ) {
        // KejaSafe Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = KejaSafeWebChatColors.EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KEJASAFE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = KejaSafeWebChatColors.TextWhite
            )
        }

        // Profile Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KejaSafeWebChatColors.CardBg)
                .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KejaSafeWebChatColors.EmeraldAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = KejaSafeWebChatColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = userRole,
                    style = MaterialTheme.typography.labelSmall,
                    color = KejaSafeWebChatColors.TextGrey
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = KejaSafeWebChatColors.TextGrey,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                KejaSafeNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentTab == KejaSafeNavTab.HOME,
                    onClick = onNavigateHome
                )
            }

            item {
                KejaSafeNavSectionHeader("COMMUNICATION")
            }

            item {
                KejaSafeNavItem(
                    label = "Apartment Chat",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    isSelected = currentTab == KejaSafeNavTab.CHAT && currentChatTab == ChatInboxTab.MESSAGES,
                    onClick = onNavigateChat
                )
            }

            item {
                KejaSafeNavItem(
                    label = "Tenants & Landlords",
                    icon = Icons.Default.People,
                    isSelected = currentTab == KejaSafeNavTab.CHAT && currentChatTab == ChatInboxTab.PEOPLE,
                    onClick = onNavigateTenants
                )
            }

            item {
                KejaSafeNavSectionHeader("PROPERTIES & BOOKINGS")
            }

            item {
                KejaSafeNavItem(
                    label = "Browse Listings",
                    icon = Icons.Default.House,
                    isSelected = currentTab == KejaSafeNavTab.HOME,
                    onClick = onNavigateBrowseListings
                )
            }
            item {
                KejaSafeNavItem(
                    label = "Rent & Escrow",
                    icon = Icons.Default.ReceiptLong,
                    isSelected = currentTab == KejaSafeNavTab.BILLS,
                    onClick = onNavigateRent
                )
            }
            item {
                KejaSafeNavItem(
                    label = "Live Tours",
                    icon = Icons.Default.Event,
                    isSelected = currentTab == KejaSafeNavTab.BILLS,
                    onClick = onNavigateLiveTours
                )
            }

            item {
                KejaSafeNavSectionHeader("ACCOUNT")
            }

            item {
                KejaSafeNavItem(
                    label = "My Profile",
                    icon = Icons.Default.Person,
                    isSelected = currentTab == KejaSafeNavTab.PROFILE,
                    onClick = onNavigateProfile
                )
            }
            item {
                KejaSafeNavItem(
                    label = "Verified Badges",
                    icon = Icons.Default.CheckCircle,
                    isSelected = currentTab == KejaSafeNavTab.PROFILE,
                    onClick = onNavigateBadges
                )
            }
        }

        // Logout Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLogout)
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logout",
                tint = KejaSafeWebChatColors.EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = KejaSafeWebChatColors.EmeraldPrimary
            )
        }
    }
}

@Composable
fun KejaSafeNavSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = KejaSafeWebChatColors.TextEmeraldLabel,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 12.dp)
    )
}

@Composable
fun KejaSafeNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) KejaSafeWebChatColors.PlumActiveBg else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(
                    BorderStroke(1.dp, KejaSafeWebChatColors.PlumBorder),
                    RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) KejaSafeWebChatColors.EmeraldPrimary else KejaSafeWebChatColors.TextGrey,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) KejaSafeWebChatColors.TextWhite else KejaSafeWebChatColors.TextGrey
        )
    }
}

@Composable
fun KejaSafeMiddleChatroomsList(
    modifier: Modifier = Modifier,
    state: ChatInboxUiState,
    selectedPeer: ApartmentChatPeer?,
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenProfile: (ApartmentChatPeer) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (ChatInboxTab) -> Unit,
    loadAttachment: (String) -> ByteArray?
) {
    Column(
        modifier = modifier
            .background(KejaSafeWebChatColors.MiddleListBg)
            .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder))
            .padding(16.dp)
    ) {
        // Top Tabs Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectTab(ChatInboxTab.MESSAGES) }
                ) {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.selectedTab == ChatInboxTab.MESSAGES) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.selectedTab == ChatInboxTab.MESSAGES) KejaSafeWebChatColors.TextWhite else KejaSafeWebChatColors.TextGrey
                    )
                    if (state.selectedTab == ChatInboxTab.MESSAGES) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(KejaSafeWebChatColors.EmeraldPrimary)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectTab(ChatInboxTab.PEOPLE) }
                ) {
                    Text(
                        text = "People & Residents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.selectedTab == ChatInboxTab.PEOPLE) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.selectedTab == ChatInboxTab.PEOPLE) KejaSafeWebChatColors.TextWhite else KejaSafeWebChatColors.TextGrey
                    )
                    if (state.selectedTab == ChatInboxTab.PEOPLE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(KejaSafeWebChatColors.EmeraldPrimary)
                        )
                    }
                }
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Grid",
                    tint = KejaSafeWebChatColors.TextGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search tenants or listings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KejaSafeWebChatColors.TextGrey
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = KejaSafeWebChatColors.TextGrey
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KejaSafeWebChatColors.InputBg,
                unfocusedContainerColor = KejaSafeWebChatColors.InputBg,
                focusedBorderColor = KejaSafeWebChatColors.EmeraldPrimary,
                unfocusedBorderColor = KejaSafeWebChatColors.CardBorder,
                focusedTextColor = KejaSafeWebChatColors.TextWhite,
                unfocusedTextColor = KejaSafeWebChatColors.TextWhite
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Conversation List
        val rows = if (state.selectedTab == ChatInboxTab.MESSAGES) state.messageRows else state.peopleRows

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(rows, key = { "${it.peer.listingId}_${it.peer.userId}" }) { row ->
                val isSelected = selectedPeer?.userId == row.peer.userId && selectedPeer.listingId == row.peer.listingId
                KejaSafePeerCard(
                    row = row,
                    myUserId = state.myUserId,
                    isSelected = isSelected,
                    onClick = { onOpenThread(row.peer) },
                    onProfileClick = { onOpenProfile(row.peer) }
                )
            }
        }
    }
}

@Composable
fun KejaSafePeerCard(
    row: InboxPeerRow,
    myUserId: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val peer = row.peer
    val last = row.lastMessage
    val initials = peer.displayName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .ifEmpty { "K" }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) KejaSafeWebChatColors.PlumActiveBg else KejaSafeWebChatColors.CardBg)
            .border(
                BorderStroke(1.dp, if (isSelected) KejaSafeWebChatColors.EmeraldPrimary else KejaSafeWebChatColors.CardBorder),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(KejaSafeWebChatColors.InputBg)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = KejaSafeWebChatColors.EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = peer.displayName.ifBlank { peer.listingTitle },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = KejaSafeWebChatColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ChatMessageText(
                text = last?.previewText() ?: (if (peer.peerIsLandlord) "Landlord · ${peer.listingTitle}" else "Resident tenant"),
                style = MaterialTheme.typography.bodySmall,
                color = KejaSafeWebChatColors.TextGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(KejaSafeWebChatColors.EmeraldAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun KejaSafeThreadPanel(
    peer: ApartmentChatPeer,
    state: ChatThreadUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
    loadAttachment: (String) -> ByteArray?
) {
    val listState = rememberLazyListState()
    val timeline = remember(state.messages) {
        buildChatTimeline(state.messages, nowMillis = platformEpochMillis())
    }

    LaunchedEffect(timeline.size) {
        if (timeline.isNotEmpty()) {
            listState.scrollToItem(timeline.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(KejaSafeWebChatColors.SidebarBg)
                .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KejaSafeWebChatColors.TextWhite
                )
                Text(
                    text = peer.listingTitle.ifBlank { "Apartment Chat" },
                    style = MaterialTheme.typography.labelSmall,
                    color = KejaSafeWebChatColors.TextGrey
                )
            }
        }

        // Timeline Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(
                items = timeline,
                key = { item ->
                    when (item) {
                        is ChatTimelineItem.DayHeader -> "header_${item.dayKey}"
                        is ChatTimelineItem.Bubble -> item.message.id
                    }
                }
            ) { item ->
                when (item) {
                    is ChatTimelineItem.DayHeader -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(KejaSafeWebChatColors.CardBg)
                                    .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KejaSafeWebChatColors.TextGrey,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is ChatTimelineItem.Bubble -> {
                        val isMine = item.message.senderId == state.myUserId
                        KejaSafeMessageBubbleRow(
                            message = item.message,
                            isMine = isMine,
                            peerName = peer.displayName,
                            loadAttachment = loadAttachment
                        )
                    }
                }
            }
        }

        // Bottom Input Bar
        KejaSafeComposerBar(
            draft = draft,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onPickImages = onPickImages,
            onPickPdfs = onPickPdfs,
            isSending = state.isSending
        )
    }
}

@Composable
fun KejaSafeMessageBubbleRow(
    message: ApartmentChatMessage,
    isMine: Boolean,
    peerName: String,
    loadAttachment: (String) -> ByteArray?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(KejaSafeWebChatColors.InputBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peerName.firstOrNull()?.uppercase() ?: "K",
                    color = KejaSafeWebChatColors.EmeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            if (!isMine) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = KejaSafeWebChatColors.TextWhite
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .background(if (isMine) KejaSafeWebChatColors.SentBubbleBg else KejaSafeWebChatColors.ReceivedBubbleBg)
                    .border(
                        BorderStroke(1.dp, if (isMine) KejaSafeWebChatColors.PlumBorder else KejaSafeWebChatColors.ReceivedBubbleBorder),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when {
                    message.isImage() -> Column {
                        Box(modifier = Modifier.width(240.dp)) {
                            ChatImageThumb(
                                attachmentId = message.attachmentId,
                                loadAttachment = loadAttachment,
                            )
                        }
                        val caption = cleanMessageText(message.content)
                        if (caption.isNotBlank() && caption != "Photo" && caption != "Image") {
                            ChatMessageText(
                                text = caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = KejaSafeWebChatColors.TextWhite,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    message.isPdf() -> ChatMessageText(
                        text = message.fileName?.takeIf { it.isNotBlank() } ?: "PDF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KejaSafeWebChatColors.TextWhite,
                    )
                    else -> ChatMessageText(
                        text = cleanMessageText(message.content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KejaSafeWebChatColors.TextWhite,
                    )
                }
            }
        }
    }
}

private fun cleanMessageText(input: String): String = input

@Composable
fun KejaSafeComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
    isSending: Boolean
) {
    var attachMenuOpen by remember { mutableStateOf(false) }
    var emojiMenuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(KejaSafeWebChatColors.SidebarBg)
            .border(BorderStroke(1.dp, KejaSafeWebChatColors.CardBorder))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { attachMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = KejaSafeWebChatColors.TextGrey
                    )
                }
            }

            DropdownMenu(
                expanded = attachMenuOpen,
                onDismissRequest = { attachMenuOpen = false },
                modifier = Modifier.background(KejaSafeWebChatColors.CardBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Photo", color = KejaSafeWebChatColors.TextWhite) },
                    onClick = {
                        attachMenuOpen = false
                        onPickImages()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = KejaSafeWebChatColors.EmeraldPrimary)
                    }
                )
                DropdownMenuItem(
                    text = { Text("PDF", color = KejaSafeWebChatColors.TextWhite) },
                    onClick = {
                        attachMenuOpen = false
                        onPickPdfs()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = KejaSafeWebChatColors.EmeraldPrimary)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = {
                Text(
                    text = "Say something...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KejaSafeWebChatColors.TextGrey
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (draft.isNotBlank() && !isSending) onSend() }),
            visualTransformation = emojiVisualTransformation(LocalEmojiFontFamily.current),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KejaSafeWebChatColors.InputBg,
                unfocusedContainerColor = KejaSafeWebChatColors.InputBg,
                focusedBorderColor = KejaSafeWebChatColors.EmeraldPrimary,
                unfocusedBorderColor = KejaSafeWebChatColors.InputBorder,
                focusedTextColor = KejaSafeWebChatColors.TextWhite,
                unfocusedTextColor = KejaSafeWebChatColors.TextWhite
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box {
            IconButton(onClick = { emojiMenuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfied,
                    contentDescription = "Emoji",
                    tint = KejaSafeWebChatColors.TextGrey
                )
            }
            DropdownMenu(
                expanded = emojiMenuOpen,
                onDismissRequest = { emojiMenuOpen = false },
                modifier = Modifier.background(KejaSafeWebChatColors.CardBg)
            ) {
                ChatEmojiPicker(
                    onPick = { emoji ->
                        emojiMenuOpen = false
                        onDraftChange(draft + emoji)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(KejaSafeWebChatColors.EmeraldAccent)
                .clickable(enabled = draft.isNotBlank() && !isSending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (draft.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChatEmojiPicker(onPick: (String) -> Unit) {
    val emojiFont = LocalEmojiFontFamily.current
    Column(modifier = Modifier.padding(8.dp)) {
        ChatEmojiChoices.chunked(8).forEach { row ->
            Row {
                row.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontFamily = emojiFont,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { onPick(emoji) }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

private val ChatEmojiChoices = listOf(
    "😀", "😂", "😍", "😎", "😢", "😡", "👍", "🙏",
    "👏", "🔥", "🎉", "❤️", "🏠", "✅", "👋", "🤝",
)
