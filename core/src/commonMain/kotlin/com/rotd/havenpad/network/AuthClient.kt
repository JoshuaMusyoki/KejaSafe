package com.rotd.havenpad.network

import com.rotd.havenpad.AuthResponse
import com.rotd.havenpad.DeviceHelloRequest
import com.rotd.havenpad.GoogleClientConfig
import com.rotd.havenpad.GoogleAuthRequest
import com.rotd.havenpad.LandlordKycRecord
import com.rotd.havenpad.LandlordKycRequest
import com.rotd.havenpad.StudentKycRequest
import com.rotd.havenpad.LoginRequest
import com.rotd.havenpad.LoginResponse
import com.rotd.havenpad.SignInPrompt
import com.rotd.havenpad.SignedInDevice
import com.rotd.havenpad.device.DeviceIdentity
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
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
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
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

    /**
     * Resolved per platform — browsers use the origin they were served from, mobile and
     * desktop use [ApiConfig.DEV_API_BASE_URL]. Call [ApiConfig.useBaseUrl] to override.
     */
    val BASE_URL: String get() = ApiConfig.baseUrl


    suspend fun googleClientId(): Result<String> = runApi {
        val response = client.get("$BASE_URL/auth/google-client")
        if (response.status.isSuccess()) {
            Result.success(response.body<GoogleClientConfig>().clientId)
        } else {
            httpFailure(response, "Google sign-in")
        }
    }

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

    suspend fun login(request: LoginRequest): Result<LoginResponse> = runApi {
        val response = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request.withThisDevice())
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Login")
        }
    }

    suspend fun googleAuth(request: GoogleAuthRequest): Result<LoginResponse> = runApi {
        val device = DeviceIdentity.current()
        val response = client.post("$BASE_URL/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(
                request.copy(
                    deviceId = request.deviceId.ifBlank { device.id },
                    deviceName = request.deviceName.ifBlank { device.name },
                    platform = request.platform.ifBlank { device.platform },
                )
            )
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Google auth")
        }
    }

    suspend fun pollSignIn(challengeId: String): Result<LoginResponse> = runApi {
        val response = client.get("$BASE_URL/auth/sign-in/$challengeId")
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Check sign-in")
    }

    suspend fun incomingSignIn(): Result<SignInPrompt> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.get("$BASE_URL/sign-in/incoming") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Check sign-in")
    }

    suspend fun approveSignIn(challengeId: String): Result<Unit> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.post("$BASE_URL/sign-in/$challengeId/approve") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) Result.success(Unit) else httpFailure(response, "Allow sign-in")
    }

    suspend fun denySignIn(challengeId: String): Result<Unit> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.post("$BASE_URL/sign-in/$challengeId/deny") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) Result.success(Unit) else httpFailure(response, "Block sign-in")
    }

    suspend fun rememberDevice(): Result<Unit> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val device = DeviceIdentity.current()
        val response = client.post("$BASE_URL/devices/hello") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(DeviceHelloRequest(device.id, device.name, device.platform))
        }
        if (response.status.isSuccess()) Result.success(Unit) else httpFailure(response, "Save device")
    }

    suspend fun signedInDevices(): Result<List<SignedInDevice>> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val device = DeviceIdentity.current()
        val response = client.get("$BASE_URL/devices/mine?deviceId=${device.id}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) Result.success(response.body())
        else httpFailure(response, "Load devices")
    }

    private fun LoginRequest.withThisDevice(): LoginRequest {
        val device = DeviceIdentity.current()
        return copy(
            deviceId = deviceId.ifBlank { device.id },
            deviceName = deviceName.ifBlank { device.name },
            platform = platform.ifBlank { device.platform },
        )
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

    suspend fun submitLandlordKyc(request: LandlordKycRequest): Result<User> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.post("$BASE_URL/kyc/landlord") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Submit documents")
        }
    }

    suspend fun submitStudentKyc(request: StudentKycRequest): Result<User> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.post("$BASE_URL/kyc/student") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Submit student ID")
        }
    }

    suspend fun myKyc(): Result<LandlordKycRecord> = runApi {
        val token = SessionManager.token.value ?: return@runApi notSignedIn()
        val response = client.get("$BASE_URL/kyc/mine") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            httpFailure(response, "Load verification")
        }
    }
}
