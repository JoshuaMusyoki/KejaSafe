package com.rotd.havenpad.data.repoImpl

import com.rotd.havenpad.data.UserProfileDataSource
import com.rotd.havenpad.domain.biometric.BiometricAuthenticator
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.domain.storage.SecureStorage

actual fun createAuthRepository(
    profiles: UserProfileDataSource,
    biometricAuthenticator: BiometricAuthenticator,
    secureStorage: SecureStorage,
): AuthRepository {
    return KtorBridgeAuthRepository(biometricAuthenticator, secureStorage)
}
