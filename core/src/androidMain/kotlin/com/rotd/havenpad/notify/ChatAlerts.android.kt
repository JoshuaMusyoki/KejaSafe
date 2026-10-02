package com.rotd.havenpad.notify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.rotd.havenpad.storage.AndroidAppContext

actual fun requestChatNotificationPermission() = Unit

actual fun chatNotificationsAllowed(): Boolean {
    val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return false
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

actual fun showIncomingChatNotification(
    title: String,
    body: String,
    notificationKey: String,
    peerUserId: String,
    listingId: String,
    listingTitle: String,
    bookingId: String,
    displayName: String,
    peerIsLandlord: Boolean,
) {
    AndroidChatBridge.showNotification?.invoke(
        ChatNotifyRequest(
            title = title,
            body = body,
            notificationKey = notificationKey,
            peerUserId = peerUserId,
            listingId = listingId,
            listingTitle = listingTitle,
            bookingId = bookingId,
            displayName = displayName,
            peerIsLandlord = peerIsLandlord,
        )
    )
}

actual fun onSessionSaved() {
    AndroidChatBridge.onSessionSaved?.invoke()
}

actual fun onSessionCleared() {
    AndroidChatBridge.onSessionCleared?.invoke()
}

@Composable
actual fun ChatNotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
