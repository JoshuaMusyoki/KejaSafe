package com.rotd.havenpad.listing

import com.rotd.havenpad.domain.listing.InitiatePropertyRequestPayment
import com.rotd.havenpad.payment.MpesaCallbackEnvelope
import com.rotd.havenpad.rent.EscrowService
import com.rotd.havenpad.rent.RentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Route.propertyRequestRoutes() {
    route("/property-requests") {
        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val body = call.receive<InitiatePropertyRequestPayment>()

                val requestId = PropertyRequestService.createRequest(userId, body.request)

                try {
                    PropertyRequestService.initiatePayment(requestId, body.phoneNumber)
                    call.respond(
                        HttpStatusCode.Accepted,
                        mapOf(
                            "requestId" to requestId,
                            "message" to "STK Push sent to ${body.phoneNumber}. Please complete payment to activate your request."
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (e.message ?: "M-Pesa initiation failed"))
                    )
                }
            }
        }

        /** Legacy callback path — prefer POST /mpesa/callback. */
        post("/mpesa-callback") {
            val raw = call.receiveText()
            println("M-Pesa callback (legacy property-requests): $raw")
            try {
                val envelope = Json { ignoreUnknownKeys = true }
                    .decodeFromString<MpesaCallbackEnvelope>(raw)
                val cb = envelope.body.stkCallback
                val paid = cb.ResultCode == 0
                PropertyRequestService.handleMpesaCallback(cb.CheckoutRequestID, cb.ResultCode, cb.ResultDesc)
                EscrowService.handleMpesaCallback(cb.CheckoutRequestID, paid, cb.ResultDesc)
                RentService.handleMpesaCallback(
                    checkoutRequestId = cb.CheckoutRequestID,
                    resultCode = cb.ResultCode,
                    resultDesc = cb.ResultDesc,
                    receiptCode = cb.mpesaReceiptCode,
                    paidAmountKes = cb.metadataValue("Amount")?.toDoubleOrNull()
                )
            } catch (e: Exception) {
                println("Legacy callback error: ${e.message}")
            }
            call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Success"))
        }
    }
}
