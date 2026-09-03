package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.data.UserProfileDataSource
import com.rotd.kejasafe.domain.biometric.BiometricAuthenticator
import com.rotd.kejasafe.domain.repository.AuthRepository
import com.rotd.kejasafe.domain.storage.SecureStorage

actual fun createAuthRepository(
    profiles: UserProfileDataSource,
    biometricAuthenticator: BiometricAuthenticator,
    secureStorage: SecureStorage,
): AuthRepository {
    return KtorBridgeAuthRepository(biometricAuthenticator, secureStorage)
}
