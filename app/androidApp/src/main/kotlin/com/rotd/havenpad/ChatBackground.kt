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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rotd.havenpad.BillStatus
import com.rotd.havenpad.chat.AppVisibility
import com.rotd.havenpad.chat.ChatNotificationInbox
import com.rotd.havenpad.chat.OpenChat
import com.rotd.havenpad.chat.openChatKey
import com.rotd.havenpad.chat.pullIncomingCall
import com.rotd.havenpad.chat.pullNewChatMessages
import com.rotd.havenpad.network.ChatClient
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
    const val CHANNEL_BILLS = "rent_bills"
    const val EXTRA_SESSION = "tourSessionId"
    const val EXTRA_BILL_ID = "billId"
}

object ChatBackgroundSync {
    private const val PERIODIC = "chat_message_sync"
    private const val SOON = "chat_message_sync_soon"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenJob: Job? = null

    fun install(context: Context) {
        val app = context.applicationContext
        app.getSystemService(NotificationManager::class.java)?.cancel(42)
        AndroidChatBridge.showNotification = { request -> showChatNotification(app, request) }
        AndroidChatBridge.showHouseTour = { request -> showHouseTourNotification(app, request) }
        AndroidChatBridge.cancelHouseTour = { sessionId ->
            app.getSystemService(NotificationManager::class.java)?.cancel(sessionId.hashCode())
        }
        ensureChannels(app)
        AndroidChatBridge.onSessionSaved = {
            schedule(app)
            startListening()
            registerPushToken()
        }
        AndroidChatBridge.onSessionClearing = { unregisterPushToken() }
        AndroidChatBridge.onSessionCleared = {
            cancel(app)
            stopListening()
        }
    }

