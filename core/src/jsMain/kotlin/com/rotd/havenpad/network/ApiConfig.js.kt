package com.rotd.havenpad.network

import kotlinx.browser.window

/**
 * Same origin when the site is served by a reverse proxy in front of the API, otherwise
 * the API port on the host serving the page. `window.HAVENPAD_API_BASE_URL` wins when set
 * in index.html.
 */
internal actual fun platformApiBaseUrl(): String {
    val configured = jsConfiguredApiBaseUrl()
    if (!configured.isNullOrBlank()) return configured
    val location = window.location
    val port = location.port
    return if (port.isBlank() || port == "80" || port == "443") {
        "${location.protocol}//${location.hostname}"
    } else {
        "${location.protocol}//${location.hostname}:8080"
    }
}

private fun jsConfiguredApiBaseUrl(): String? =
    js("(typeof window !== 'undefined' && window.HAVENPAD_API_BASE_URL) ? window.HAVENPAD_API_BASE_URL : null")
        .unsafeCast<String?>()
