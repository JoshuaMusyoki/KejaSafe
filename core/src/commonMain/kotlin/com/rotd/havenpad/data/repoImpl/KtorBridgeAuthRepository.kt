package com.rotd.havenpad.data.repoImpl

import com.rotd.havenpad.AuthResponse
import com.rotd.havenpad.LoginResponse
import com.rotd.havenpad.auth.SignInAwaitingApproval
import com.rotd.havenpad.GoogleAuthRequest
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.LandlordKycRequest
import com.rotd.havenpad.StudentKycRequest
import com.rotd.havenpad.LoginRequest
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.domain.biometric.BiometricAuthSupport
import com.rotd.havenpad.domain.biometric.BiometricAuthenticator
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.domain.storage.SecureStorage
import com.rotd.havenpad.network.AuthClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Non-Android fallback that still talks to the Ktor API until those targets
 * get a Firebase Auth / SQL Connect client.
 */
class KtorBridgeAuthRepository(
    biometricAuthenticator: BiometricAuthenticator,
    secureStorage: SecureStorage,
) : AuthRepository {
    private val biometricSupport = BiometricAuthSupport(biometricAuthenticator, secureStorage)
    override val currentUser: Flow<User?> = flowOf(SessionManager.currentUser.value)

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        fullName: String,
        role: UserRole,
        details: RegistrationRequest?,
    ): Result<User> = AuthClient.register(
        details ?: RegistrationRequest(email, username, fullName, password, role)
    ).mapSession()

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        AuthClient.login(LoginRequest(email, password)).mapLogin()

    override suspend fun signInWithIdentifier(identifier: String, password: String): Result<User> =
        AuthClient.login(LoginRequest(identifier, password)).mapLogin()

    override suspend fun signInWithGoogle(idToken: String, role: UserRole): Result<User> =
        AuthClient.googleAuth(GoogleAuthRequest(idToken, role)).mapLogin()

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

    override suspend fun submitLandlordKyc(request: LandlordKycRequest): Result<User> =
        AuthClient.submitLandlordKyc(request).onSuccess { SessionManager.updateUser(it) }

    override suspend fun submitStudentKyc(request: StudentKycRequest): Result<User> =
        AuthClient.submitStudentKyc(request).onSuccess { SessionManager.updateUser(it) }

    override suspend fun signOut() {
        SessionManager.clearSession()
    }

    override fun isSignedIn(): Boolean = SessionManager.isLogged()

    override fun isBiometricAvailable(): Boolean = biometricSupport.isAvailable()

    override fun isBiometricEnabled(): Boolean = biometricSupport.isEnabled()

    override suspend fun enableBiometricLogin(identifier: String, password: String): Result<Unit> =
        biometricSupport.enable(identifier, password)

    override suspend fun disableBiometricLogin() {
        biometricSupport.disable()
    }

    override suspend fun signInWithBiometrics(): Result<User> {
        val credentials = biometricSupport.unlockCredentials().getOrElse { return Result.failure(it) }
        return signInWithIdentifier(credentials.first, credentials.second)
    }

    private fun Result<AuthResponse>.mapSession(): Result<User> = map {
        SessionManager.saveSession(it.token, it.user)
        it.user
    }

    private fun Result<LoginResponse>.mapLogin(): Result<User> = fold(
        onSuccess = { body ->
            when {
                body.pending -> Result.failure(
                    SignInAwaitingApproval(body.challengeId, body.deviceName.ifBlank { "Another device" })
                )
                body.denied || body.user == null || body.token.isBlank() ->
                    Result.failure(IllegalStateException("This sign-in was blocked."))
                else -> {
                    SessionManager.saveSession(body.token, body.user)
                    Result.success(body.user)
                }
            }
        },
        onFailure = { Result.failure(it) },
    )
}
