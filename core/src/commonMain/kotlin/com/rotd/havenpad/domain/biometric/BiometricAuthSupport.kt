package com.rotd.havenpad.domain.biometric

import com.rotd.havenpad.domain.storage.SecureStorage
import com.rotd.havenpad.error.ErrorMessages

/**
 * Stores identifier + password only after the user opts in, gated by a biometric check.
 */
class BiometricAuthSupport(
    private val authenticator: BiometricAuthenticator,
    private val storage: SecureStorage,
) {
    fun isAvailable(): Boolean = authenticator.canAuthenticate()

    fun isEnabled(): Boolean =
        storage.getString(KEY_ENABLED) == VALUE_TRUE &&
            !storage.getString(KEY_IDENTIFIER).isNullOrBlank() &&
            !storage.getString(KEY_PASSWORD).isNullOrBlank()

    suspend fun enable(identifier: String, password: String): Result<Unit> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_UNAVAILABLE))
        }
        val trimmedId = identifier.trim()
        if (trimmedId.isBlank() || password.isBlank()) {
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_NEED_PASSWORD))
        }
        authenticator.authenticate(
            title = "Enable biometric login",
            subtitle = "Confirm it is you to save login on this device",
        ).onFailure { return Result.failure(it) }
        storage.putString(KEY_IDENTIFIER, trimmedId)
        storage.putString(KEY_PASSWORD, password)
        storage.putString(KEY_ENABLED, VALUE_TRUE)
        return Result.success(Unit)
    }

    fun disable() {
        storage.putString(KEY_IDENTIFIER, null)
        storage.putString(KEY_PASSWORD, null)
        storage.putString(KEY_ENABLED, null)
    }

    suspend fun unlockCredentials(): Result<Pair<String, String>> {
        if (!isEnabled()) {
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_NOT_ENABLED))
        }
        authenticator.authenticate().onFailure { return Result.failure(it) }
        val identifier = storage.getString(KEY_IDENTIFIER)
        val password = storage.getString(KEY_PASSWORD)
        if (identifier.isNullOrBlank() || password.isNullOrBlank()) {
            disable()
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_NOT_ENABLED))
        }
        return Result.success(identifier to password)
    }

    companion object {
        const val KEY_IDENTIFIER = "biometric_identifier"
        const val KEY_PASSWORD = "biometric_password"
        const val KEY_ENABLED = "biometric_enabled"
        private const val VALUE_TRUE = "true"
    }
}
