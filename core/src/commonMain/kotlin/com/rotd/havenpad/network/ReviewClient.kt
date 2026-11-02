package com.rotd.havenpad.network

import com.rotd.havenpad.FollowUpReviewRequest
import com.rotd.havenpad.ListingReviewPage
import com.rotd.havenpad.MyReviews
import com.rotd.havenpad.ReplyReviewRequest
import com.rotd.havenpad.ReportReviewRequest
import com.rotd.havenpad.ReviewComposer
import com.rotd.havenpad.StayReview
import com.rotd.havenpad.SubmitReviewRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ReviewClient {
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
                encodeDefaults = true
            })
        }
    }

    private fun token(): String? = SessionManager.token.value?.let { "Bearer $it" }

    suspend fun listingReviews(listingId: String): Result<ListingReviewPage> = runApi {
        val response = client.get("${AuthClient.BASE_URL}/listings/$listingId/reviews") {
            token()?.let { header(HttpHeaders.Authorization, it) }
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load reviews")
    }

    suspend fun composer(listingId: String): Result<ReviewComposer> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/listings/$listingId/review-composer") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load review form")
    }

    suspend fun mine(): Result<MyReviews> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/reviews/me") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load your reviews")
    }

    suspend fun submit(request: SubmitReviewRequest): Result<StayReview> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/reviews") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Save review")
    }

    suspend fun reply(reviewId: String, reply: String): Result<StayReview> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/reviews/$reviewId/reply") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(ReplyReviewRequest(reply))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Save reply")
    }

    suspend fun report(reviewId: String, reason: String): Result<StayReview> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/reviews/$reviewId/report") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(ReportReviewRequest(reason))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Report review")
    }

    suspend fun reports(): Result<List<StayReview>> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/admin/review-reports") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load reported reviews")
    }

    suspend fun followUp(reviewId: String, note: String): Result<StayReview> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/admin/review-reports/$reviewId/follow-up") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(FollowUpReviewRequest(note))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Save follow-up")
    }

    suspend fun dismiss(reviewId: String): Result<StayReview> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/admin/review-reports/$reviewId/dismiss") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Keep review")
    }

    suspend fun delete(reviewId: String): Result<Unit> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.delete("${AuthClient.BASE_URL}/admin/reviews/$reviewId") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Delete review")
    }
}
