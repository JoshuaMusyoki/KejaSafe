package com.rotd.havenpad.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.notify.ChatNotificationPermissionEffect
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** Listing + person currently open, so a notification is skipped while you are reading that chat. */
object OpenChat {
    var peerKey: String? = null
}

fun openChatKey(listingId: String, userId: String): String = "${listingId}_$userId"

/**
 * While this screen is in memory, keep checking for messages.
 * A background worker does the same check after the app is closed.
 */
@Composable
fun ChatIncomingNotifications() {
    val chatRepository = koinInject<ApartmentChatRepository>()
    ChatNotificationPermissionEffect()
    LaunchedEffect(Unit) {
        while (true) {
            if (SessionManager.currentUser.value?.id == null) {
                delay(2_000)
                continue
            }
            pullNewChatMessages(chatRepository)
            delay(8_000)
        }
    }
}

@Composable
fun TrackOpenChat(listingId: String?, userId: String?) {
    DisposableEffect(listingId, userId) {
        OpenChat.peerKey = if (listingId.isNullOrBlank() || userId.isNullOrBlank()) {
            null
        } else {
            openChatKey(listingId, userId)
        }
        onDispose {
            if (OpenChat.peerKey == openChatKey(listingId.orEmpty(), userId.orEmpty())) {
                OpenChat.peerKey = null
            }
        }
    }
}
