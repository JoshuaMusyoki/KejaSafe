package com.rotd.kejasafe.network

import com.rotd.kejasafe.AuthResponse
import com.rotd.kejasafe.GoogleAuthRequest
import com.rotd.kejasafe.LoginRequest
import com.rotd.kejasafe.RegistrationRequest
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AuthClient {
    private val client = HttpClient {
        expectSuccess = false
        install(io.ktor.client.plugins.HttpTimeout) {
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

    // Use 10.0.2.2 for Android Emulator
    // Use your machine's IP (10.8.120.210) for physical devices
    //const val BASE_URL = "http://10.8.120.210:8080"
    const val BASE_URL = "http://192.168.100.3:8080"
    //const val BASE_URL = "http://10.120.212.251:8080"


    suspend fun register(request: RegistrationRequest): Result<AuthResponse> = runApi {
        val response = client.post("$BASE_URL/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Registration")
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runApi {
        val response = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Login")
        }
    }

    suspend fun googleAuth(request: GoogleAuthRequest): Result<AuthResponse> = runApi {
        val response = client.post("$BASE_URL/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Google auth")
        }
    }

    suspend fun getProfile(): Result<User> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.get("$BASE_URL/user/profile") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load profile")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<User> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.put("$BASE_URL/user/profile") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Update profile")
        }
    }
}
