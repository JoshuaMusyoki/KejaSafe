package com.rotd.kejasafe.network

import com.rotd.kejasafe.BookListingRequest
import com.rotd.kejasafe.CreateListingRequest
import com.rotd.kejasafe.ListingAnalytics
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.ListingCalendar
import com.rotd.kejasafe.ListingPerformance
import com.rotd.kejasafe.ListingPhotoResponse
import com.rotd.kejasafe.PendingListingPhoto
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.GenerateBillRequest
import com.rotd.kejasafe.PaymentRequest
import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.RoommateMatch
import com.rotd.kejasafe.UpdateBillRequest
import com.rotd.kejasafe.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object ListingClient {
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private fun authHeader(): String? =
        SessionManager.token.value?.let { "Bearer $it" }

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = AuthClient.BASE_URL.trimEnd('/')
        return base + if (path.startsWith("/")) path else "/$path"
    }

    suspend fun fetchImageBytes(url: String): ByteArray {
        return try {
            val response = client.get(url)
            if (!response.status.isSuccess()) {
                error("Image failed: ${response.status}")
            }
            response.readRawBytes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // ClosedChannel / write-to-closed-channel happens when UI cancels a fetch
            // (scroll/offscreen) — surface a plain failure for callers' catch blocks.
            throw Exception(e.message ?: "Image download interrupted", e)
        }
    }

    suspend fun createListing(request: CreateListingRequest): Result<PropertyListing> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Create listing failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getListing(id: String): Result<PropertyListing> = try {
        val token = authHeader()
        val response = client.get("${AuthClient.BASE_URL}/listings/$id") {
            if (token != null) header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load listing failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getListingAnalytics(listingId: String): Result<ListingAnalytics> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/$listingId/analytics") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load analytics failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getListingCalendar(
        listingId: String,
        year: Int? = null,
        month: Int? = null
    ): Result<ListingCalendar> = try {
        val token = authHeader()
        val response = client.get("${AuthClient.BASE_URL}/listings/$listingId/calendar") {
            if (token != null) header(HttpHeaders.Authorization, token)
            if (year != null) parameter("year", year)
            if (month != null) parameter("month", month)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load calendar failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun bookListing(
        listingId: String,
        request: BookListingRequest
    ): Result<ListingBooking> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/$listingId/book") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Booking failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun approveBooking(bookingId: String): Result<ListingBooking> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/bookings/$bookingId/approve") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Approve failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun declineBooking(bookingId: String): Result<ListingBooking> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/bookings/$bookingId/decline") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Decline failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getRoommateMatch(matchId: String): Result<RoommateMatch> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/roommate-matches/$matchId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load roommate match")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun approveRoommateMatch(matchId: String): Result<RoommateMatch> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/roommate-matches/$matchId/approve") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Approve match failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun declineRoommateMatch(matchId: String): Result<RoommateMatch> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/listings/roommate-matches/$matchId/decline") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Decline match failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun generateBill(request: GenerateBillRequest): Result<RentBill> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/rent/generate") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Generate bill failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getBills(): Result<List<RentBill>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/rent/bills") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load bills failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun downloadBillPdf(billId: String): Result<ByteArray> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/rent/bills/$billId/pdf") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.readRawBytes())
        } else {
            httpFailure(response, "PDF download failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun payBill(billId: String, amount: Double = 0.0): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/rent/pay") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(PaymentRequest(billId = billId, amount = amount))
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            httpFailure(response, "Payment failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun requestPayment(billId: String): Result<RentBill> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/rent/bills/$billId/request-payment") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Request payment failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun updateBill(billId: String, request: UpdateBillRequest): Result<RentBill> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.put("${AuthClient.BASE_URL}/rent/bills/$billId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Update bill failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun deleteListingPhoto(listingId: String, photoId: String): Result<Unit> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.delete("${AuthClient.BASE_URL}/listings/$listingId/photos/$photoId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            httpFailure(response, "Delete photo failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getMyBookings(): Result<List<ListingBooking>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/mine/bookings") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load bookings")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getApartmentMates(): Result<List<ListingBooking>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/mine/apartment-mates") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load apartment mates")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getIncomingRequests(): Result<List<com.rotd.kejasafe.IncomingRequest>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/mine/requests") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load requests")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun updateListing(listingId: String, request: CreateListingRequest): Result<PropertyListing> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.put("${AuthClient.BASE_URL}/listings/$listingId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Update listing failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getMyListings(): Result<List<PropertyListing>> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/mine") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load listings")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getBrowseListings(): Result<List<PropertyListing>> = try {
        val token = authHeader()
        val response = client.get("${AuthClient.BASE_URL}/listings") {
            if (token != null) header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load listings")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getMyPerformance(): Result<ListingPerformance> = try {
        val token = authHeader() ?: return notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/mine/performance") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Failed to load performance")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun uploadListingPhotos(
        listingId: String,
        photos: List<PendingListingPhoto>
    ): Result<List<ListingPhotoResponse>> = try {
        if (photos.isEmpty()) return Result.success(emptyList())
        val token = authHeader() ?: return notSignedIn()
        val response = client.submitFormWithBinaryData(
            url = "${AuthClient.BASE_URL}/listings/$listingId/photos",
            formData = formData {
                photos.forEach { photo ->
                    append(
                        key = "photos",
                        value = photo.bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, photo.mimeType)
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"${photo.fileName}\""
                            )
                        }
                    )
                }
            }
        ) {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Photo upload failed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun createListingWithPhotos(
        request: CreateListingRequest,
        photos: List<PendingListingPhoto>
    ): Result<PropertyListing> {
        val created = createListing(request).getOrElse { return Result.failure(it) }
        if (photos.isEmpty()) return Result.success(created)

        val upload = uploadListingPhotos(created.id, photos)
        return if (upload.isSuccess) {
            getListing(created.id).getOrElse { created }.let { Result.success(it) }
        } else {
            Result.failure(AppException.from(Exception("Listing ${created.id} created but photos failed: ${upload.exceptionOrNull()?.message}")))
        }
    }

    suspend fun updateListingWithPhotos(
        listingId: String,
        request: CreateListingRequest,
        photos: List<PendingListingPhoto>
    ): Result<PropertyListing> {
        val updated = updateListing(listingId, request).getOrElse { return Result.failure(it) }
        if (photos.isEmpty()) return Result.success(updated)

        val upload = uploadListingPhotos(listingId, photos)
        return if (upload.isSuccess) {
            getListing(listingId).getOrElse { updated }.let { Result.success(it) }
        } else {
            Result.failure(AppException.from(Exception("Listing updated but photos failed: ${upload.exceptionOrNull()?.message}")))
        }
    }
}
