package com.rotd.havenpad.domain.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rotd.havenpad.error.ErrorMessages
import com.rotd.havenpad.storage.AndroidActivityHolder
import com.rotd.havenpad.storage.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual fun createBiometricAuthenticator(): BiometricAuthenticator = AndroidBiometricAuthenticator()

class AndroidBiometricAuthenticator : BiometricAuthenticator {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    override fun canAuthenticate(): Boolean {
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return false
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(title: String, subtitle: String): Result<Unit> {
        if (!canAuthenticate()) {
            return Result.failure(IllegalStateException(ErrorMessages.BIOMETRIC_UNAVAILABLE))
        }
        return withContext(Dispatchers.Main) {
            val activity = AndroidActivityHolder.current
                ?: return@withContext Result.failure(
                    IllegalStateException(ErrorMessages.BIOMETRIC_UNAVAILABLE),
                )
            prompt(activity, title, subtitle)
        }
    }

    private suspend fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(Result.success(Unit))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!cont.isActive) return
                val message = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED,
                    -> ErrorMessages.BIOMETRIC_CANCELLED
                    else -> errString.toString().ifBlank { ErrorMessages.BIOMETRIC_FAILED }
                }
                cont.resume(Result.failure(IllegalStateException(message)))
            }

            override fun onAuthenticationFailed() {
                // Called for a rejected attempt while the prompt stays open.
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
