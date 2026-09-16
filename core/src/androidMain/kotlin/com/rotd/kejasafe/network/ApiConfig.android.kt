package com.rotd.kejasafe.network

import android.os.Build

/**
 * Emulators reach the host machine on a dedicated address, never on localhost.
 *
 * A physical device goes through `adb reverse tcp:8080 tcp:8080`, which tunnels the
 * device's port 8080 to the dev machine over USB. Swap the else branch to
 * [ApiConfig.DEV_API_BASE_URL] to talk to the dev machine over Wi-Fi instead.
 */
internal actual fun platformApiBaseUrl(): String =
    if (isEmulator) ApiConfig.EMULATOR_API_BASE_URL else ApiConfig.USB_API_BASE_URL

private val isEmulator: Boolean
    get() = Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
        Build.FINGERPRINT.contains("vbox", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk", ignoreCase = true)
