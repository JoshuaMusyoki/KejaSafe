package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.domain.biometric.FakeBiometricAuthenticator
import com.rotd.kejasafe.domain.storage.MemorySecureStorage
import com.rotd.kejasafe.error.ErrorMessages
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtorBridgeAuthRepositoryBiometricTest {
    @Test
    fun biometricLoginRequiresPriorEnable() = runTest {
        val repository = KtorBridgeAuthRepository(
            biometricAuthenticator = FakeBiometricAuthenticator(),
            secureStorage = MemorySecureStorage(),
        )

        val result = repository.signInWithBiometrics()

        assertTrue(result.isFailure)
        assertEquals(ErrorMessages.BIOMETRIC_NOT_ENABLED, result.exceptionOrNull()?.message)
        assertFalse(repository.isBiometricEnabled())
    }

    @Test
    fun enableThenDisableClearsBiometricFlag() = runTest {
        val repository = KtorBridgeAuthRepository(
            biometricAuthenticator = FakeBiometricAuthenticator(),
            secureStorage = MemorySecureStorage(),
        )

        val enabled = repository.enableBiometricLogin("ada@kejasafe.test", "secret")
        assertTrue(enabled.isSuccess)
        assertTrue(repository.isBiometricEnabled())

        repository.disableBiometricLogin()
        assertFalse(repository.isBiometricEnabled())
    }
}
