package com.rotd.kejasafe.network

import android.os.Build

/**
 * Emulators reach the host machine on a dedicated address, never on localhost.
 *
 * Physical devices use [ApiConfig.DEV_API_BASE_URL] over the same Wi-Fi as the
 * laptop. Prefer `adb reverse tcp:8080 tcp:8080` + [ApiConfig.USB_API_BASE_URL]
 * when USB debugging works and the laptop IP keeps changing.
 */
internal actual fun platformApiBaseUrl(): String =
    if (isEmulator) ApiConfig.EMULATOR_API_BASE_URL else ApiConfig.DEV_API_BASE_URL

private val isEmulator: Boolean
    get() = Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
        Build.FINGERPRINT.contains("vbox", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk", ignoreCase = true)
