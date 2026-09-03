package com.rotd.kejasafe.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.chat.InboxPeerRow
import com.rotd.kejasafe.chat.PeerConnectAction
import com.rotd.kejasafe.components.KejaSafeBottomSheet
import com.rotd.kejasafe.components.KejaSafeOutlinedButton
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.domain.chat.formatUtcCalendarDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileBottomSheet(
    row: InboxPeerRow,
    myUserId: String?,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onMessage: () -> Unit,
    onCall: () -> Unit = {},
    callBusy: Boolean = false,
    callError: String? = null,
) {
    val peer = row.peer
    val action = row.actionFor(myUserId)

    KejaSafeBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when {
                        peer.peerIsLandlord -> Icons.Default.Home
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = peer.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (peer.peerIsLandlord) "Landlord" else "Tenant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = peer.listingTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (peer.joinedAtMillis > 0L) {
                Text(
                    text = "Joined ${formatJoinedDate(peer.joinedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = connectionStatusLabel(action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!callError.isNullOrBlank()) {
                Text(
                    text = callError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (action) {
                PeerConnectAction.CONNECT -> {
                    KejaSafePrimaryButton(
                        text = "Connect",
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PeerConnectAction.PENDING_OUTGOING -> {
                    KejaSafeOutlinedButton(
                        text = "Request pending",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }
                PeerConnectAction.ACCEPT -> {
                    KejaSafePrimaryButton(
                        text = "Accept request",
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KejaSafeOutlinedButton(
                        text = "Decline",
                        onClick = onReject,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PeerConnectAction.MESSAGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KejaSafeOutlinedButton(
                            text = if (callBusy) "Calling…" else "Call",
                            onClick = onCall,
                            modifier = Modifier.weight(1f),
                            enabled = !callBusy,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (callBusy) "Calling…" else "Call")
                        }
                        KejaSafePrimaryButton(
                            text = "Message",
                            onClick = onMessage,
                            modifier = Modifier.weight(1f),
                            enabled = !callBusy,
                        )
                    }
                }
                PeerConnectAction.NONE -> Unit
            }
        }
    }
}

@Composable
fun ChatPeopleSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

private fun connectionStatusLabel(action: PeerConnectAction): String = when (action) {
    PeerConnectAction.CONNECT -> "Not connected — send a request to chat"
    PeerConnectAction.PENDING_OUTGOING -> "Waiting for them to accept"
    PeerConnectAction.ACCEPT -> "They want to connect with you"
    PeerConnectAction.MESSAGE -> "Connected — message or start a live call"
    PeerConnectAction.NONE -> ""
}

/** Formats booking start as e.g. 13 Aug 2026 (UTC calendar day). */
internal fun formatJoinedDate(millis: Long): String = formatUtcCalendarDate(millis)
