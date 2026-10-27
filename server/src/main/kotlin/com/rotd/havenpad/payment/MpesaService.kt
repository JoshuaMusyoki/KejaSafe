package com.rotd.havenpad.payment

import com.rotd.havenpad.config.ServerConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

object MpesaService {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun getAccessToken(): String {
        val key = ServerConfig.mpesaConsumerKey.trim()
        val secret = ServerConfig.mpesaConsumerSecret.trim()
        if (key.isBlank() || secret.isBlank()) {
            error("M-Pesa Consumer Key and Consumer Secret must be set in local.properties or environment variables.")
        }
        val auth = Base64.getEncoder().encodeToString("$key:$secret".toByteArray())
        val httpResponse = client.get(ServerConfig.mpesaAuthUrl) {
            header(HttpHeaders.Authorization, "Basic $auth")
        }
        if (!httpResponse.status.isSuccess()) {
            val bodyText = runCatching { httpResponse.bodyAsText() }.getOrDefault("")
            println("M-Pesa auth HTTP ${httpResponse.status.value}: $bodyText")
            error("M-Pesa authentication failed: ${httpResponse.status.description}. Check your Consumer Key & Secret.")
        }
        val response: MpesaAuthResponse = httpResponse.body()
        return response.accessToken
    }

    /**
     * @param amountKes whole Kenya shillings (Daraja Amount is not cents)
     * @param reference short account reference (max ~12 chars recommended)
     */
    suspend fun initiateStkPush(
        phoneNumber: String,
        amountKes: Long,
        reference: String,
        description: String = "HavenPad payment"
    ): MpesaStkResponse {
        require(amountKes >= 1) { "Amount must be at least 1 KES" }
        val shortCode = ServerConfig.mpesaBusinessShortCode.trim()
        val passkey = ServerConfig.mpesaPasskey.trim()
        if (shortCode.isBlank() || passkey.isBlank()) {
            error("M-Pesa Shortcode and Passkey must be set in local.properties or environment variables.")
        }

        val token = getAccessToken()
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val password = Base64.getEncoder().encodeToString(
            "$shortCode$passkey$timestamp".toByteArray()
        )

        val formattedPhone = formatMpesaPhone(phoneNumber)
        val safeReference = reference.filter { it.isLetterOrDigit() }.take(12).ifBlank { "HavenPad" }
        val safeDesc = description.filter { it.isLetterOrDigit() }.take(20).ifBlank { "Payment" }

        val secretToken = ServerConfig.mpesaCallbackSecretToken
        val effectiveCallbackUrl = if (!secretToken.isNullOrBlank() && !ServerConfig.mpesaCallbackUrl.contains("token=")) {
            if (ServerConfig.mpesaCallbackUrl.contains("?")) {
                "${ServerConfig.mpesaCallbackUrl}&token=$secretToken"
            } else {
                "${ServerConfig.mpesaCallbackUrl}?token=$secretToken"
            }
        } else {
            ServerConfig.mpesaCallbackUrl
        }

        val request = MpesaStkRequest(
            BusinessShortCode = shortCode,
            Password = password,
            Timestamp = timestamp,
            TransactionType = ServerConfig.mpesaCommandId,
            Amount = amountKes,
            PartyA = formattedPhone,
            PartyB = shortCode,
            PhoneNumber = formattedPhone,
            CallBackURL = effectiveCallbackUrl,
            AccountReference = safeReference,
            TransactionDesc = safeDesc
        )

        val httpResponse = client.post(ServerConfig.mpesaStkPushUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!httpResponse.status.isSuccess()) {
            val bodyText = runCatching { httpResponse.bodyAsText() }.getOrDefault("")
            println("M-Pesa STK push HTTP ${httpResponse.status.value}: $bodyText")
            error("M-Pesa STK Push request failed (${httpResponse.status.value}): $bodyText")
        }

        return httpResponse.body()
    }

    /**
     * Asks Daraja for the final STK outcome. Use when the callback URL cannot reach this server.
     */
    suspend fun queryStkPush(checkoutRequestId: String): MpesaStkQueryResponse {
        val shortCode = ServerConfig.mpesaBusinessShortCode.trim()
        val passkey = ServerConfig.mpesaPasskey.trim()
        val token = getAccessToken()
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val password = Base64.getEncoder().encodeToString(
            "$shortCode$passkey$timestamp".toByteArray()
        )
        val httpResponse = client.post(ServerConfig.mpesaStkQueryUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                MpesaStkQueryRequest(
                    BusinessShortCode = shortCode,
                    Password = password,
                    Timestamp = timestamp,
                    CheckoutRequestID = checkoutRequestId
                )
            )
        }
        if (!httpResponse.status.isSuccess()) {
            val bodyText = runCatching { httpResponse.bodyAsText() }.getOrDefault("")
            println("M-Pesa STK query HTTP ${httpResponse.status.value}: $bodyText")
            error("M-Pesa STK Query failed (${httpResponse.status.value}): $bodyText")
        }
        return httpResponse.body()
    }

    fun formatMpesaPhone(phoneNumber: String): String {
        val digits = phoneNumber.filter { it.isDigit() }
        return when {
            digits.startsWith("254") && digits.length >= 12 -> digits.take(12)
            digits.startsWith("0") && digits.length >= 10 -> "254" + digits.drop(1).take(9)
            digits.length == 9 -> "254$digits"
            else -> digits
        }
    }
}
