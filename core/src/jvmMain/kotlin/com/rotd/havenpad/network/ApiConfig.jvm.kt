package com.rotd.havenpad.network

internal actual fun platformApiBaseUrl(): String =
    System.getenv("HAVENPAD_API_BASE_URL")?.trim()?.ifBlank { null } ?: ApiConfig.DEV_API_BASE_URL
