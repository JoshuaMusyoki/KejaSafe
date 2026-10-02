package com.rotd.havenpad.data.repoImpl

import com.rotd.havenpad.data.UserProfileDataSource
import com.rotd.havenpad.domain.biometric.BiometricAuthenticator
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.domain.storage.SecureStorage

/**
 * Ktor HS256 JWTs while listings/rent hit `:server`.
 *
 * Firebase Auth + Data Connect (paused) would be:
 * ```
 * AuthRepositoryImpl(auth = FirebaseAuth.getInstance(), profiles = profiles)
 * ```
 * Do not mix: Firebase ID tokens are RS256; Ktor verifies HMAC256 → AlgorithmMismatchException.
 */
actual fun createAuthRepository(
    profiles: UserProfileDataSource,
    biometricAuthenticator: BiometricAuthenticator,
    secureStorage: SecureStorage,
): AuthRepository {
    return KtorBridgeAuthRepository(biometricAuthenticator, secureStorage)
}
