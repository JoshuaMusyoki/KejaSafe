package com.rotd.havenpad.network

import com.rotd.havenpad.domain.listing.InitiatePropertyRequestPayment
import com.rotd.havenpad.domain.listing.PropertyRequest
import com.rotd.havenpad.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object PropertyRequestClient {
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private fun authHeader(): String? =
        SessionManager.token.value?.let { "Bearer $it" }

    suspend fun submitRequest(phoneNumber: String, request: PropertyRequest): Result<String> = try {
        val token = authHeader() ?: return Result.failure(AppException.notSignedIn())
        val body = InitiatePropertyRequestPayment(phoneNumber, request)
        val response = client.post("${AuthClient.BASE_URL}/property-requests") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            val map = response.body<Map<String, String>>()
            Result.success(map["requestId"] ?: "")
        } else {
            Result.failure(Exception("Submission failed: ${response.status}"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
