package com.rotd.kejasafe

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.rotd.kejasafe.auth.AuthService
import com.rotd.kejasafe.chat.ApartmentChatService
import com.rotd.kejasafe.community.PlatformStatsService
import com.rotd.kejasafe.config.ServerConfig
import com.rotd.kejasafe.db.initDatabase
import com.rotd.kejasafe.kyc.KycService
import com.rotd.kejasafe.listing.ListingService
import com.rotd.kejasafe.listing.LiveTourService
import com.rotd.kejasafe.listing.StoryService
import com.rotd.kejasafe.listing.propertyRequestRoutes
import com.rotd.kejasafe.payment.mpesaCallbackRoutes
import com.rotd.kejasafe.places.placesRoutes
import com.rotd.kejasafe.rent.RentService
import com.rotd.kejasafe.rent.escrowRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

fun main() {
    initDatabase()
    ListingService.purgeSyntheticBookings()
    embeddedServer(
        Netty,
        port = ServerConfig.port,
        host = ServerConfig.host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            // Send every field, so clients and the OpenAPI examples see full payloads.
            encodeDefaults = true
        })
    }

    install(WebSockets)

    install(CallLogging)

    // The Compose web client is served from a different origin than the API.
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        exposeHeader(HttpHeaders.ContentDisposition)
        if (ServerConfig.allowedOrigins.isEmpty()) {
            anyHost()
        } else {
            ServerConfig.allowedOrigins.forEach { origin ->
                allowHost(origin, schemes = listOf("http", "https"))
            }
        }
    }

    val jwtSecret = ServerConfig.jwtSecret
    val jwtIssuer = ServerConfig.jwtIssuer
    val jwtAudience = ServerConfig.jwtAudience

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "KejaSafe Server"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Welcome to KejaSafe API — interactive docs at /swagger")
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // Interactive API docs; the raw contract lives in resources/openapi/documentation.yaml.
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        staticFiles("/uploads", ListingService.uploadRoot)

        // Community stories: public to read, students/landlords write their own.
        get("/stories") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 12
            call.respond(StoryService.getStories(limit))
        }

        get("/stats") {
            call.respond(PlatformStatsService.getStats())
        }

        // Safaricom Daraja STK callback (must be public / no JWT)
        mpesaCallbackRoutes()

        route("/auth") {
            post("/register") {
                val request = call.receive<RegistrationRequest>()
                val user = AuthService.registerUser(request)
                if (user != null) {
                    val token = AuthService.generateToken(user)
                    call.respond(AuthResponse(token, user))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "User already exists")
                }
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val user = AuthService.loginUser(request.identifier, request.password)
                if (user != null) {
                    val token = AuthService.generateToken(user)
                    call.respond(AuthResponse(token, user))
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "Wrong username/email or password. If you only had a Firebase account, register again on this server."
                    )
                }
            }

            post("/google") {
                val request = call.receive<GoogleAuthRequest>()
                val user = AuthService.authenticateWithGoogle(request.idToken, request.role)
                if (user != null) {
                    val token = AuthService.generateToken(user)
                    call.respond(AuthResponse(token, user))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Google authentication failed")
                }
            }
        }

        // Public browse endpoints (tenants can view housing without auth, auth still accepted)
        get("/listings") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            call.respond(ListingService.getBrowseListings(lat, lng))
        }

        get("/listings/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val listing = ListingService.getListing(id)
            if (listing != null) call.respond(listing)
            else call.respond(HttpStatusCode.NotFound, "Listing not found")
        }

        authenticate("auth-jwt") {
            placesRoutes()

            route("/kyc") {
                post("/submit") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val submission = call.receive<KycSubmission>().copy(userId = userId)

                    KycService.submitKyc(submission)
                    call.respond(HttpStatusCode.OK, "KYC submitted successfully")
                }

                get("/status") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val status = KycService.getKycStatus(userId)
                    call.respond(mapOf("status" to status))
                }
            }

            route("/stories") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val request = call.receive<PostStoryRequest>()
                    try {
                        call.respond(HttpStatusCode.Created, StoryService.postStory(userId, request))
                    } catch (e: IllegalArgumentException) {
                        // Plain text so clients can show the reason as-is.
                        call.respondText(
                            text = e.message ?: "Could not post story",
                            contentType = ContentType.Text.Plain,
                            status = HttpStatusCode.BadRequest
                        )
                    }
                }

                get("/mine") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(StoryService.getStoriesByAuthor(userId))
                }

                delete("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val storyId = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing story id")
                    if (StoryService.deleteStory(storyId, userId)) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Story removed"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Story not found or not yours")
                    }
                }
            }

            get("/user/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val user = AuthService.getUserById(userId)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            put("/user/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val request = call.receive<UpdateProfileRequest>()
                if (request.fullName.isBlank() || request.username.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Name and username are required")
                    return@put
                }
                val updated = AuthService.updateProfile(userId, request)
                if (updated != null) {
                    call.respond(updated)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Could not update profile (username may be taken)")
                }
            }

            route("/rent") {
                get("/bills") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val bills = RentService.getBillsForUser(userId)
                    call.respond(bills)
                }

                post("/pay") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val request = call.receive<PaymentRequest>()
                    try {
                        val result = RentService.initiateBillPayment(
                            billId = request.billId,
                            payerUserId = userId,
                            phoneNumber = request.phoneNumber,
                            amountKes = request.amount.takeIf { it > 0 }?.toLong()
                        )
                        call.respond(HttpStatusCode.Accepted, BillPaymentResponse(
                            billId = result["billId"].orEmpty(),
                            checkoutRequestId = result["checkoutRequestId"].orEmpty(),
                            message = result["message"].orEmpty()
                        ))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Payment failed")
                    }
                }

                post("/generate") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val request = call.receive<GenerateBillRequest>()
                    try {
                        val bill = RentService.generateBillFromBooking(userId, request)
                        call.respond(HttpStatusCode.Created, bill)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not generate bill")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not generate bill")
                    }
                }

                post("/bills/{billId}/request-payment") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val billId = call.parameters["billId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing bill id")
                    try {
                        val bill = RentService.requestPayment(billId, userId)
                        call.respond(bill)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not request payment")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not request payment")
                    }
                }

                put("/bills/{billId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val billId = call.parameters["billId"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing bill id")
                    val request = call.receive<UpdateBillRequest>()
                    try {
                        val bill = RentService.updateBill(billId, userId, request)
                        call.respond(bill)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not update bill")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not update bill")
                    }
                }

                get("/bills/{billId}/pdf") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val billId = call.parameters["billId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing bill id")
                    try {
                        val bytes = RentService.buildBillPdf(billId, userId)
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            "inline; filename=\"kejasafe-invoice-$billId.pdf\""
                        )
                        call.respondBytes(bytes, ContentType.Application.Pdf)
                    } catch (e: IllegalStateException) {
                        println("Bill PDF not found for $billId: ${e.message}")
                        call.respond(HttpStatusCode.NotFound, e.message ?: "Bill not found")
                    } catch (e: Exception) {
                        println("Bill PDF failed for $billId: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            e.message?.take(200) ?: "Could not generate invoice PDF"
                        )
                    }
                }
            }

            route("/listings") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val request = call.receive<CreateListingRequest>()
                    if (request.title.isBlank() || request.address.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Title and address are required")
                        return@post
                    }
                    val listing = ListingService.createListing(userId, request)
                    call.respond(HttpStatusCode.Created, listing)
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<CreateListingRequest>()
                    if (request.title.isBlank() || request.address.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Title and address are required")
                        return@put
                    }
                    val listing = ListingService.updateListing(listingId, userId, request)
                    if (listing != null) call.respond(listing)
                    else call.respond(HttpStatusCode.NotFound, "Listing not found or not owned by you")
                }

                get("/{id}/analytics") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val analytics = ListingService.getListingAnalytics(listingId, userId)
                    if (analytics != null) call.respond(analytics)
                    else call.respond(HttpStatusCode.NotFound, "Listing not found or not owned by you")
                }

                get("/{id}/calendar") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString()
                    val listingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val year = call.request.queryParameters["year"]?.toIntOrNull()
                    val month = call.request.queryParameters["month"]?.toIntOrNull()
                    // Tenant calendar opens count as real views for landlord dashboards.
                    ListingService.recordListingView(listingId, userId)
                    val calendar = ListingService.getListingCalendar(listingId, year, month)
                    if (calendar != null) call.respond(calendar)
                    else call.respond(HttpStatusCode.NotFound, "Listing not found")
                }

                post("/{id}/book") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<BookListingRequest>()
                    try {
                        val booking = ListingService.bookListing(listingId, userId, request)
                        call.respond(HttpStatusCode.Created, booking)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not book")
                    }
                }

                post("/bookings/{bookingId}/approve") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val bookingId = call.parameters["bookingId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        val booking = ListingService.approveBooking(bookingId, userId)
                        call.respond(booking)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not approve")
                    }
                }

                post("/bookings/{bookingId}/decline") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val bookingId = call.parameters["bookingId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        val booking = ListingService.declineBooking(bookingId, userId)
                        call.respond(booking)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not decline")
                    }
                }

                get("/roommate-matches/{matchId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val matchId = call.parameters["matchId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val match = ListingService.getRoommateMatch(matchId, userId)
                    if (match != null) call.respond(match)
                    else call.respond(HttpStatusCode.NotFound, "Match not found")
                }

                post("/roommate-matches/{matchId}/approve") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val matchId = call.parameters["matchId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(ListingService.approveRoommateMatch(matchId, userId))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not approve match")
                    }
                }

                post("/roommate-matches/{matchId}/decline") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val matchId = call.parameters["matchId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(ListingService.declineRoommateMatch(matchId, userId))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not decline match")
                    }
                }

                post("/{id}/live-tours") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val body = call.receive<CreateLiveTourRequest>()
                    try {
                        call.respond(HttpStatusCode.Created, LiveTourService.createTour(listingId, userId, body))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not request live tour")
                    }
                }

                post("/{id}/live-tours/direct") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val body = call.receive<DirectLiveTourRequest>()
                    try {
                        call.respond(
                            HttpStatusCode.Created,
                            LiveTourService.startDirectTour(
                                listingId = listingId,
                                callerId = userId,
                                peerUserId = body.peerUserId,
                                mediaType = body.mediaType,
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not start live call")
                    }
                }

                get("/mine/bookings") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ListingService.getMyBookings(userId))
                }

                get("/mine/apartment-mates") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ListingService.getConfirmedApartmentMates(userId))
                }

                get("/mine") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ListingService.getListingsForLandlord(userId))
                }

                get("/mine/performance") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ListingService.getPerformance(userId))
                }

                get("/mine/requests") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ListingService.getIncomingRequests(userId))
                }

                post("/{id}/photos") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                    if (!ListingService.isOwnedBy(listingId, userId)) {
                        call.respond(HttpStatusCode.Forbidden, "Not your listing")
                        return@post
                    }

                    val uploaded = mutableListOf<ListingPhotoResponse>()
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val name = part.originalFileName
                                    ?: "photo_${System.currentTimeMillis()}.jpg"
                                val bytes = part.provider().readRemaining().readByteArray()
                                try {
                                    uploaded += ListingService.addPhoto(listingId, name, bytes)
                                } catch (e: IllegalArgumentException) {
                                    // skip invalid part
                                }
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }

                    if (uploaded.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, "No valid photos uploaded")
                    } else {
                        call.respond(HttpStatusCode.Created, uploaded)
                    }
                }

                delete("/{id}/photos/{photoId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val photoId = call.parameters["photoId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val deleted = ListingService.deletePhoto(listingId, photoId, userId)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Photo removed"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Photo not found or not your listing")
                    }
                }
            }

            route("/live-tours") {
                get("/mine") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(LiveTourService.openToursForTenant(userId))
                }

                get("/{sessionId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val sessionId = call.parameters["sessionId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val tour = LiveTourService.getTour(sessionId, userId)
                    if (tour != null) call.respond(tour)
                    else call.respond(HttpStatusCode.NotFound, "Live tour not found")
                }

                post("/{sessionId}/confirm") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(LiveTourService.confirmTour(sessionId, userId))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not confirm tour")
                    }
                }

                post("/{sessionId}/reschedule") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val body = call.receive<RescheduleLiveTourRequest>()
                    try {
                        call.respond(LiveTourService.rescheduleTour(sessionId, userId, body))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not reschedule tour")
                    }
                }

                post("/{sessionId}/token") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(LiveTourService.issueToken(sessionId, userId))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not issue LiveKit token")
                    }
                }

                post("/{sessionId}/end") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val sessionId = call.parameters["sessionId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    try {
                        call.respond(LiveTourService.endTour(sessionId, userId))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not end tour")
                    }
                }
            }

            escrowRoutes()
            propertyRequestRoutes()

            route("/chat") {
                get("/connections") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    call.respond(ApartmentChatService.listConnections(userId))
                }

                post("/connections") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val body = call.receive<UpsertChatConnectionRequest>()
                    try {
                        call.respond(ApartmentChatService.upsertConnection(userId, body))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid connection")
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid connection")
                    }
                }

                get("/messages") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.request.queryParameters["listingId"].orEmpty()
                    val peerUserId = call.request.queryParameters["peerUserId"].orEmpty()
                    val after = call.request.queryParameters["after"]?.toLongOrNull() ?: 0L
                    if (listingId.isBlank() || peerUserId.isBlank()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "listingId and peerUserId required"
                        )
                    }
                    try {
                        call.respond(
                            ApartmentChatService.listMessages(userId, listingId, peerUserId, after)
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
                    }
                }

                post("/messages") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val body = call.receive<SendApartmentChatMessageRequest>()
                    try {
                        call.respond(
                            HttpStatusCode.Created,
                            ApartmentChatService.sendMessage(userId, body)
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not send")
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Could not send")
                    }
                }

                post("/messages/delivered") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.request.queryParameters["listingId"].orEmpty()
                    val peerUserId = call.request.queryParameters["peerUserId"].orEmpty()
                    if (listingId.isBlank() || peerUserId.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest)
                    }
                    ApartmentChatService.markDelivered(userId, listingId, peerUserId)
                    call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                }

                post("/messages/read") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val listingId = call.request.queryParameters["listingId"].orEmpty()
                    val peerUserId = call.request.queryParameters["peerUserId"].orEmpty()
                    if (listingId.isBlank() || peerUserId.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest)
                    }
                    ApartmentChatService.markRead(userId, listingId, peerUserId)
                    call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                }

                delete("/messages/{messageId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val messageId = call.parameters["messageId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (ApartmentChatService.deleteMessage(userId, messageId)) {
                        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Message not found")
                    }
                }

                get("/attachments/{attachmentId}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val attachmentId = call.parameters["attachmentId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val bytes = ApartmentChatService.loadAttachment(userId, attachmentId)
                    if (bytes != null) {
                        call.respondBytes(bytes, ContentType.Application.OctetStream)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Attachment not found")
                    }
                }
            }
        }
    }
}
