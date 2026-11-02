package com.rotd.havenpad.error

/**
 * Maps raw Throwables / Ktor failures into [AppError].
 */
object ErrorMapper {
    fun map(throwable: Throwable): AppError {
        if (throwable is AppException) return throwable.error

        val message = buildString {
            append(throwable.message.orEmpty())
            throwable.cause?.message?.let { cause ->
                if (isNotEmpty()) append(' ')
                append(cause)
            }
        }.lowercase()

        return when {
            looksLikeNotSignedIn(message) -> AppError.NotSignedIn()
            looksLikeOffline(message) -> AppError.NoInternet()
            looksLikeTimeout(message) -> AppError.Timeout()
            looksLikeServerDown(message) -> AppError.ServerUnavailable()
            else -> AppError.Unknown(
                message = ErrorMessages.UNKNOWN,
                causeMessage = throwable.message
            )
        }
    }

    private fun looksLikeNotSignedIn(message: String): Boolean =
        message.contains("not signed in") ||
            message.contains("unauthorized") ||
            message.contains("401")

    private fun looksLikeOffline(message: String): Boolean =
        message.contains("unable to resolve host") ||
            message.contains("failed to connect") ||
            message.contains("connection refused") ||
            message.contains("network is unreachable") ||
            message.contains("no address associated") ||
            message.contains("software caused connection abort") ||
            message.contains("connection reset") ||
            message.contains("broken pipe") ||
            message.contains("closedchannel") ||
            message.contains("unknownhost") ||
            message.contains("socketexception") ||
            message.contains("connectexception") ||
            message.contains("no route to host") ||
            message.contains("offline") ||
            message.contains("network unreachable")

    private fun looksLikeTimeout(message: String): Boolean =
        message.contains("timeout") ||
            message.contains("timed out") ||
            message.contains("socket timeout")

    private fun looksLikeServerDown(message: String): Boolean =
        message.contains("503") ||
            message.contains("502") ||
            message.contains("bad gateway") ||
            message.contains("service unavailable") ||
            message.contains("connection reset by peer")
}
