package com.rotd.stawi.data.repository

import android.content.SharedPreferences
import android.util.Base64
import com.rotd.stawi.data.remote.dto.MpesaStkRequest
import com.rotd.stawi.data.remote.dto.MpesaStkResponse
import com.rotd.stawi.domain.repository.BillingRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BillingRepositoryImpl(
    private val httpClient: HttpClient,
    private val encryptedPrefs: SharedPreferences
) : BillingRepository {

    override suspend fun initiateMpesaStkPush(phoneNumber: String, amount: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val businessShortCode = "174379" // Sandbox
            val passkey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919"
            val password = Base64.encodeToString(
                (businessShortCode + passkey + timestamp).toByteArray(),
                Base64.NO_WRAP
            )

            val request = MpesaStkRequest(
                BusinessShortCode = businessShortCode,
                Password = password,
                Timestamp = timestamp,
                Amount = amount,
                PartyA = phoneNumber,
                PartyB = businessShortCode,
                PhoneNumber = phoneNumber,
                CallBackURL = "https://yourdomain.com/mpesa/callback",
                AccountReference = "Stawi_Premium",
                TransactionDesc = "Premium Subscription"
            )

            val response: MpesaStkResponse = httpClient.post("https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/query") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.ResponseCode == "0") {
                Result.success(true)
            } else {
                Result.failure(Exception(response.ResponseDescription))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifySubscription(cardToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        // Implement card verification logic here
        // If successful, update local state
        encryptedPrefs.edit().putBoolean("is_premium", true).apply()
        Result.success(true)
    }

    override suspend fun isPremiumActive(): Boolean {
        return encryptedPrefs.getBoolean("is_premium", false)
    }
}



