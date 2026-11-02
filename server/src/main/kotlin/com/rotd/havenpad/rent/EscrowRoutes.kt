package com.rotd.havenpad.rent

import com.rotd.havenpad.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.escrowRoutes() {
    authenticate("auth-jwt") {
        route("/escrow") {
            /** Triggers the payment collection prompt to the student's phone. */
            post("/initiate") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val request = call.receive<InitiatePaymentRequest>()
                
                try {
                    val response = EscrowService.initiatePayment(userId, request)
                    call.respond(HttpStatusCode.Accepted, response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            /** Queries current escrow state and fee breakdown. */
            get("/status/{transactionId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val transactionId = call.parameters["transactionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                try {
                    val response = EscrowService.getEscrowStatus(transactionId, userId)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                }
            }

            /** Accepts student release OTP and executes payout logic. */
            post("/release") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val request = call.receive<ReleaseEscrowRequest>()
                
                try {
                    val response = EscrowService.releaseEscrow(userId, request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            /** Tenant raises a dispute to freeze escrow payout. */
            post("/dispute") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val request = call.receive<DisputeEscrowRequest>()
                
                try {
                    val response = EscrowService.disputeEscrow(userId, request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            /** WebSocket for real-time status updates. */
            webSocket("/updates/{transactionId}") {
                val transactionId = call.parameters["transactionId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing ID"))
                
                EscrowService.observeTransaction(transactionId) { newState ->
                    val update = PaymentStatusResponse(transactionId, newState)
                    send(Frame.Text(Json.encodeToString(update)))
                }

                try {
                    for (frame in incoming) {
                        // Keep alive
                    }
                } finally {
                    EscrowService.stopObserving(transactionId)
                }
            }
        }
    }
}
