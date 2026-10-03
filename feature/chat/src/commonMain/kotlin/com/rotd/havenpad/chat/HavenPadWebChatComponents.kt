package com.rotd.havenpad.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.havenpad.CallLogEntry
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.data.platformEpochMillis
import com.rotd.havenpad.chat.components.ForwardedLabel
import com.rotd.havenpad.chat.components.MessageReactionChips
import com.rotd.havenpad.chat.components.PinnedMessageBanner
import com.rotd.havenpad.chat.components.ReplyComposerBanner
import com.rotd.havenpad.chat.components.ReplyQuote
import com.rotd.havenpad.chat.components.SwipeToReplyBox
import com.rotd.havenpad.domain.chat.ApartmentChatMessage
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.media.ExpandedPdfPreview
import com.rotd.havenpad.media.decodeToImageBitmap
import com.rotd.havenpad.media.rememberMediaExport
import com.rotd.havenpad.domain.chat.replySnippet
import com.rotd.havenpad.domain.chat.ChatTimelineItem
import com.rotd.havenpad.domain.chat.buildChatTimeline
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.theme.HavenPadDashboardColors as HavenPadWebChatColors
import com.rotd.havenpad.theme.LocalEmojiFontFamily
import com.rotd.havenpad.theme.ProvideEmojiFont
import havenpad.feature.chat.generated.resources.Res
import havenpad.feature.chat.generated.resources.haven_pad_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Modern 3-Column Split Web Layout for HavenPad Chat reusing brand theme colors.
 */
@Composable
fun HavenPadWebChatLayout(
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
    onRemovePending: (Int) -> Unit = {},
    onMessageLongPress: (ApartmentChatMessage) -> Unit = {},
    onMessageReply: (ApartmentChatMessage) -> Unit = {},
    onCancelReply: () -> Unit = {},
    onUnpin: (ApartmentChatMessage) -> Unit = {},
    onCloseChat: () -> Unit = {},
    notificationsAllowed: Boolean = true,
    onEnableNotifications: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onCallLog: (CallLogEntry) -> Unit = {},
    onMessageLog: (CallLogEntry) -> Unit = {},
    onDeleteCall: (CallLogEntry) -> Unit = {},
) {
    ProvideEmojiFont {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenPadWebChatColors.ThreadBg)
    ) {
        // Column 2: Middle Conversations / Tenant List
        HavenPadMiddleChatroomsList(
            modifier = Modifier
                .width(350.dp)
                .fillMaxHeight(),
            state = inboxState,
            selectedPeer = selectedPeer,
            onOpenThread = onOpenThread,
            onOpenProfile = onOpenProfile,
            onSearchQueryChange = onSearchQueryChange,
            onSelectTab = onSelectTab,
            loadAttachment = loadAttachment,
            onOpenNotifications = onOpenNotifications,
            onCallLog = onCallLog,
            onMessageLog = onMessageLog,
            onDeleteCall = onDeleteCall,
        )

        // Column 3: Right Active Live Chat Thread Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(HavenPadWebChatColors.ThreadBg)
        ) {
            if (selectedPeer != null && threadState != null) {
                HavenPadThreadPanel(
                    peer = selectedPeer,
                    state = threadState,
                    draft = threadDraft,
                    onDraftChange = onThreadDraftChange,
                    onSend = onSendThreadMessage,
                    onPickImages = onPickImages,
                    onPickPdfs = onPickPdfs,
                    onRemovePending = onRemovePending,
                    loadAttachment = loadAttachment,
                    onMessageLongPress = onMessageLongPress,
                    onMessageReply = onMessageReply,
                    onCancelReply = onCancelReply,
                    onUnpin = onUnpin,
                    onClose = onCloseChat,
                )
            } else {
                HavenPadEmptyChatPane(
                    notificationsAllowed = notificationsAllowed,
                    onEnableNotifications = onEnableNotifications,
                )
            }
        }
    }
    }
}

