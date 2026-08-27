package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.AuthResponse
import com.rotd.kejasafe.GoogleAuthRequest
import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.LoginRequest
import com.rotd.kejasafe.RegistrationRequest
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.domain.repository.AuthRepository
import com.rotd.kejasafe.network.AuthClient
import com.rotd.kejasafe.network.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Non-Android fallback that still talks to the Ktor API until those targets
 * get a Firebase Auth / SQL Connect client.
 */
class KtorBridgeAuthRepository : AuthRepository {
    override val currentUser: Flow<User?> = flowOf(SessionManager.currentUser.value)

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        fullName: String,
        role: UserRole,
    ): Result<User> = AuthClient.register(
        RegistrationRequest(email, username, fullName, password, role)
    ).mapSession()

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        AuthClient.login(LoginRequest(email, password)).mapSession()

    override suspend fun signInWithIdentifier(identifier: String, password: String): Result<User> =
        AuthClient.login(LoginRequest(identifier, password)).mapSession()

    override suspend fun signInWithGoogle(idToken: String, role: UserRole): Result<User> =
        AuthClient.googleAuth(GoogleAuthRequest(idToken, role)).mapSession()

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Password reset requires Firebase Auth (Android)."))

    override suspend fun refreshProfile(): Result<User> =
        AuthClient.getProfile().onSuccess { SessionManager.updateUser(it) }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<User> =
        AuthClient.updateProfile(request).onSuccess { SessionManager.updateUser(it) }

    override suspend fun updateProfilePicture(url: String): Result<User> =
        Result.failure(UnsupportedOperationException("Profile picture update not wired on this target."))

    override suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus> =
        Result.failure(UnsupportedOperationException("KYC submit not wired on this target."))

    override suspend fun signOut() {
        SessionManager.clearSession()
    }

    override fun isSignedIn(): Boolean = SessionManager.isLogged()

    private fun Result<AuthResponse>.mapSession(): Result<User> = map {
        SessionManager.saveSession(it.token, it.user)
        it.user
    }
}
