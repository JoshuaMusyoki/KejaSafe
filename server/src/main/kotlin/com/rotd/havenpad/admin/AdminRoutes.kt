package com.rotd.havenpad.admin

import com.rotd.havenpad.CreateLandlordRequest
import com.rotd.havenpad.FollowUpReviewRequest
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.RejectKycRequest
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.listing.ReviewService
import com.rotd.havenpad.auth.AuthService
import com.rotd.havenpad.kyc.KycService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.adminRoutes() {
    route("/admin") {
        get("/users") {
            if (requireAdmin(call) == null) return@get
            val role = runCatching {
                UserRole.valueOf(call.request.queryParameters["role"] ?: "LANDLORD")
            }.getOrDefault(UserRole.LANDLORD)
            try {
                call.respond(AdminService.list(role))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not list accounts", status = HttpStatusCode.BadRequest)
            }
        }

        post("/landlords") {
            if (requireAdmin(call) == null) return@post
            val request = call.receive<CreateLandlordRequest>()
            try {
                call.respond(HttpStatusCode.Created, AdminService.addLandlord(request))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not add landlord", status = HttpStatusCode.BadRequest)
            }
        }

        get("/users/{id}") {
            if (requireAdmin(call) == null) return@get
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            try {
                call.respond(AdminService.detail(id))
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/users/{id}/kyc/{kind}") {
            if (requireAdmin(call) == null) return@get
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val kind = call.parameters["kind"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = KycService.readFile(id, kind)
            if (file == null) {
                call.respond(HttpStatusCode.NotFound, "Document not found")
            } else {
                call.respondBytes(file.first, ContentType.parse(file.second))
            }
        }

        post("/users/{id}/verify") {
            if (requireAdmin(call) == null) return@post
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                AdminService.review(id, KycStatus.VERIFIED, null)
                call.respond(AdminService.detail(id))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not verify", status = HttpStatusCode.BadRequest)
            }
        }

        post("/users/{id}/reject") {
            if (requireAdmin(call) == null) return@post
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val note = runCatching { call.receive<RejectKycRequest>().note }.getOrDefault("")
            try {
                AdminService.review(id, KycStatus.REJECTED, note.ifBlank { "Documents need to be submitted again." })
                call.respond(AdminService.detail(id))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not reject", status = HttpStatusCode.BadRequest)
            }
        }

        get("/review-reports") {
            if (requireAdmin(call) == null) return@get
            call.respond(ReviewService.openReports())
        }

        post("/review-reports/{id}/follow-up") {
            if (requireAdmin(call) == null) return@post
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<FollowUpReviewRequest>()
            try {
                call.respond(ReviewService.followUp(id, request))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not save the follow-up", status = HttpStatusCode.BadRequest)
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Review not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/review-reports/{id}/dismiss") {
            if (requireAdmin(call) == null) return@post
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            try {
                call.respond(ReviewService.dismiss(id))
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Review not found", status = HttpStatusCode.NotFound)
            }
        }

        delete("/reviews/{id}") {
            if (requireAdmin(call) == null) return@delete
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            try {
                ReviewService.delete(id)
                call.respond(mapOf("message" to "Review deleted"))
            } catch (e: IllegalStateException) {
                call.respondText(e.message ?: "Review not found", status = HttpStatusCode.NotFound)
            }
        }

        delete("/users/{id}") {
            val adminId = requireAdmin(call) ?: return@delete
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            try {
                AdminService.remove(adminId, id)
                call.respond(mapOf("message" to "Account removed"))
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Could not remove account", status = HttpStatusCode.BadRequest)
            }
        }
    }
}

private suspend fun requireAdmin(call: ApplicationCall): String? {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.getClaim("userId")?.asString().orEmpty()
    if (userId.isBlank() || !AuthService.isActive(userId) || !isAdmin(userId)) {
        call.respondText("Admin access only", status = HttpStatusCode.Forbidden)
        return null
    }
    return userId
}

private fun isAdmin(userId: String): Boolean {
    val user = AuthService.getUserById(userId) ?: return false
    return user.role == UserRole.ADMIN
}