@Composable
private fun HavenPadEmptyChatPane(
    notificationsAllowed: Boolean,
    onEnableNotifications: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenPadWebChatColors.ThreadBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(HavenPadWebChatColors.CardBg)
                    .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = HavenPadWebChatColors.EmeraldPrimary,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "HavenPad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HavenPadWebChatColors.TextWhite,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Send and receive messages with your landlord or tenants.",
                style = MaterialTheme.typography.bodyLarge,
                color = HavenPadWebChatColors.TextGrey,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select a chat on the left. Close it any time from the button at the top.",
                style = MaterialTheme.typography.bodyMedium,
                color = HavenPadWebChatColors.TextGrey,
            )
            if (!notificationsAllowed) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onEnableNotifications) {
                    Text("Turn on message notifications")
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = HavenPadWebChatColors.TextGrey,
                )
                Text(
                    text = "Your chats stay on your HavenPad account.",
                    style = MaterialTheme.typography.labelMedium,
                    color = HavenPadWebChatColors.TextGrey,
                )
            }
        }
    }
}

@Composable
fun HavenPadWebDashboardShell(
    currentTab: HavenPadNavTab,
    currentChatTab: ChatInboxTab = ChatInboxTab.MESSAGES,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onChatTabSelected: (ChatInboxTab) -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    bookingsSelected: Boolean = false,
    toursSelected: Boolean = false,
    adminSelected: Boolean = false,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenPadWebChatColors.BackgroundDark)
    ) {
        HavenPadWebSidebar(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight(),
            currentTab = currentTab,
            currentChatTab = currentChatTab,
            onNavigateHome = { onTabSelected(HavenPadNavTab.HOME) },
            onNavigateChat = {
                onTabSelected(HavenPadNavTab.CHAT)
                onChatTabSelected(ChatInboxTab.MESSAGES)
            },
            onNavigateTenants = {
                onTabSelected(HavenPadNavTab.CHAT)
                onChatTabSelected(ChatInboxTab.PEOPLE)
            },
            onNavigateBrowseListings = { onTabSelected(HavenPadNavTab.HOME) },
            onNavigateBookings = onOpenBookings,
            onNavigateRent = { onTabSelected(HavenPadNavTab.BILLS) },
            onNavigateLiveTours = { onTabSelected(HavenPadNavTab.BILLS) },
            onNavigateProfile = { onTabSelected(HavenPadNavTab.PROFILE) },
            onNavigateBadges = { onTabSelected(HavenPadNavTab.PROFILE) },
            onNavigateAdmin = onOpenAdmin,
            bookingsSelected = bookingsSelected,
            toursSelected = toursSelected,
            adminSelected = adminSelected,
            onLogout = onLogout
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .background(HavenPadWebChatColors.ThreadBg)
        ) {
            content()
        }
    }
}

