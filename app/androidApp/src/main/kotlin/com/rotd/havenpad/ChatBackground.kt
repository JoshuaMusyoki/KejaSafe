package com.rotd.havenpad

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rotd.havenpad.chat.ChatNotificationInbox
import com.rotd.havenpad.chat.pullNewChatMessages
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.navigation.ChatLaunchRequest
import com.rotd.havenpad.navigation.HavenPadDestination
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.notify.AndroidChatBridge
import com.rotd.havenpad.notify.ChatNotifyRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

object ChatNotifyContract {
    const val ACTION_OPEN = "com.rotd.havenpad.action.OPEN_CHAT"
    const val ACTION_REPLY = "com.rotd.havenpad.action.REPLY_CHAT"
    const val EXTRA_MESSAGE_ID = "messageId"
    const val EXTRA_PEER = "peerUserId"
    const val EXTRA_LISTING = "listingId"
    const val EXTRA_LISTING_TITLE = "listingTitle"
    const val EXTRA_BOOKING = "bookingId"
    const val EXTRA_NAME = "displayName"
    const val EXTRA_LANDLORD = "peerIsLandlord"
    const val KEY_REPLY = "reply_text"
    const val CHANNEL_ID = "chat_messages"
}

object ChatBackgroundSync {
    private const val PERIODIC = "chat_message_sync"
    private const val SOON = "chat_message_sync_soon"

    fun install(context: Context) {
        val app = context.applicationContext
        AndroidChatBridge.showNotification = { request -> showChatNotification(app, request) }
        AndroidChatBridge.onSessionSaved = { schedule(app) }
        AndroidChatBridge.onSessionCleared = { cancel(app) }
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<ChatSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
        enqueueSoon(context)
    }

    fun enqueueSoon(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val soon = OneTimeWorkRequestBuilder<ChatSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SOON,
            ExistingWorkPolicy.KEEP,
            soon,
        )
    }

    fun cancel(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(PERIODIC)
        manager.cancelUniqueWork(SOON)
    }
}

class ChatSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val koin = KoinPlatform.getKoinOrNull() ?: return Result.retry()
        return try {
            pullNewChatMessages(koin.get<ApartmentChatRepository>())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

class ChatReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(ChatNotifyContract.KEY_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (reply.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SessionManager.restore()
                val koin = KoinPlatform.getKoinOrNull() ?: return@launch
                val repo = koin.get<ApartmentChatRepository>()
                repo.sendMessage(
                    listingId = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING).orEmpty(),
                    bookingId = intent.getStringExtra(ChatNotifyContract.EXTRA_BOOKING).orEmpty(),
                    peerUserId = intent.getStringExtra(ChatNotifyContract.EXTRA_PEER).orEmpty(),
                    content = reply,
                )
                val messageId = intent.getStringExtra(ChatNotifyContract.EXTRA_MESSAGE_ID)
                messageId?.let {
                    ChatNotificationInbox.markRead(it)
                    context.getSystemService(NotificationManager::class.java)?.cancel(it.hashCode())
                }
            } finally {
                pending.finish()
            }
        }
    }
}

fun openChatFromNotification(intent: Intent?) {
    val peerId = intent?.getStringExtra(ChatNotifyContract.EXTRA_PEER)?.takeIf { it.isNotBlank() } ?: return
    intent.getStringExtra(ChatNotifyContract.EXTRA_MESSAGE_ID)?.let(ChatNotificationInbox::markRead)
    ChatLaunchRequest.request(
        HavenPadDestination.ChatThread(
            peerUserId = peerId,
            listingId = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING).orEmpty(),
            listingTitle = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING_TITLE).orEmpty(),
            bookingId = intent.getStringExtra(ChatNotifyContract.EXTRA_BOOKING).orEmpty(),
            displayName = intent.getStringExtra(ChatNotifyContract.EXTRA_NAME).orEmpty(),
            peerIsLandlord = intent.getBooleanExtra(ChatNotifyContract.EXTRA_LANDLORD, false),
        )
    )
}

private fun showChatNotification(context: Context, request: ChatNotifyRequest) {
    if (request.peerUserId.isBlank()) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                ChatNotifyContract.CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }
    val openIntent = Intent(context, MainActivity::class.java).apply {
        action = ChatNotifyContract.ACTION_OPEN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        putChatExtras(request)
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        request.notificationKey.hashCode(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val replyIntent = Intent(context, ChatReplyReceiver::class.java).apply {
        action = ChatNotifyContract.ACTION_REPLY
        putChatExtras(request)
    }
    val replyPending = PendingIntent.getBroadcast(
        context,
        request.notificationKey.hashCode(),
        replyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    val remoteInput = RemoteInput.Builder(ChatNotifyContract.KEY_REPLY)
        .setLabel("Reply")
        .build()
    val replyAction = NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_send,
        "Reply",
        replyPending,
    ).addRemoteInput(remoteInput).build()
    val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.haven_pad_logo)
    val notification = NotificationCompat.Builder(context, ChatNotifyContract.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_message)
        .setLargeIcon(largeIcon)
        .setContentTitle(request.title)
        .setContentText(request.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(request.body))
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .addAction(replyAction)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    manager.notify(request.notificationKey.hashCode(), notification)
}

private fun Intent.putChatExtras(request: ChatNotifyRequest) {
    putExtra(ChatNotifyContract.EXTRA_MESSAGE_ID, request.notificationKey)
    putExtra(ChatNotifyContract.EXTRA_PEER, request.peerUserId)
    putExtra(ChatNotifyContract.EXTRA_LISTING, request.listingId)
    putExtra(ChatNotifyContract.EXTRA_LISTING_TITLE, request.listingTitle)
    putExtra(ChatNotifyContract.EXTRA_BOOKING, request.bookingId)
    putExtra(ChatNotifyContract.EXTRA_NAME, request.displayName)
    putExtra(ChatNotifyContract.EXTRA_LANDLORD, request.peerIsLandlord)
}
