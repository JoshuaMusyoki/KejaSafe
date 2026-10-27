package com.rotd.havenpad.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.components.UserPhoto
import com.rotd.havenpad.components.photoInitials
import com.rotd.havenpad.chat.InboxImageThumb
import com.rotd.havenpad.chat.InboxPeerRow
import com.rotd.havenpad.chat.PeerConnectAction
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews

@Composable
fun PeerRow(
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
        UserPhoto(
            imageUrl = peer.profilePictureUrl,
            fallback = photoInitials(peer.displayName),
            modifier = Modifier.size(44.dp).clickable(onClick = onProfileClick),
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                        else -> "Tap to chat"
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
    }
}

@ScreenPreviews
@Composable
internal fun PeerRowPreview() {
    val mockPeer = InboxPeerRow(
        peer = ApartmentChatPeer(
            userId = "landlord_101",
            displayName = "Kevin Mwangi",
            listingTitle = "Sunview Heights - Unit 3B",
            peerIsLandlord = true,
            joinedAtMillis = 1726752000000L,
            listingId = "listing_123",
            bookingId = "booking_123",
        ),
        lastMessage = null,
    )

    HavenPadTheme {
        PeerRow(
            row = mockPeer,
            myUserId = "user_123",
            showConnectActions = true,
            onClick = {},
            onProfileClick = {},
            onConnect = {},
            onAccept = {},
            loadAttachment = { null }
        )
    }
}