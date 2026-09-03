package com.rotd.kejasafe.storage

import androidx.fragment.app.FragmentActivity

/**
 * Current [FragmentActivity] for [androidx.biometric.BiometricPrompt].
 * Bound from the app [android.app.Activity] lifecycle.
 */
object AndroidActivityHolder {
    @Volatile
    var current: FragmentActivity? = null
}
