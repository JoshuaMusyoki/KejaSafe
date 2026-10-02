package com.rotd.havenpad.chat.components

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
import com.rotd.havenpad.chat.InboxPeerRow
import com.rotd.havenpad.chat.PeerConnectAction
import com.rotd.havenpad.components.HavenPadBottomSheet
import com.rotd.havenpad.components.HavenPadOutlinedButton
import com.rotd.havenpad.components.HavenPadPrimaryButton

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

    HavenPadBottomSheet(onDismissRequest = onDismiss) {
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
                    imageVector = Icons.Default.Person,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = connectionStatusLabel(action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val last = row.lastMessage
                if (last != null && last.senderId == myUserId) {
                    MessageReceiptTicks(status = last.status)
                }
            }

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
                    HavenPadPrimaryButton(
                        text = "Connect",
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PeerConnectAction.PENDING_OUTGOING -> {
                    HavenPadOutlinedButton(
                        text = "Request pending",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }
                PeerConnectAction.ACCEPT -> {
                    HavenPadPrimaryButton(
                        text = "Accept request",
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HavenPadOutlinedButton(
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
                        HavenPadOutlinedButton(
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
                        HavenPadPrimaryButton(
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