@Composable
fun HavenPadWebSidebar(
    modifier: Modifier = Modifier,
    currentTab: HavenPadNavTab = HavenPadNavTab.CHAT,
    currentChatTab: ChatInboxTab = ChatInboxTab.MESSAGES,
    onNavigateHome: () -> Unit = {},
    onNavigateChat: () -> Unit = {},
    onNavigateTenants: () -> Unit = {},
    onNavigateBrowseListings: () -> Unit = {},
    onNavigateBookings: () -> Unit = {},
    onNavigateRent: () -> Unit = {},
    onNavigateLiveTours: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateBadges: () -> Unit = {},
    onNavigateAdmin: () -> Unit = {},
    bookingsSelected: Boolean = false,
    toursSelected: Boolean = false,
    adminSelected: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val user = SessionManager.currentUser.value
    val userName = user?.fullName?.ifBlank { null } ?: user?.email ?: "HavenPad User"
    val userRole = user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "User"
    val initial = userName.firstOrNull()?.uppercase() ?: "K"

    Column(
        modifier = modifier
            .background(HavenPadWebChatColors.SidebarBg)
            .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder))
            .padding(vertical = 18.dp, horizontal = 14.dp)
    ) {
        // HavenPad Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.haven_pad_logo),
                contentDescription = "HavenPad",
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "HavenPad",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = HavenPadWebChatColors.TextWhite
            )
        }

        // Profile Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HavenPadWebChatColors.CardBg)
                .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HavenPadWebChatColors.EmeraldAccent),
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
                    color = HavenPadWebChatColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = userRole,
                    style = MaterialTheme.typography.labelSmall,
                    color = HavenPadWebChatColors.TextGrey
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = HavenPadWebChatColors.TextGrey,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isAdmin = user?.role == com.rotd.havenpad.UserRole.ADMIN

        // Navigation Items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isAdmin) {
                item {
                    HavenPadNavItem(
                        label = "Admin",
                        icon = Icons.Default.Lock,
                        isSelected = true,
                        onClick = onNavigateAdmin
                    )
                }
            }
            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentTab == HavenPadNavTab.HOME && !bookingsSelected && !toursSelected && !adminSelected,
                    onClick = onNavigateHome
                )
            }

            if (!isAdmin) item {
                HavenPadNavSectionHeader("COMMUNICATION")
            }

            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Apartment Chat",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    isSelected = currentTab == HavenPadNavTab.CHAT &&
                        currentChatTab != ChatInboxTab.PEOPLE &&
                        !bookingsSelected &&
                        !toursSelected &&
                        !adminSelected,
                    onClick = onNavigateChat
                )
            }

            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Tenants & Landlords",
                    icon = Icons.Default.People,
                    isSelected = currentTab == HavenPadNavTab.CHAT &&
                        currentChatTab == ChatInboxTab.PEOPLE &&
                        !bookingsSelected &&
                        !toursSelected &&
                        !adminSelected,
                    onClick = onNavigateTenants
                )
            }

            if (!isAdmin) item {
                HavenPadNavSectionHeader("PROPERTIES & BOOKINGS")
            }

            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Browse Listings",
                    icon = Icons.Default.House,
                    isSelected = false,
                    onClick = onNavigateBrowseListings
                )
            }
            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Bookings",
                    icon = Icons.Default.DateRange,
                    isSelected = bookingsSelected,
                    onClick = onNavigateBookings
                )
            }
            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Rent & Escrow",
                    icon = Icons.Default.ReceiptLong,
                    isSelected = currentTab == HavenPadNavTab.BILLS && !toursSelected && !adminSelected,
                    onClick = onNavigateRent
                )
            }
            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Live Tours",
                    icon = Icons.Default.Event,
                    isSelected = toursSelected,
                    onClick = onNavigateLiveTours
                )
            }

            if (!isAdmin) item {
                HavenPadNavSectionHeader("ACCOUNT")
            }

            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "My Profile",
                    icon = Icons.Default.Person,
                    isSelected = currentTab == HavenPadNavTab.PROFILE && !bookingsSelected && !adminSelected,
                    onClick = onNavigateProfile
                )
            }
            if (!isAdmin) item {
                HavenPadNavItem(
                    label = "Verified Badges",
                    icon = Icons.Default.CheckCircle,
                    isSelected = false,
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
                tint = HavenPadWebChatColors.EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = HavenPadWebChatColors.EmeraldPrimary
            )
        }
    }
}

@Composable
fun HavenPadNavSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = HavenPadWebChatColors.TextEmeraldLabel,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 12.dp)
    )
}

@Composable
fun HavenPadNavItem(
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
            .background(if (isSelected) HavenPadWebChatColors.PlumActiveBg else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(
                    BorderStroke(1.dp, HavenPadWebChatColors.PlumBorder),
                    RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) HavenPadWebChatColors.EmeraldPrimary else HavenPadWebChatColors.TextGrey,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) HavenPadWebChatColors.TextWhite else HavenPadWebChatColors.TextGrey
        )
    }
}

