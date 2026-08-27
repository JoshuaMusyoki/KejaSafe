package com.rotd.kejasafe.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
                    KejaSafePrimaryButton(
                        text = "Message",
                        onClick = onMessage,
                        modifier = Modifier.fillMaxWidth()
                    )
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
    PeerConnectAction.MESSAGE -> "Connected — you can message"
    PeerConnectAction.NONE -> ""
}

/** Formats booking start as e.g. 13 Aug 2026 (UTC calendar day). */
internal fun formatJoinedDate(millis: Long): String {
    if (millis <= 0L) return ""
    val days = millis / 86_400_000L
    var z = days + 719_468L
    val era = if (z >= 0) z / 146_097L else (z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146_096L) / 365L
    var year = (yoe + era * 400L).toInt()
    val doy = (doe - (365L * yoe + yoe / 4L - yoe / 100L)).toInt()
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    if (month <= 2) year += 1
    val monthName = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
    }
    return "$day $monthName $year"
}
