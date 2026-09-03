package com.rotd.kejasafe.domain.biometric

import com.rotd.kejasafe.error.ErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Foundation.NSError
import kotlin.coroutines.resume

actual fun createBiometricAuthenticator(): BiometricAuthenticator = IosBiometricAuthenticator()

class IosBiometricAuthenticator : BiometricAuthenticator {
    override fun canAuthenticate(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    override suspend fun authenticate(title: String, subtitle: String): Result<Unit> {
        if (!canAuthenticate()) {
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_UNAVAILABLE))
        }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val context = LAContext()
                val reason = listOf(title, subtitle).filter { it.isNotBlank() }.joinToString(". ")
                context.evaluatePolicy(
                    LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                    localizedReason = reason,
                ) { success, error ->
                    if (!cont.isActive) return@evaluatePolicy
                    if (success) {
                        cont.resume(Result.success(Unit))
                    } else {
                        val nsError = error as? NSError
                        val cancelled = nsError?.code == kLAErrorUserCancel ||
                            nsError?.code == kLAErrorSystemCancel ||
                            nsError?.code == kLAErrorAppCancel ||
                            nsError?.code == kLAErrorUserFallback
                        val message = when {
                            cancelled -> ErrorMessages.BIOMETRIC_CANCELLED
                            else -> nsError?.localizedDescription
                                ?: ErrorMessages.BIOMETRIC_FAILED
                        }
                        cont.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            }
        }
    }
}

// LocalAuthentication error codes (LAError)
private const val kLAErrorUserFallback: Long = -3
private const val kLAErrorUserCancel: Long = -2
private const val kLAErrorSystemCancel: Long = -4
private const val kLAErrorAppCancel: Long = -9
