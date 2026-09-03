package com.rotd.kejasafe.network

import com.rotd.kejasafe.CreateLiveTourRequest
import com.rotd.kejasafe.DirectLiveTourRequest
import com.rotd.kejasafe.LiveKitConnectionDetails
import com.rotd.kejasafe.LiveTourSession
import com.rotd.kejasafe.RescheduleLiveTourRequest
import com.rotd.kejasafe.LiveTourMediaType
import com.rotd.kejasafe.error.AppException
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
