package com.rotd.havenpad.network

import com.rotd.havenpad.PinChatMessageRequest
import com.rotd.havenpad.ReactToChatMessageRequest
import com.rotd.havenpad.RegisterDeviceRequest
import com.rotd.havenpad.SendApartmentChatMessageRequest
import com.rotd.havenpad.UpsertChatConnectionRequest
import com.rotd.havenpad.domain.chat.ApartmentChatMessage
import com.rotd.havenpad.domain.chat.Connection
import com.rotd.havenpad.domain.chat.ConnectionStatus
import com.rotd.havenpad.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object ChatClient {
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private fun authHeader(): String? =
        SessionManager.token.value?.let { "Bearer $it" }

    private fun notSignedIn(): Result<Nothing> =
        Result.failure(AppException.notSignedIn())

    private suspend fun httpFailure(response: io.ktor.client.statement.HttpResponse, fallback: String): Result<Nothing> {
        val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
        val message = body.ifBlank { "$fallback (${response.status})" }
        return Result.failure(AppException.from(Exception(message)))
    }

    suspend fun listConnections(): Result<List<Connection>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/chat/connections") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to load connections")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun upsertConnection(
        peerUserId: String,
        status: ConnectionStatus,
    ): Result<Connection> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/connections") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(UpsertChatConnectionRequest(peerUserId = peerUserId, status = status))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to update connection")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun listMessages(
        listingId: String,
        peerUserId: String,
        afterTimestamp: Long = 0L,
    ): Result<List<ApartmentChatMessage>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/chat/messages") {
            header(HttpHeaders.Authorization, token)
            parameter("listingId", listingId)
            parameter("peerUserId", peerUserId)
            if (afterTimestamp > 0L) parameter("after", afterTimestamp)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to load messages")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun inquiryPeers(): Result<List<com.rotd.havenpad.domain.chat.ApartmentChatPeer>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/chat/inquiries") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to load landlord chats")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun sendMessage(
        request: SendApartmentChatMessageRequest,
    ): Result<ApartmentChatMessage> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/messages") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to send message")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun markDelivered(listingId: String, peerUserId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/messages/delivered") {
            header(HttpHeaders.Authorization, token)
            parameter("listingId", listingId)
            parameter("peerUserId", peerUserId)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Failed to mark delivered")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun markRead(listingId: String, peerUserId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/messages/read") {
            header(HttpHeaders.Authorization, token)
            parameter("listingId", listingId)
            parameter("peerUserId", peerUserId)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Failed to mark read")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun reactToMessage(messageId: String, emoji: String): Result<ApartmentChatMessage> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/messages/$messageId/react") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(ReactToChatMessageRequest(emoji))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to react")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun pinMessage(messageId: String, pinned: Boolean): Result<ApartmentChatMessage> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/chat/messages/$messageId/pin") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(PinChatMessageRequest(pinned))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to pin message")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun deleteMessage(messageId: String): Result<ApartmentChatMessage> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.delete("${AuthClient.BASE_URL}/chat/messages/$messageId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Failed to delete message")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun registerDevice(token: String, platform: String = "android"): Result<Unit> = try {
        val auth = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/devices") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceRequest(token = token, platform = platform))
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Failed to register this phone")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun unregisterDevice(token: String, authorization: String): Result<Unit> = try {
        val response = client.post("${AuthClient.BASE_URL}/devices/remove") {
            header(HttpHeaders.Authorization, authorization)
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceRequest(token = token))
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Failed to remove this phone")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun downloadAttachment(attachmentId: String): Result<ByteArray> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/chat/attachments/$attachmentId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.bodyAsBytes())
        else httpFailure(response, "Failed to download attachment")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }
}
