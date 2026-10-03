package com.rotd.havenpad.network

import com.rotd.havenpad.AdminUserDetail
import com.rotd.havenpad.AdminUserSummary
import com.rotd.havenpad.CreateLandlordRequest
import com.rotd.havenpad.RejectKycRequest
import com.rotd.havenpad.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AdminClient {
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
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

    suspend fun listUsers(role: UserRole): Result<List<AdminUserSummary>> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/admin/users") {
            header(HttpHeaders.Authorization, auth)
            parameter("role", role.name)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load accounts")
    }

    suspend fun userDetail(userId: String): Result<AdminUserDetail> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/admin/users/$userId") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load account")
    }

    suspend fun kycImage(userId: String, kind: String): Result<ByteArray> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.get("${AuthClient.BASE_URL}/admin/users/$userId/kyc/$kind") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.bodyAsBytes())
        else httpFailure(response, "Load document")
    }

    suspend fun addLandlord(request: CreateLandlordRequest): Result<AdminUserSummary> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/admin/landlords") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Add landlord")
    }

    suspend fun verify(userId: String): Result<AdminUserDetail> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/admin/users/$userId/verify") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Verify account")
    }

    suspend fun reject(userId: String, note: String): Result<AdminUserDetail> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.post("${AuthClient.BASE_URL}/admin/users/$userId/reject") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(RejectKycRequest(note))
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Reject documents")
    }

    suspend fun remove(userId: String): Result<Unit> = runApi {
        val auth = token() ?: return@runApi notSignedIn()
        val response = client.delete("${AuthClient.BASE_URL}/admin/users/$userId") {
            header(HttpHeaders.Authorization, auth)
        }
        if (response.status.isSuccess()) Result.success(Unit)
        else httpFailure(response, "Remove account")
    }
}