@Composable
fun HavenPadMiddleChatroomsList(
    modifier: Modifier = Modifier,
    state: ChatInboxUiState,
    selectedPeer: ApartmentChatPeer?,
    onOpenThread: (ApartmentChatPeer) -> Unit,
    onOpenProfile: (ApartmentChatPeer) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (ChatInboxTab) -> Unit,
    loadAttachment: (String) -> ByteArray?,
    onOpenNotifications: () -> Unit = {},
    onCallLog: (CallLogEntry) -> Unit = {},
    onMessageLog: (CallLogEntry) -> Unit = {},
    onDeleteCall: (CallLogEntry) -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(HavenPadWebChatColors.MiddleListBg)
            .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder))
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
                        color = if (state.selectedTab == ChatInboxTab.MESSAGES) HavenPadWebChatColors.TextWhite else HavenPadWebChatColors.TextGrey
                    )
                    if (state.selectedTab == ChatInboxTab.MESSAGES) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HavenPadWebChatColors.EmeraldPrimary)
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
                        color = if (state.selectedTab == ChatInboxTab.PEOPLE) HavenPadWebChatColors.TextWhite else HavenPadWebChatColors.TextGrey
                    )
                    if (state.selectedTab == ChatInboxTab.PEOPLE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HavenPadWebChatColors.EmeraldPrimary)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectTab(ChatInboxTab.CALLS) }
                ) {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.selectedTab == ChatInboxTab.CALLS) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.selectedTab == ChatInboxTab.CALLS) HavenPadWebChatColors.TextWhite else HavenPadWebChatColors.TextGrey
                    )
                    if (state.selectedTab == ChatInboxTab.CALLS) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HavenPadWebChatColors.EmeraldPrimary)
                        )
                    }
                }
            }

            NotificationBell(
                onClick = onOpenNotifications,
                tint = HavenPadWebChatColors.TextGrey,
            )
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
                    color = HavenPadWebChatColors.TextGrey
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = HavenPadWebChatColors.TextGrey
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenPadWebChatColors.InputBg,
                unfocusedContainerColor = HavenPadWebChatColors.InputBg,
                focusedBorderColor = HavenPadWebChatColors.EmeraldPrimary,
                unfocusedBorderColor = HavenPadWebChatColors.CardBorder,
                focusedTextColor = HavenPadWebChatColors.TextWhite,
                unfocusedTextColor = HavenPadWebChatColors.TextWhite
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.selectedTab == ChatInboxTab.CALLS) {
            CallHistoryList(
                rows = state.callRows,
                busy = state.callBusy,
                error = state.callError,
                onCall = onCallLog,
                onMessage = onMessageLog,
                onDelete = onDeleteCall,
            )
            return@Column
        }

        // Conversation List
        val rows = if (state.selectedTab == ChatInboxTab.MESSAGES) state.messageRows else state.peopleRows

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(rows, key = { "${it.peer.listingId}_${it.peer.userId}" }) { row ->
                val isSelected = selectedPeer?.userId == row.peer.userId && selectedPeer.listingId == row.peer.listingId
                HavenPadPeerCard(
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
fun HavenPadPeerCard(
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
            .background(if (isSelected) HavenPadWebChatColors.PlumActiveBg else HavenPadWebChatColors.CardBg)
            .border(
                BorderStroke(1.dp, if (isSelected) HavenPadWebChatColors.EmeraldPrimary else HavenPadWebChatColors.CardBorder),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(HavenPadWebChatColors.InputBg)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = HavenPadWebChatColors.EmeraldPrimary,
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
                    text = peer.displayName.ifBlank { "Chat" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = HavenPadWebChatColors.TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ChatMessageText(
                text = last?.previewText() ?: "Tap to open",
                style = MaterialTheme.typography.bodySmall,
                color = HavenPadWebChatColors.TextGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(HavenPadWebChatColors.EmeraldAccent),
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
fun HavenPadThreadPanel(
    peer: ApartmentChatPeer,
    state: ChatThreadUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
    onRemovePending: (Int) -> Unit = {},
    loadAttachment: (String) -> ByteArray?,
    onMessageLongPress: (ApartmentChatMessage) -> Unit,
    onMessageReply: (ApartmentChatMessage) -> Unit,
    onCancelReply: () -> Unit,
    onUnpin: (ApartmentChatMessage) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()
    val export = rememberMediaExport()
    var preview by remember { mutableStateOf<ChatMediaPreview?>(null) }
    var previewNote by remember { mutableStateOf<String?>(null) }
    val timeline = remember(state.messages) {
        buildChatTimeline(state.messages, nowMillis = platformEpochMillis())
    }

    LaunchedEffect(timeline.size) {
        if (timeline.isNotEmpty()) {
            listState.scrollToItem(timeline.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(HavenPadWebChatColors.SidebarBg)
                .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HavenPadWebChatColors.TextWhite
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close chat",
                    tint = HavenPadWebChatColors.TextGrey,
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
            state.pinnedMessage?.let { pinned ->
                item(key = "pinned_${pinned.id}") {
                    PinnedMessageBanner(
                        message = pinned,
                        onUnpin = { onUnpin(pinned) },
                    )
                }
            }
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
                                    .background(HavenPadWebChatColors.CardBg)
                                    .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HavenPadWebChatColors.TextGrey,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is ChatTimelineItem.Bubble -> {
                        val isMine = item.message.senderId == state.myUserId
                        HavenPadMessageBubbleRow(
                            message = item.message,
                            isMine = isMine,
                            peerName = peer.displayName,
                            loadAttachment = loadAttachment,
                            onLongPress = { onMessageLongPress(item.message) },
                            onReply = { onMessageReply(item.message) },
                            onOpenAttachment = {
                                val opened = item.message.toMediaPreview(loadAttachment)
                                if (opened != null) {
                                    previewNote = null
                                    preview = opened
                                }
                            },
                        )
                    }
                }
            }
        }

        // Bottom Input Bar
        state.replyingTo?.let { reply ->
            ReplyComposerBanner(
                preview = reply.replySnippet(),
                onCancel = onCancelReply,
                contentColor = HavenPadWebChatColors.TextWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        HavenPadComposerBar(
            draft = draft,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onPickImages = onPickImages,
            onPickPdfs = onPickPdfs,
            isSending = state.isSending,
            pendingFiles = state.pendingFiles,
            onRemovePending = onRemovePending,
        )
    }
        preview?.let { item ->
            ChatAttachmentPreview(
                preview = item,
                note = previewNote,
                onClose = {
                    preview = null
                    previewNote = null
                },
                onDownload = {
                    previewNote = runCatching {
                        export.download(item.fileName, item.mimeType, item.bytes)
                    }.getOrElse { it.message ?: "Could not save the file" }
                },
                onShare = {
                    previewNote = runCatching {
                        export.share(item.fileName, item.mimeType, item.bytes)
                    }.getOrElse { it.message ?: "Could not share the file" }
                },
            )
        }
    }
}

private class ChatMediaPreview(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val image: ImageBitmap?,
)

private fun ApartmentChatMessage.toMediaPreview(
    loadAttachment: (String) -> ByteArray?,
): ChatMediaPreview? {
    if (!isImage() && !isPdf()) return null
    val id = attachmentId ?: return null
    val bytes = loadAttachment(id) ?: return null
    val name = fileName?.takeIf { it.isNotBlank() } ?: if (isPdf()) "document.pdf" else "photo.jpg"
    val mime = mimeType?.takeIf { it.isNotBlank() }
        ?: if (isPdf()) "application/pdf" else mimeForImageName(name)
    return ChatMediaPreview(
        fileName = name,
        mimeType = mime,
        bytes = bytes,
        image = if (isImage()) bytes.decodeToImageBitmap() else null,
    )
}

private fun mimeForImageName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    else -> "image/jpeg"
}

@Composable
private fun ChatAttachmentPreview(
    preview: ChatMediaPreview,
    note: String?,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1210)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = preview.fileName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }
        val image = preview.image
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = preview.fileName,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentScale = ContentScale.Fit,
            )
        } else if (preview.mimeType == "application/pdf") {
            ExpandedPdfPreview(
                bytes = preview.bytes,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            Text(
                text = "This file could not be previewed.",
                color = Color.White,
                modifier = Modifier.padding(24.dp),
            )
        }
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HavenPadMessageBubbleRow(
    message: ApartmentChatMessage,
    isMine: Boolean,
    peerName: String,
    loadAttachment: (String) -> ByteArray?,
    onLongPress: () -> Unit = {},
    onReply: () -> Unit = {},
    onOpenAttachment: () -> Unit = {},
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
                    .background(HavenPadWebChatColors.InputBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peerName.firstOrNull()?.uppercase() ?: "K",
                    color = HavenPadWebChatColors.EmeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        SwipeToReplyBox(onReply = onReply, modifier = Modifier.widthIn(max = 380.dp)) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
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
                        color = HavenPadWebChatColors.TextWhite
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
                    .background(if (isMine) HavenPadWebChatColors.SentBubbleBg else HavenPadWebChatColors.ReceivedBubbleBg)
                    .border(
                        BorderStroke(1.dp, if (isMine) HavenPadWebChatColors.PlumBorder else HavenPadWebChatColors.ReceivedBubbleBorder),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {
                            if (message.isImage() || message.isPdf()) onOpenAttachment()
                        },
                        onLongClick = onLongPress,
                    )
                    .pointerInput(onLongPress) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (
                                    event.type == PointerEventType.Press &&
                                    event.buttons.isSecondaryPressed
                                ) {
                                    onLongPress()
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (message.deleted) {
                        ChatMessageText(
                            text = message.deletedLabel(isMine),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = HavenPadWebChatColors.TextGrey,
                        )
                    } else if (message.forwarded) {
                        ForwardedLabel(color = HavenPadWebChatColors.TextGrey)
                    }
                    if (!message.deleted && message.replyToPreview.isNotBlank()) {
                        ReplyQuote(
                            preview = message.replyToPreview,
                            color = HavenPadWebChatColors.TextWhite,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    when {
                    message.deleted -> Unit
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
                                color = HavenPadWebChatColors.TextWhite,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    message.isPdf() -> ChatPdfThumb(
                        attachmentId = message.attachmentId,
                        fileName = message.fileName?.takeIf { it.isNotBlank() } ?: "PDF",
                        textColor = HavenPadWebChatColors.TextWhite,
                        loadAttachment = loadAttachment,
                    )
                    else -> ChatMessageText(
                        text = cleanMessageText(message.content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HavenPadWebChatColors.TextWhite,
                    )
                    }
                }
            }
            if (!message.deleted) {
                MessageReactionChips(reactions = message.reactions)
            }
        }
        }
    }
}

private fun cleanMessageText(input: String): String = input

@Composable
fun HavenPadComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
    isSending: Boolean,
    pendingFiles: List<PendingChatFile> = emptyList(),
    onRemovePending: (Int) -> Unit = {},
) {
    var attachMenuOpen by remember { mutableStateOf(false) }
    var emojiMenuOpen by remember { mutableStateOf(false) }

    val canSend = !isSending && (draft.isNotBlank() || pendingFiles.isNotEmpty())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HavenPadWebChatColors.SidebarBg)
            .border(BorderStroke(1.dp, HavenPadWebChatColors.CardBorder))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
    if (pendingFiles.isNotEmpty()) {
        PendingAttachmentRow(
            files = pendingFiles,
            onRemove = onRemovePending,
            contentColor = HavenPadWebChatColors.TextWhite,
            chipColor = HavenPadWebChatColors.CardBg,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { attachMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = HavenPadWebChatColors.TextGrey
                    )
                }
            }

            DropdownMenu(
                expanded = attachMenuOpen,
                onDismissRequest = { attachMenuOpen = false },
                modifier = Modifier.background(HavenPadWebChatColors.CardBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Photo", color = HavenPadWebChatColors.TextWhite) },
                    onClick = {
                        attachMenuOpen = false
                        onPickImages()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = HavenPadWebChatColors.EmeraldPrimary)
                    }
                )
                DropdownMenuItem(
                    text = { Text("PDF", color = HavenPadWebChatColors.TextWhite) },
                    onClick = {
                        attachMenuOpen = false
                        onPickPdfs()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = HavenPadWebChatColors.EmeraldPrimary)
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
                    text = if (pendingFiles.isEmpty()) "Say something..." else "Add a caption...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HavenPadWebChatColors.TextGrey
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            visualTransformation = emojiVisualTransformation(LocalEmojiFontFamily.current),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenPadWebChatColors.InputBg,
                unfocusedContainerColor = HavenPadWebChatColors.InputBg,
                focusedBorderColor = HavenPadWebChatColors.EmeraldPrimary,
                unfocusedBorderColor = HavenPadWebChatColors.InputBorder,
                focusedTextColor = HavenPadWebChatColors.TextWhite,
                unfocusedTextColor = HavenPadWebChatColors.TextWhite
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box {
            IconButton(onClick = { emojiMenuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfied,
                    contentDescription = "Emoji",
                    tint = HavenPadWebChatColors.TextGrey
                )
            }
            DropdownMenu(
                expanded = emojiMenuOpen,
                onDismissRequest = { emojiMenuOpen = false },
                modifier = Modifier.background(HavenPadWebChatColors.CardBg)
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
                .background(HavenPadWebChatColors.EmeraldAccent)
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    }
}

@Composable
internal fun ChatEmojiPicker(onPick: (String) -> Unit) {
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
