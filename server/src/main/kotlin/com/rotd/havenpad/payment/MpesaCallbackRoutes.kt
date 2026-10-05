package com.rotd.havenpad.payment

import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.listing.PropertyRequestService
import com.rotd.havenpad.rent.EscrowService
import com.rotd.havenpad.rent.RentService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Public Safaricom HTTPS callback. Daraja POSTs here after the user responds to STK Push.
 * Dispatches by CheckoutRequestID to escrow, rent bill, or property-request payments.
 */
fun Route.mpesaCallbackRoutes() {
    val json = Json { ignoreUnknownKeys = true }

    route("/mpesa") {
        post("/callback") {
            // Optional security validation: Check callback token if configured
            val expectedToken = ServerConfig.mpesaCallbackSecretToken
            if (!expectedToken.isNullOrBlank()) {
                val queryToken = call.request.queryParameters["token"]
                val headerToken = call.request.headers["X-Mpesa-Token"]
                if (queryToken != expectedToken && headerToken != expectedToken) {
                    println("M-Pesa callback rejected: Invalid token")
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid callback token"))
                    return@post
                }
            }

            val raw = call.receiveText()
            println("M-Pesa callback received (${ServerConfig.mpesaEnv} mode): $raw")

            try {
                val envelope = json.decodeFromString<MpesaCallbackEnvelope>(raw)
                val cb = envelope.body.stkCallback
                val paid = cb.ResultCode == 0

                val phoneFromMeta = cb.metadataValue("PhoneNumber")
                val customerNameFromMeta = cb.metadataValue("CustomerName") ?: cb.metadataValue("PayerName")
                var actualPayerName = customerNameFromMeta
                if (actualPayerName.isNullOrBlank() && !phoneFromMeta.isNullOrBlank()) {
                    transaction {
                        val digits = phoneFromMeta.filter { it.isDigit() }
                        if (digits.length >= 9) {
                            val suffix9 = digits.takeLast(9)
                            UsersTable.selectAll()
                                .where {
                                    (UsersTable.phone eq phoneFromMeta) or
                                    (UsersTable.phone eq "0$suffix9") or
                                    (UsersTable.phone eq "254$suffix9") or
                                    (UsersTable.phone eq "+254$suffix9")
                                }
                                .firstOrNull()
                                ?.get(UsersTable.fullName)
                        } else null
                    }?.let { actualPayerName = it }
                }

                EscrowService.handleMpesaCallback(cb.CheckoutRequestID, paid, cb.ResultDesc)
                RentService.handleMpesaCallback(
                    checkoutRequestId = cb.CheckoutRequestID,
                    resultCode = cb.ResultCode,
                    resultDesc = cb.ResultDesc,
                    receiptCode = cb.mpesaReceiptCode,
                    paidAmountKes = cb.metadataValue("Amount")?.toDoubleOrNull(),
                    payerName = actualPayerName
                )
                PropertyRequestService.handleMpesaCallback(
                    cb.CheckoutRequestID,
                    cb.ResultCode,
                    cb.ResultDesc
                )
            } catch (e: Exception) {
                println("M-Pesa callback parse/handle error: ${e.message}")
            }
            // Safaricom Daraja requires HTTP 200 OK fast response with ResultCode 0
            call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Accepted"))
        }
    }
}
