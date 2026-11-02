package com.rotd.havenpad.network

import com.rotd.havenpad.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

object EscrowClient {
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
        install(WebSockets)
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun authHeader(): String? =
        SessionManager.token.value?.let { "Bearer $it" }

    suspend fun initiatePayment(request: InitiatePaymentRequest): Result<PaymentStatusResponse> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/escrow/initiate") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Initiate payment")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getStatus(transactionId: String): Result<PaymentStatusResponse> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/escrow/status/$transactionId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Get escrow status")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun releaseEscrow(request: ReleaseEscrowRequest): Result<PaymentStatusResponse> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/escrow/release") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Release escrow")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun disputeEscrow(request: DisputeEscrowRequest): Result<PaymentStatusResponse> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/escrow/dispute") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Dispute escrow")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Connects to the WebSocket for real-time status updates.
     */
    fun observeStatus(transactionId: String): Flow<PaymentStatusResponse> = flow {
        val token = SessionManager.token.value ?: return@flow
        val wsBase = AuthClient.BASE_URL.replace("http", "ws")
        
        try {
            client.webSocket("$wsBase/escrow/updates/$transactionId", request = {
                header(HttpHeaders.Authorization, "Bearer $token")
            }) {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        emit(json.decodeFromString<PaymentStatusResponse>(text))
                    }
                }
            }
        } catch (e: Exception) {
            // Log or handle error — prevents UI crash on handshake failure
            println("Escrow WS Error: ${e.message}")
        }
    }
}