    fun startListening() {
        if (listenJob?.isActive == true) return
        listenJob = scope.launch {
            while (isActive) {
                if (!SessionManager.isLogged()) break
                val koin = KoinPlatform.getKoinOrNull()
                if (koin != null) {
                    runCatching { pullIncomingCall() }
                    runCatching { pullNewChatMessages(koin.get<ApartmentChatRepository>()) }
                }
                delay(4_000)
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    fun registerPushToken() {
        if (!SessionManager.isLogged()) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result?.takeIf { it.isNotBlank() } ?: return@addOnCompleteListener
            scope.launch { ChatClient.registerDevice(token) }
        }
    }

    fun unregisterPushToken() {
        val bearer = SessionManager.token.value ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result?.takeIf { it.isNotBlank() } ?: return@addOnCompleteListener
            scope.launch { ChatClient.unregisterDevice(token, "Bearer $bearer") }
        }
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
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
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val soon = OneTimeWorkRequestBuilder<ChatSyncWorker>()
            .setConstraints(constraints)
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
            SessionManager.restore()
            if (!SessionManager.isLogged()) return Result.success()
            pullNewChatMessages(koin.get<ApartmentChatRepository>())
            pullIncomingCall()
            runCatching {
                val rentRepo = koin.get<com.rotd.havenpad.data.RentRepository>()
                val result = rentRepo.getBills()
                result.onSuccess { wrapper ->
                    val bills = wrapper.value
                    val prefs = applicationContext.getSharedPreferences("havenpad_bills_sync", Context.MODE_PRIVATE)
                    bills.forEach { bill ->
                        val lastTotal = prefs.getFloat("bill_total_${bill.id}", -1f)
                        val currentTotal = bill.totalAmount.toFloat()
                        if (lastTotal == -1f) {
                            prefs.edit().putFloat("bill_total_${bill.id}", currentTotal).apply()
                        } else if (lastTotal != currentTotal) {
                            prefs.edit().putFloat("bill_total_${bill.id}", currentTotal).apply()
                            if (bill.status != BillStatus.PAID) {
                                showBillNotification(
                                    applicationContext,
                                    BillNotifyRequest(
                                        title = "Rent bill updated",
                                        body = "Your invoice for ${bill.propertyTitle.ifBlank { "your property" }} has been updated. Total: KES ${bill.totalAmount.toInt()}.",
                                        billId = bill.id,
                                    )
                                )
                            }
                        }
                    }
                }
            }
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

fun openBillFromNotification(intent: Intent?) {
    val billId = intent?.getStringExtra(ChatNotifyContract.EXTRA_BILL_ID)?.takeIf { it.isNotBlank() } ?: return
    com.rotd.havenpad.navigation.BillLaunchRequest.request(billId)
}

data class BillNotifyRequest(
    val title: String,
    val body: String,
    val billId: String,
)

private fun showBillNotification(context: Context, request: BillNotifyRequest) {
    ChatNotificationInbox.recordNotice(
        id = request.billId,
        title = request.title,
        body = request.body,
    )
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                ChatNotifyContract.CHANNEL_BILLS,
                "Rent & Bills",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Rent bills and utility updates"
            }
        )
    }
    val openIntent = Intent(context, MainActivity::class.java).apply {
        action = ChatNotifyContract.ACTION_OPEN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(ChatNotifyContract.EXTRA_BILL_ID, request.billId)
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        request.billId.hashCode(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.haven_pad_logo)
    val notification = NotificationCompat.Builder(context, ChatNotifyContract.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_message)
        .setLargeIcon(largeIcon)
        .setContentTitle(request.title)
        .setContentText(request.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(request.body))
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()
    manager.notify(request.billId.hashCode(), notification)
}

fun openChatFromNotification(intent: Intent?) {
    val peerId = intent?.getStringExtra(ChatNotifyContract.EXTRA_PEER)?.takeIf { it.isNotBlank() } ?: return
    intent.getStringExtra(ChatNotifyContract.EXTRA_MESSAGE_ID)?.let { id ->
        ChatNotificationInbox.markSeen(listOf(id))
        ChatNotificationInbox.markRead(id)
    }
    ChatLaunchRequest.request(
        HavenPadDestination.ChatThread(
            peerUserId = peerId,
            listingId = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING).orEmpty(),
            listingTitle = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING_TITLE).orEmpty(),
            bookingId = intent.getStringExtra(ChatNotifyContract.EXTRA_BOOKING).orEmpty(),
            displayName = intent.getStringExtra(ChatNotifyContract.EXTRA_NAME).orEmpty(),
            peerIsLandlord = intent.truthyExtra(ChatNotifyContract.EXTRA_LANDLORD),
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

private fun Intent.putChatExtras(request: ChatNotifyRequest) {
    putExtra(ChatNotifyContract.EXTRA_MESSAGE_ID, request.notificationKey)
    putExtra(ChatNotifyContract.EXTRA_PEER, request.peerUserId)
    putExtra(ChatNotifyContract.EXTRA_LISTING, request.listingId)
    putExtra(ChatNotifyContract.EXTRA_LISTING_TITLE, request.listingTitle)
    putExtra(ChatNotifyContract.EXTRA_BOOKING, request.bookingId)
    putExtra(ChatNotifyContract.EXTRA_NAME, request.displayName)
    putExtra(ChatNotifyContract.EXTRA_LANDLORD, request.peerIsLandlord)
}

private fun Intent.truthyExtra(name: String): Boolean {
    val raw = extras?.get(name) ?: return false
    return when (raw) {
        is Boolean -> raw
        else -> raw.toString().equals("true", ignoreCase = true)
    }
}

private fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
        NotificationChannel(
            ChatNotifyContract.CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Chat messages"
            enableVibration(true)
        },
    )
    manager.createNotificationChannel(
        NotificationChannel(
            ChatNotifyContract.CHANNEL_TOURS,
            "House tours",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Incoming house tours"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        },
    )
    manager.createNotificationChannel(
        NotificationChannel(
            ChatNotifyContract.CHANNEL_BILLS,
            "Rent & Bills",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Rent bills and utility updates"
        },
    )
}

/**
 * Foreground delivery. When HavenPad is closed, Play Services shows the
 * notification payload itself and this service is not started.
 */
class HavenPadMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (!SessionManager.isLogged()) return
        CoroutineScope(Dispatchers.IO).launch { ChatClient.registerDevice(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["kind"] == "signin") {
            val challengeId = data["challengeId"].orEmpty()
            val deviceName = data["deviceName"].orEmpty().ifBlank { "Another device" }
            if (challengeId.isNotBlank()) {
                com.rotd.havenpad.auth.SignInPromptBus.offer(
                    com.rotd.havenpad.SignInPrompt(challengeId = challengeId, deviceName = deviceName)
                )
            }
            return
        }
        if (data["kind"] == "bill") {
            showBillNotification(
                this,
                BillNotifyRequest(
                    title = message.notification?.title ?: "Rent bill updated",
                    body = message.notification?.body.orEmpty(),
                    billId = data["billId"].orEmpty(),
                )
            )
            return
        }
        if (data["kind"] == "tour") {
            val sessionId = data["tourSessionId"].orEmpty()
            if (sessionId.isBlank()) return
            showHouseTourNotification(
                this,
                HouseTourNotify(
                    callerName = data["displayName"].orEmpty(),
                    listingTitle = data["listingTitle"].orEmpty(),
                    sessionId = sessionId,
                    peerUserId = data["peerUserId"].orEmpty(),
                    listingId = data["listingId"].orEmpty(),
                    peerIsLandlord = data["peerIsLandlord"].equals("true", ignoreCase = true),
                ),
            )
            return
        }
        val peerId = data["peerUserId"].orEmpty()
        if (peerId.isBlank()) return
        val messageId = data["messageId"].orEmpty().ifBlank { message.messageId.orEmpty() }
        if (messageId.isNotBlank()) ChatNotificationInbox.markSeen(listOf(messageId))
        val reading = AppVisibility.inForeground &&
            OpenChat.peerKey == openChatKey(data["listingId"].orEmpty(), peerId)
        if (reading) return
        val title = message.notification?.title
            ?: data["displayName"].orEmpty().ifBlank { "HavenPad" }
        val body = message.notification?.body.orEmpty()
        showChatNotification(
            this,
            ChatNotifyRequest(
                title = title,
                body = body,
                notificationKey = messageId.ifBlank { peerId },
                peerUserId = peerId,
                listingId = data["listingId"].orEmpty(),
                listingTitle = data["listingTitle"].orEmpty(),
                bookingId = data["bookingId"].orEmpty(),
                displayName = data["displayName"].orEmpty().ifBlank { title },
                peerIsLandlord = data["peerIsLandlord"].equals("true", ignoreCase = true),
            ),
        )
    }
}
