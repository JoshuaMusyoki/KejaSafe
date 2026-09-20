package com.rotd.kejasafe.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MpesaAuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: String
)

@Serializable
data class MpesaStkRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String,
    val Amount: Long,
    val PartyA: String,
    val PartyB: String,
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)

@Serializable
data class MpesaStkResponse(
    val MerchantRequestID: String? = null,
    val CheckoutRequestID: String? = null,
    val ResponseCode: String? = null,
    val ResponseDescription: String? = null,
    val CustomerMessage: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    val accepted: Boolean get() = ResponseCode == "0" && !CheckoutRequestID.isNullOrBlank()
    val failureMessage: String
        get() = errorMessage
            ?: ResponseDescription
            ?: CustomerMessage
            ?: "M-Pesa STK push failed"
}

/** Safaricom Daraja STK callback envelope. */
@Serializable
data class MpesaCallbackEnvelope(
    @SerialName("Body") val body: MpesaCallbackBody
)

@Serializable
data class MpesaCallbackBody(
    val stkCallback: MpesaStkCallback
)

@Serializable
data class MpesaStkCallback(
    val MerchantRequestID: String? = null,
    val CheckoutRequestID: String,
    val ResultCode: Int,
    val ResultDesc: String = "",
    val CallbackMetadata: MpesaCallbackMetadata? = null
) {
    fun metadataValue(name: String): String? {
        val item = CallbackMetadata?.Item?.firstOrNull {
            it.Name.equals(name, ignoreCase = true)
        } ?: return null
        val el = item.Value ?: return null
        return when (el) {
            is kotlinx.serialization.json.JsonPrimitive -> el.content
            else -> el.toString().trim('"')
        }.takeIf { it.isNotBlank() }
    }

    val mpesaReceiptCode: String?
        get() = metadataValue("MpesaReceiptNumber")
}

@Serializable
data class MpesaCallbackMetadata(
    val Item: List<MpesaCallbackItem> = emptyList()
)

@Serializable
data class MpesaCallbackItem(
    val Name: String,
    /** Daraja sends numbers for Amount / PhoneNumber and strings for others. */
    val Value: kotlinx.serialization.json.JsonElement? = null
)
