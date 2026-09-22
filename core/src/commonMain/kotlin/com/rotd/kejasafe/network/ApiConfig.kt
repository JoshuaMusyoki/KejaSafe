package com.rotd.kejasafe.network

/**
 * Where the API lives for the current platform.
 *
 * Mobile and desktop builds compile the address in, while the browser derives it from the
 * page it was served from (or `window.KEJASAFE_API_BASE_URL` when the API is hosted
 * separately), so the web app talks to a real server instead of a hardcoded host.
 */
object ApiConfig {
    /**
     * Physical devices on the same Wi-Fi as the dev machine.
     *
     * Prefer `adb reverse tcp:8080 tcp:8080` over editing this: it tunnels the device's
     * own port 8080 to the dev machine over USB, which keeps working on guest Wi-Fi,
     * behind a VPN, or when the laptop's IP changes. See [USB_API_BASE_URL].
     */
    //const val DEV_API_BASE_URL = "http://10.8.121.71:8080"
    //const val DEV_API_BASE_URL = "http://192.168.100.3:8080"

    const val DEV_API_BASE_URL = "http://10.8.123.173:8080"
    /** Reaches the dev machine through `adb reverse` on Android. */
    const val USB_API_BASE_URL = "http://127.0.0.1:8080"

    /** The Android emulator sees its host on this address, never on localhost. */
    const val EMULATOR_API_BASE_URL = "http://10.0.2.2:8080"

    private var override: String? = null

    val baseUrl: String
        get() = (override ?: platformApiBaseUrl()).trimEnd('/')

    /** Point the clients at another server at runtime (tests, staging, self-hosted). */
    fun useBaseUrl(url: String) {
        override = url.trim().ifBlank { null }
    }
}

internal expect fun platformApiBaseUrl(): String
