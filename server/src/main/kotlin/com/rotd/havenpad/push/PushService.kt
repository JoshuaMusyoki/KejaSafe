package com.rotd.havenpad.push

import com.google.auth.oauth2.GoogleCredentials
import com.rotd.havenpad.LiveTourSession
import com.rotd.havenpad.RegisterDeviceRequest
import com.rotd.havenpad.RentBill
import com.rotd.havenpad.admin.AccountEventLog
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.db.DeviceTokensTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.UsersTable
import com.rotd.havenpad.domain.chat.ApartmentChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import kotlin.concurrent.thread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Sends a notification the phone can show after HavenPad has been closed.
 * The payload includes both a notification (system display) and data (opened on tap).
 */
object PushService {
    private const val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val http = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
    }
    private var warnedMissingCredentials = false

    fun register(userId: String, request: RegisterDeviceRequest) {
        val token = request.token.trim()
        if (userId.isBlank() || token.isBlank()) return
        val platform = request.platform.trim().ifBlank { "android" }
        val now = System.currentTimeMillis()
        val event = transaction {
            val existing = DeviceTokensTable.selectAll()
                .where { DeviceTokensTable.token eq token }
                .singleOrNull()
            val otherDevices = DeviceTokensTable.selectAll()
                .where { (DeviceTokensTable.userId eq userId) and (DeviceTokensTable.token neq token) }
                .count()
            val label = platform.replaceFirstChar { it.titlecase() }
            if (existing == null) {
                DeviceTokensTable.insert {
                    it[DeviceTokensTable.token] = token
                    it[DeviceTokensTable.userId] = userId
                    it[DeviceTokensTable.platform] = platform
                    it[updatedAt] = now
                }
                val title = if (otherDevices > 0) "Added a device" else "Signed in on a device"
                userId to (title to label)
            } else {
                val previousUser = existing[DeviceTokensTable.userId]
                DeviceTokensTable.update({ DeviceTokensTable.token eq token }) {
                    it[DeviceTokensTable.userId] = userId
                    it[DeviceTokensTable.platform] = platform
                    it[updatedAt] = now
                }
                if (previousUser != userId) {
                    userId to ("Changed device" to "This $label device moved onto the account")
                } else {
                    null
                }
            }
        }
        if (event != null) {
            AccountEventLog.record(event.first, event.second.first, event.second.second)
        }
    }

    fun unregister(userId: String, token: String) {
        val trimmed = token.trim()
        if (userId.isBlank() || trimmed.isBlank()) return
        transaction {
            DeviceTokensTable.deleteWhere {
                (DeviceTokensTable.token eq trimmed) and (DeviceTokensTable.userId eq userId)
            }
        }
    }

    fun notifyChat(message: ApartmentChatMessage) {
        thread(name = "fcm-chat", isDaemon = true) {
            runCatching { deliverChat(message) }
                .onFailure { println("FCM chat push failed: ${it.message}") }
        }
    }

    fun notifySignIn(userId: String, deviceName: String, challengeId: String) {
        thread(name = "fcm-signin", isDaemon = true) {
            runCatching {
                send(
                    userId,
                    ChatPush(
                        title = "New sign-in",
                        body = "$deviceName is trying to sign in. Open HavenPad to allow or block it.",
                        channelId = "chat_messages",
                        data = mapOf(
                            "kind" to "signin",
                            "challengeId" to challengeId,
                            "deviceName" to deviceName,
                            "peerUserId" to userId,
                            "messageId" to challengeId,
                            "listingId" to "",
                            "listingTitle" to "",
                            "bookingId" to "",
                            "displayName" to deviceName,
                            "peerIsLandlord" to "false",
                        ),
                    ),
                )
            }.onFailure { println("FCM sign-in push failed: ${it.message}") }
        }
    }

    fun notifyTour(session: LiveTourSession) {
        thread(name = "fcm-tour", isDaemon = true) {
            runCatching { deliverTour(session) }
                .onFailure { println("FCM tour push failed: ${it.message}") }
        }
    }

    fun notifyBill(bill: RentBill, title: String = "New rent bill issued", body: String = "") {
        println("PushService.notifyBill triggered for billId=${bill.id}, userId=${bill.userId}")
        thread(name = "fcm-bill", isDaemon = true) {
            runCatching {
                send(
                    bill.userId,
                    ChatPush(
                        title = title,
                        body = body.ifBlank { "Rent bill of KES ${bill.totalAmount.toInt()} for ${bill.propertyTitle.ifBlank { "your property" }} is ready." },
                        channelId = "chat_messages",
                        data = mapOf(
                            "kind" to "bill",
                            "billId" to bill.id,
                            "propertyId" to bill.propertyId,
                            "propertyTitle" to bill.propertyTitle,
                            "messageId" to bill.id,
                            "peerUserId" to bill.userId,
                            "listingId" to bill.propertyId,
                            "listingTitle" to bill.propertyTitle,
                            "bookingId" to bill.bookingId.orEmpty(),
                            "displayName" to "HavenPad Billing",
                            "peerIsLandlord" to "false",
                        ),
                    ),
                )
            }.onFailure { println("FCM bill push failed: ${it.message}") }
        }
    }

    private fun deliverChat(message: ApartmentChatMessage) {
        val meta = transaction {
            val sender = UsersTable.selectAll()
                .where { UsersTable.id eq message.senderId }
                .singleOrNull()
            val listing = ListingsTable.selectAll()
                .where { ListingsTable.id eq message.listingId }
                .singleOrNull()
            val name = sender?.get(UsersTable.fullName)?.trim().orEmpty().ifBlank { "New message" }
            val listingTitle = listing?.get(ListingsTable.title).orEmpty()
            val landlordId = listing?.get(ListingsTable.landlordId).orEmpty()
            ChatPush(
                title = name,
                body = when {
                    message.type.equals(ApartmentChatMessage.TYPE_IMAGE, true) -> "Photo"
                    message.type.equals(ApartmentChatMessage.TYPE_PDF, true) ->
                        message.fileName?.trim()?.ifBlank { null } ?: "PDF"
                    else -> message.content
                }.take(180),
                data = mapOf(
                    "kind" to "chat",
                    "messageId" to message.id,
                    "peerUserId" to message.senderId,
                    "listingId" to message.listingId,
                    "listingTitle" to listingTitle,
                    "bookingId" to message.bookingId,
                    "displayName" to name,
                    "peerIsLandlord" to (message.senderId == landlordId).toString(),
                ),
                channelId = "chat_messages",
            )
        }
        send(message.receiverId, meta)
    }

    private fun deliverTour(session: LiveTourSession) {
        val calleeId = when (session.callerId) {
            session.landlordId -> session.tenantId
            session.tenantId -> session.landlordId
            else -> return
        }
        val callerName = when (session.callerId) {
            session.landlordId -> session.landlordName
            else -> session.tenantName
        }.trim().ifBlank { "House tour" }
        val listingTitle = session.listingTitle
        send(
            calleeId,
            ChatPush(
                title = callerName,
                body = if (listingTitle.isBlank()) "Incoming house tour" else "Incoming house tour · $listingTitle",
                channelId = "house_tours",
                data = mapOf(
                    "kind" to "tour",
                    "tourSessionId" to session.id,
                    "peerUserId" to session.callerId,
                    "listingId" to session.listingId,
                    "listingTitle" to listingTitle,
                    "displayName" to callerName,
                    "bookingId" to "",
                    "messageId" to session.id,
                    "peerIsLandlord" to (session.callerId == session.landlordId).toString(),
                ),
            ),
        )
    }

    private fun send(userId: String, push: ChatPush) {
        val tokens = transaction {
            DeviceTokensTable.selectAll()
                .where { DeviceTokensTable.userId eq userId }
                .map { it[DeviceTokensTable.token] }
        }
        println("PushService.send for userId=$userId found ${tokens.size} tokens (kind=${push.data["kind"]})")
        if (tokens.isEmpty()) return
        val access = accessToken() ?: run {
            return
        }
        val url = "https://fcm.googleapis.com/v1/projects/${ServerConfig.fcmProjectId}/messages:send"
        tokens.forEach { token ->
            val response = kotlinx.coroutines.runBlocking {
                http.post(url) {
                    header(HttpHeaders.Authorization, "Bearer $access")
                    contentType(ContentType.Application.Json)
                    setBody(
                        FcmRequest(
                            message = FcmMessage(
                                token = token,
                                notification = FcmNotification(title = push.title, body = push.body),
                                data = push.data + mapOf("title" to push.title, "body" to push.body),
                            ),
                        ),
                    )
                }
            }
            if (response.status.isSuccess()) {
                println("PushService.send FCM SUCCESS for token ${token.take(12)}... kind=${push.data["kind"]}")
                return@forEach
            }
            val body = runCatching { kotlinx.coroutines.runBlocking { response.bodyAsText() } }.getOrNull().orEmpty()
            println("FCM ${response.status.value} for $userId: ${body.take(300)}")
            if (response.status.value == 404 || body.contains("UNREGISTERED")) {
                transaction {
                    DeviceTokensTable.deleteWhere { DeviceTokensTable.token eq token }
                }
            }
        }
    }

    private fun accessToken(): String? {
        val path = ServerConfig.fcmCredentialsPath
        val file = path?.let { File(it) }?.takeIf { it.isFile }
        if (file == null) {
            if (!warnedMissingCredentials) {
                warnedMissingCredentials = true
                println(
                    "FCM skipped: save a Firebase service account as firebase-service-account.json " +
                        "or set HAVENPAD_FCM_CREDENTIALS. Chat still saves."
                )
            }
            return null
        }
        val credentials = GoogleCredentials.fromStream(file.inputStream())
            .createScoped(listOf(MESSAGING_SCOPE))
        credentials.refreshIfExpired()
        return credentials.accessToken?.tokenValue
    }
}

private data class ChatPush(
    val title: String,
    val body: String,
    val channelId: String,
    val data: Map<String, String>,
)

@Serializable
private data class FcmRequest(val message: FcmMessage)

@Serializable
private data class FcmMessage(
    val token: String,
    val notification: FcmNotification,
    val data: Map<String, String>,
)

@Serializable
private data class FcmNotification(val title: String, val body: String)
