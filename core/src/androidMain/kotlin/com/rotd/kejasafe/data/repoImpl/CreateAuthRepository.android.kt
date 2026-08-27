package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.data.UserProfileDataSource
import com.rotd.kejasafe.domain.repository.AuthRepository

/**
 * Ktor HS256 JWTs while listings/rent hit `:server`.
 *
 * Firebase Auth + Data Connect (paused) would be:
 * ```
 * AuthRepositoryImpl(auth = FirebaseAuth.getInstance(), profiles = profiles)
 * ```
 * Do not mix: Firebase ID tokens are RS256; Ktor verifies HMAC256 → AlgorithmMismatchException.
 */
actual fun createAuthRepository(profiles: UserProfileDataSource): AuthRepository {
    return KtorBridgeAuthRepository()
}
