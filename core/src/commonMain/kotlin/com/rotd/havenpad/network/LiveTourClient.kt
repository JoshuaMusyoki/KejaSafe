package com.rotd.havenpad.network

import com.rotd.havenpad.CallLogEntry
import com.rotd.havenpad.CreateLiveTourRequest
import com.rotd.havenpad.DirectLiveTourRequest
import com.rotd.havenpad.LiveKitConnectionDetails
import com.rotd.havenpad.LiveTourSession
import com.rotd.havenpad.RescheduleLiveTourRequest
import com.rotd.havenpad.RtcIceCandidate
import com.rotd.havenpad.RtcRoomSnapshot
import com.rotd.havenpad.RtcSessionDescription
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object LiveTourClient {
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
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

    private suspend fun httpFailure(
        response: io.ktor.client.statement.HttpResponse,
        fallback: String,
    ): Result<Nothing> {
        val raw = runCatching { response.bodyAsText() }.getOrNull().orEmpty().trim()
        val body = raw.removeSurrounding("\"").ifBlank { raw }
        return Result.failure(
            AppException.fromHttp(response.status.value, body, fallback)
        )
    }

    suspend fun createTour(listingId: String, request: CreateLiveTourRequest): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/$listingId/live-tours") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not request live tour")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun startDirectTour(
        listingId: String,
        peerUserId: String,
        mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO,
    ): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/$listingId/live-tours/direct") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(DirectLiveTourRequest(peerUserId = peerUserId, mediaType = mediaType))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not start live call")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getTour(sessionId: String): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/live-tours/$sessionId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not load live tour")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun incomingRing(): Result<CallLogEntry?> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/live-tours/incoming") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            val rows = response.body<List<CallLogEntry>>()
            Result.success(rows.firstOrNull())
        } else httpFailure(response, "Could not check incoming calls")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun hideCall(sessionId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/hide") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Could not delete that call")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun callHistory(): Result<List<CallLogEntry>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/live-tours/history") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not load calls")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun ring(sessionId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/ring") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Could not ring")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun markJoined(sessionId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/joined") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Could not answer call")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun myOpenTours(): Result<List<LiveTourSession>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/live-tours/mine") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not load live tours")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun confirmTour(sessionId: String): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/confirm") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not confirm live tour")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun rescheduleTour(sessionId: String, note: String): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/reschedule") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(RescheduleLiveTourRequest(preferredTimeNote = note))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not reschedule live tour")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun fetchToken(sessionId: String): Result<LiveKitConnectionDetails> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/token") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not get LiveKit token")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun rtcRoom(sessionId: String): Result<RtcRoomSnapshot> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/live-tours/$sessionId/rtc") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not load call")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun postOffer(sessionId: String, description: RtcSessionDescription): Result<Unit> =
        postSignal(sessionId, "offer", description)

    suspend fun postAnswer(sessionId: String, description: RtcSessionDescription): Result<Unit> =
        postSignal(sessionId, "answer", description)

    suspend fun postIce(sessionId: String, candidate: RtcIceCandidate): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/rtc/ice") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(candidate)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Could not send network candidate")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    private suspend fun postSignal(
        sessionId: String,
        kind: String,
        description: RtcSessionDescription,
    ): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/rtc/$kind") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(description)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Could not send $kind")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun endTour(sessionId: String): Result<LiveTourSession> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/live-tours/$sessionId/end") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Could not end live tour")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }
}
