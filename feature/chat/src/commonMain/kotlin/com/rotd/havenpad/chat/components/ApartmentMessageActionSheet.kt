package com.rotd.havenpad.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.chat.ChatMessageText
import com.rotd.havenpad.domain.chat.ApartmentChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentMessageActionSheet(
    message: ApartmentChatMessage,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ChatQuickReactions.forEach { emoji ->
                ChatMessageText(
                    text = emoji,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .clickable {
                                onReact(emoji)
                                onDismiss()
                            }
                            .padding(6.dp),
                    )
                }
            }
            Text(
                text = message.previewText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (isMine) {
                ActionRow(
                    icon = Icons.Rounded.Delete,
                    label = "Delete for everyone",
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                )
            }
            if (onOpen != null) {
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = if (message.isPdf()) "Open PDF" else "Open",
                    onClick = {
                        onOpen()
                        onDismiss()
                    }
                )
            }
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.Reply,
                label = "Reply",
                onClick = {
                    onReply()
                    onDismiss()
                }
            )
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.Shortcut,
                label = "Forward",
                onClick = {
                    onForward()
                }
            )
            ActionRow(
                icon = Icons.Rounded.ContentCopy,
                label = "Copy",
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            ActionRow(
                icon = Icons.Rounded.PushPin,
                label = if (message.pinnedAt > 0L) "Unpin" else "Pin",
                onClick = {
                    onPin()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
