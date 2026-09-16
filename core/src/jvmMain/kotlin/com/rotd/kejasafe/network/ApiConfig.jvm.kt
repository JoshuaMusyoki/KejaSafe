package com.rotd.kejasafe.network

internal actual fun platformApiBaseUrl(): String =
    System.getenv("KEJASAFE_API_BASE_URL")?.trim()?.ifBlank { null } ?: ApiConfig.DEV_API_BASE_URL
