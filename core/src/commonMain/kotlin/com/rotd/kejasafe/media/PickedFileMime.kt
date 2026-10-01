package com.rotd.kejasafe.media

internal fun mimeTypeForPickedFile(fileName: String, reported: String): String {
    val type = reported.trim()
    if (type.isNotEmpty() && !type.equals("application/octet-stream", ignoreCase = true)) {
        return type
    }
    val name = fileName.lowercase()
    return when {
        name.endsWith(".png") -> "image/png"
        name.endsWith(".webp") -> "image/webp"
        name.endsWith(".gif") -> "image/gif"
        name.endsWith(".pdf") -> "application/pdf"
        else -> "image/jpeg"
    }
}
