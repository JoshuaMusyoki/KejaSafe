package com.rotd.havenpad.domain.biometric

import com.rotd.havenpad.domain.storage.MemorySecureStorage
import com.rotd.havenpad.error.ErrorMessages
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeBiometricAuthenticator(
    var available: Boolean = true,
    var authenticateResult: Result<Unit> = Result.success(Unit),
    var authenticateCalls: Int = 0,
) : BiometricAuthenticator {
    override fun canAuthenticate(): Boolean = available

    override suspend fun authenticate(title: String, subtitle: String): Result<Unit> {
        authenticateCalls++
        return authenticateResult
    }
}

class BiometricAuthSupportTest {
    @Test
    fun enableStoresCredentialsAfterSuccessfulBiometric() = runTest {
        val authenticator = FakeBiometricAuthenticator()
        val storage = MemorySecureStorage()
        val support = BiometricAuthSupport(authenticator, storage)

        val result = support.enable("ada@havenpad.test", "secret")

        assertTrue(result.isSuccess)
        assertTrue(support.isEnabled())
        assertEquals(1, authenticator.authenticateCalls)
        assertEquals("ada@havenpad.test", storage.getString(BiometricAuthSupport.KEY_IDENTIFIER))
        assertEquals("secret", storage.getString(BiometricAuthSupport.KEY_PASSWORD))
    }

    @Test
    fun enableFailsWhenBiometricUnavailable() = runTest {
        val support = BiometricAuthSupport(
            FakeBiometricAuthenticator(available = false),
            MemorySecureStorage(),
        )

        val result = support.enable("ada@havenpad.test", "secret")

        assertTrue(result.isFailure)
        assertEquals(ErrorMessages.BIOMETRIC_UNAVAILABLE, result.exceptionOrNull()?.message)
        assertFalse(support.isEnabled())
    }

    @Test
    fun enableDoesNotStoreWhenBiometricFails() = runTest {
        val authenticator = FakeBiometricAuthenticator(
            authenticateResult = Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_FAILED)),
        )
        val support = BiometricAuthSupport(authenticator, MemorySecureStorage())

        val result = support.enable("ada@havenpad.test", "secret")

        assertTrue(result.isFailure)
        assertFalse(support.isEnabled())
    }

    @Test
    fun unlockReturnsCredentialsAfterSuccessfulPrompt() = runTest {
        val storage = MemorySecureStorage()
        val support = BiometricAuthSupport(FakeBiometricAuthenticator(), storage)
        support.enable("ada", "pw")

        val unlocked = support.unlockCredentials()

        assertEquals("ada" to "pw", unlocked.getOrThrow())
    }

    @Test
    fun unlockFailsWhenNotEnabled() = runTest {
        val support = BiometricAuthSupport(FakeBiometricAuthenticator(), MemorySecureStorage())

        val result = support.unlockCredentials()

        assertTrue(result.isFailure)
        assertEquals(ErrorMessages.BIOMETRIC_NOT_ENABLED, result.exceptionOrNull()?.message)
    }

    @Test
    fun disableClearsStoredCredentials() = runTest {
        val storage = MemorySecureStorage()
        val support = BiometricAuthSupport(FakeBiometricAuthenticator(), storage)
        support.enable("ada", "pw")

        support.disable()

        assertFalse(support.isEnabled())
        assertEquals(null, storage.getString(BiometricAuthSupport.KEY_IDENTIFIER))
        assertEquals(null, storage.getString(BiometricAuthSupport.KEY_PASSWORD))
    }
}
