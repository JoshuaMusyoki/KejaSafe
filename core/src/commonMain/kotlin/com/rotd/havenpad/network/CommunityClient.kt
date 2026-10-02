package com.rotd.havenpad.network

import com.rotd.havenpad.CommunityStory
import com.rotd.havenpad.PlatformStats
import com.rotd.havenpad.PostStoryRequest
import com.rotd.havenpad.error.AppException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object CommunityClient {
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

    suspend fun getStories(limit: Int = 12): Result<List<CommunityStory>> = try {
        val response = client.get("${AuthClient.BASE_URL}/stories") {
            parameter("limit", limit)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(AppException.from(Exception("Load stories failed: ${response.status}")))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getMyStories(): Result<List<CommunityStory>> = try {
        val token = authHeader() ?: return Result.failure(AppException.notSignedIn())
        val response = client.get("${AuthClient.BASE_URL}/stories/mine") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(AppException.from(Exception("Load stories failed: ${response.status}")))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun postStory(request: PostStoryRequest): Result<CommunityStory> = try {
        val token = authHeader() ?: return Result.failure(AppException.notSignedIn())
        val response = client.post("${AuthClient.BASE_URL}/stories") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            // The server explains why (too short, wrong role) in the response body.
            val reason = runCatching { response.bodyAsText() }.getOrNull()
            Result.failure(
                AppException.fromHttp(response.status.value, reason, "Posting your story")
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun deleteStory(storyId: String): Result<Unit> = try {
        val token = authHeader() ?: return Result.failure(AppException.notSignedIn())
        val response = client.delete("${AuthClient.BASE_URL}/stories/$storyId") {
            header(HttpHeaders.Authorization, token)
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(AppException.from(Exception("Delete story failed: ${response.status}")))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    suspend fun getStats(): Result<PlatformStats> = try {
        val response = client.get("${AuthClient.BASE_URL}/stats")
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(AppException.from(Exception("Load stats failed: ${response.status}")))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }
}
