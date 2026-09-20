package com.rotd.kejasafe.payment

import com.rotd.kejasafe.listing.PropertyRequestService
import com.rotd.kejasafe.rent.EscrowService
import com.rotd.kejasafe.rent.RentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * Public Safaricom callback. Daraja must POST here after the user enters their PIN.
 * Dispatches by CheckoutRequestID to escrow, rent bill, or property-request payments.
 */
fun Route.mpesaCallbackRoutes() {
    val json = Json { ignoreUnknownKeys = true }

    route("/mpesa") {
        post("/callback") {
            val raw = call.receiveText()
            println("M-Pesa callback: $raw")
            try {
                val envelope = json.decodeFromString<MpesaCallbackEnvelope>(raw)
                val cb = envelope.body.stkCallback
                val paid = cb.ResultCode == 0
                EscrowService.handleMpesaCallback(cb.CheckoutRequestID, paid, cb.ResultDesc)
                RentService.handleMpesaCallback(
                    checkoutRequestId = cb.CheckoutRequestID,
                    paid = paid,
                    resultDesc = cb.ResultDesc,
                    receiptCode = cb.mpesaReceiptCode
                )
                PropertyRequestService.handleMpesaCallback(
                    cb.CheckoutRequestID,
                    cb.ResultCode,
                    cb.ResultDesc
                )
            } catch (e: Exception) {
                println("M-Pesa callback parse/handle error: ${e.message}")
            }
            call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Accepted"))
        }
    }
}
