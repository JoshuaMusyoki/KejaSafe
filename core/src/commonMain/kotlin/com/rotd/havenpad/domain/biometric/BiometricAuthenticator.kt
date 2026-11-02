package com.rotd.havenpad.domain.biometric

/**
 * Device owner verification via fingerprint / Face ID.
 * Does not itself sign the user in; callers unlock stored credentials after success.
 */
interface BiometricAuthenticator {
    fun canAuthenticate(): Boolean

    suspend fun authenticate(
        title: String = "Sign in to HavenPad",
        subtitle: String = "Confirm it is you",
    ): Result<Unit>
}

expect fun createBiometricAuthenticator(): BiometricAuthenticator

class UnsupportedBiometricAuthenticator : BiometricAuthenticator {
    override fun canAuthenticate(): Boolean = false

    override suspend fun authenticate(title: String, subtitle: String): Result<Unit> =
        Result.failure(IllegalStateException("Biometrics are not available on this device."))
}
