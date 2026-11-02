package com.rotd.havenpad.listing

import com.rotd.havenpad.ReplyReviewRequest
import com.rotd.havenpad.ReportReviewRequest
import com.rotd.havenpad.SubmitReviewRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.reviewRoutes() {
    route("/reviews") {
        get("/me") {
            val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized, "Sign in first")
            try {
                call.respond(ReviewService.mine(userId))
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Could not load reviews", status = HttpStatusCode.BadRequest)
            }
        }

        post {
            val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, "Sign in first")
            val request = call.receive<SubmitReviewRequest>()
            try {
                call.respond(HttpStatusCode.Created, ReviewService.submit(userId, request))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not save the review", status = HttpStatusCode.BadRequest)
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "House not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/{id}/reply") {
            val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, "Sign in first")
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<ReplyReviewRequest>()
            try {
                call.respond(ReviewService.reply(userId, id, request))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not save the reply", status = HttpStatusCode.BadRequest)
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Review not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/{id}/report") {
            val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, "Sign in first")
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<ReportReviewRequest>()
            try {
                call.respond(ReviewService.report(userId, id, request))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not report the review", status = HttpStatusCode.BadRequest)
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Review not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    get("/listings/{listingId}/review-composer") {
        val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized, "Sign in first")
        val listingId = call.parameters["listingId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        try {
            call.respond(ReviewService.composer(userId, listingId))
        } catch (e: IllegalStateException) {
            call.respondText(e.message ?: "House not found", status = HttpStatusCode.NotFound)
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()?.takeIf { it.isNotBlank() }
