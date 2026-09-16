package com.rotd.kejasafe.network

import kotlin.js.ExperimentalWasmJsInterop

/**
 * Same origin when the site is served by a reverse proxy in front of the API, otherwise
 * the API port on the host serving the page. `window.KEJASAFE_API_BASE_URL` wins when set
 * in index.html.
 */
internal actual fun platformApiBaseUrl(): String = jsApiBaseUrl()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        if (typeof window === 'undefined') return 'http://localhost:8080';
        if (window.KEJASAFE_API_BASE_URL) return window.KEJASAFE_API_BASE_URL;
        var location = window.location;
        var sameOrigin = !location.port || location.port === '80' || location.port === '443';
        return sameOrigin
            ? location.protocol + '//' + location.hostname
            : location.protocol + '//' + location.hostname + ':8080';
    }"""
)
private external fun jsApiBaseUrl(): String
