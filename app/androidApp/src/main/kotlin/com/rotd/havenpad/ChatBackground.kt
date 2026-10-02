package com.rotd.havenpad

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
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
import com.rotd.havenpad.chat.pullIncomingCall
import com.rotd.havenpad.chat.pullNewChatMessages
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.navigation.ChatLaunchRequest
import com.rotd.havenpad.navigation.HavenPadDestination
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.notify.AndroidChatBridge
import com.rotd.havenpad.notify.ChatNotifyRequest
import com.rotd.havenpad.notify.HouseTourNotify
import com.rotd.havenpad.storage.AndroidActivityHolder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    const val CHANNEL_TOURS = "house_tours"
    const val CHANNEL_PRESENCE = "havenpad_listening"
    const val EXTRA_SESSION = "tourSessionId"
    const val PRESENCE_ID = 42
}

object ChatBackgroundSync {
    private const val PERIODIC = "chat_message_sync"
    private const val SOON = "chat_message_sync_soon"

    fun install(context: Context) {
        val app = context.applicationContext
        AndroidChatBridge.showNotification = { request -> showChatNotification(app, request) }
        AndroidChatBridge.showHouseTour = { request -> showHouseTourNotification(app, request) }
        AndroidChatBridge.cancelHouseTour = { sessionId ->
            app.getSystemService(NotificationManager::class.java)?.cancel(sessionId.hashCode())
        }
        AndroidChatBridge.onSessionSaved = {
            schedule(app)
            startAlerts(app)
        }
        AndroidChatBridge.onSessionCleared = {
            cancel(app)
            stopAlerts(app)
        }
    }

    fun startAlerts(context: Context) {
        val intent = Intent(context, HavenPadAlertService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun stopAlerts(context: Context) {
        runCatching {
            context.stopService(Intent(context, HavenPadAlertService::class.java))
        }
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
            .setInitialDelay(20, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SOON,
            ExistingWorkPolicy.REPLACE,
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
            pullIncomingCall()
            ChatBackgroundSync.enqueueSoon(applicationContext)
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

fun openTourFromNotification(intent: Intent?) {
    val sessionId = intent?.getStringExtra(ChatNotifyContract.EXTRA_SESSION)?.takeIf { it.isNotBlank() }
        ?: return
    com.rotd.havenpad.navigation.TourLaunchRequest.request(sessionId)
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
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()
    manager.notify(request.notificationKey.hashCode(), notification)
}

private fun showHouseTourNotification(context: Context, request: HouseTourNotify) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ChatNotifyContract.CHANNEL_TOURS,
            "House tours",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Incoming house tour calls"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
    val open = Intent(context, IncomingTourActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(ChatNotifyContract.EXTRA_SESSION, request.sessionId)
        putExtra(ChatNotifyContract.EXTRA_NAME, request.callerName)
        putExtra(ChatNotifyContract.EXTRA_LISTING_TITLE, request.listingTitle)
        putExtra(ChatNotifyContract.EXTRA_PEER, request.peerUserId)
        putExtra(ChatNotifyContract.EXTRA_LISTING, request.listingId)
        putExtra(ChatNotifyContract.EXTRA_LANDLORD, request.peerIsLandlord)
    }
    val fullScreen = PendingIntent.getActivity(
        context,
        request.sessionId.hashCode(),
        open,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, ChatNotifyContract.CHANNEL_TOURS)
        .setSmallIcon(R.drawable.ic_stat_message)
        .setContentTitle(request.callerName.ifBlank { "House tour" })
        .setContentText(
            "Incoming house tour" +
                if (request.listingTitle.isBlank()) "" else " · ${request.listingTitle}"
        )
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setFullScreenIntent(fullScreen, true)
        .setContentIntent(fullScreen)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()
    manager.notify(request.sessionId.hashCode(), notification)
    if (AndroidActivityHolder.current != null) {
        runCatching { context.startActivity(open) }
    }
}

private fun presenceNotification(context: Context): android.app.Notification {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
        manager.createNotificationChannel(
            NotificationChannel(
                ChatNotifyContract.CHANNEL_PRESENCE,
                "Message listener",
                NotificationManager.IMPORTANCE_MIN,
            )
        )
    }
    return NotificationCompat.Builder(context, ChatNotifyContract.CHANNEL_PRESENCE)
        .setSmallIcon(R.drawable.ic_stat_message)
        .setContentTitle("HavenPad")
        .setContentText("Listening for messages and house tours")
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build()
}

class HavenPadAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = presenceNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    ChatNotifyContract.PRESENCE_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(ChatNotifyContract.PRESENCE_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive != true) {
            job = scope.launch {
                while (isActive) {
                    if (!SessionManager.isLogged()) {
                        withContext(Dispatchers.Main) { stopAlertsQuietly() }
                        break
                    }
                    val koin = KoinPlatform.getKoinOrNull()
                    if (koin != null) {
                        runCatching { pullIncomingCall() }
                        runCatching { pullNewChatMessages(koin.get<ApartmentChatRepository>()) }
                    }
                    delay(2_000)
                }
            }
        }
        return START_STICKY
    }

    private fun stopAlertsQuietly() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
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
