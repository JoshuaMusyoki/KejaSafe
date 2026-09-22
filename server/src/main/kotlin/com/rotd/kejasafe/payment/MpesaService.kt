package com.rotd.kejasafe.payment

import com.rotd.kejasafe.config.ServerConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
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

    private const val AUTH_URL =
        "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
    private const val STK_PUSH_URL =
        "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
    private const val STK_QUERY_URL =
        "https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query"

    suspend fun getAccessToken(): String {
        val auth = Base64.getEncoder().encodeToString(
            "${ServerConfig.mpesaConsumerKey}:${ServerConfig.mpesaConsumerSecret}".toByteArray()
        )
        val response: MpesaAuthResponse = client.get(AUTH_URL) {
            header(HttpHeaders.Authorization, "Basic $auth")
        }.body()
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
        description: String = "KejaSafe payment"
    ): MpesaStkResponse {
        require(amountKes >= 1) { "Amount must be at least 1 KES" }
        val token = getAccessToken()
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val password = Base64.getEncoder().encodeToString(
            "${ServerConfig.mpesaBusinessShortCode}${ServerConfig.mpesaPasskey}$timestamp".toByteArray()
        )

        val formattedPhone = formatMpesaPhone(phoneNumber)
        val safeReference = reference.filter { it.isLetterOrDigit() }.take(12).ifBlank { "KejaSafe" }
        val safeDesc = description.filter { it.isLetterOrDigit() }.take(20).ifBlank { "Payment" }

        val request = MpesaStkRequest(
            BusinessShortCode = ServerConfig.mpesaBusinessShortCode,
            Password = password,
            Timestamp = timestamp,
            TransactionType = "CustomerPayBillOnline",
            Amount = amountKes,
            PartyA = formattedPhone,
            PartyB = ServerConfig.mpesaBusinessShortCode,
            PhoneNumber = formattedPhone,
            CallBackURL = ServerConfig.mpesaCallbackUrl,
            AccountReference = safeReference,
            TransactionDesc = safeDesc
        )

        return client.post(STK_PUSH_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Asks Daraja for the final STK outcome. Use when the callback URL cannot reach this server.
     */
    suspend fun queryStkPush(checkoutRequestId: String): MpesaStkQueryResponse {
        val token = getAccessToken()
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val password = Base64.getEncoder().encodeToString(
            "${ServerConfig.mpesaBusinessShortCode}${ServerConfig.mpesaPasskey}$timestamp".toByteArray()
        )
        return client.post(STK_QUERY_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                MpesaStkQueryRequest(
                    BusinessShortCode = ServerConfig.mpesaBusinessShortCode,
                    Password = password,
                    Timestamp = timestamp,
                    CheckoutRequestID = checkoutRequestId
                )
            )
        }.body()
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
