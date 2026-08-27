package com.rotd.kejasafe.error

/**
 * Typed app failures with stable, reusable user messages.
 */
sealed class AppError(
    open val message: String,
    open val canRetry: Boolean = true
) {
    data class NoInternet(
        override val message: String = ErrorMessages.NO_INTERNET
    ) : AppError(message, canRetry = true)

    data class Timeout(
        override val message: String = ErrorMessages.CONNECTION_TIMEOUT
    ) : AppError(message, canRetry = true)

    data class ServerUnavailable(
        override val message: String = ErrorMessages.SERVER_UNAVAILABLE
    ) : AppError(message, canRetry = true)

    data class Server(
        override val message: String = ErrorMessages.SERVER_ERROR,
        val statusCode: Int? = null
    ) : AppError(message, canRetry = true)

    data class Unauthorized(
        override val message: String = ErrorMessages.UNAUTHORIZED
    ) : AppError(message, canRetry = false)

    data class Forbidden(
        override val message: String = ErrorMessages.FORBIDDEN
    ) : AppError(message, canRetry = false)

    data class NotFound(
        override val message: String = ErrorMessages.NOT_FOUND
    ) : AppError(message, canRetry = false)

    data class Conflict(
        override val message: String = ErrorMessages.CONFLICT
    ) : AppError(message, canRetry = true)

    data class Validation(
        override val message: String = ErrorMessages.VALIDATION
    ) : AppError(message, canRetry = false)

    data class NotSignedIn(
        override val message: String = ErrorMessages.NOT_SIGNED_IN
    ) : AppError(message, canRetry = false)

    data class OfflineNoCache(
        override val message: String = ErrorMessages.OFFLINE_NO_CACHE
    ) : AppError(message, canRetry = true)

    data class Unknown(
        override val message: String = ErrorMessages.UNKNOWN,
        val causeMessage: String? = null
    ) : AppError(message, canRetry = true)

    /** Prefer a server-provided message when it is short and human-readable. */
    companion object {
        fun fromHttp(statusCode: Int, body: String? = null, fallbackAction: String? = null): AppError {
            val trimmed = body?.trim().orEmpty()
            val usableBody = trimmed.takeIf {
                it.isNotBlank() &&
                    it.length <= 200 &&
                    !it.startsWith("{") &&
                    !it.startsWith("<")
            }

            return when (statusCode) {
                401 -> Unauthorized(
                    usableBody
                        ?.takeIf { !it.equals("unauthorized", ignoreCase = true) }
                        ?: ErrorMessages.UNAUTHORIZED
                )
                403 -> Forbidden(usableBody ?: ErrorMessages.FORBIDDEN)
                404 -> NotFound(usableBody ?: ErrorMessages.NOT_FOUND)
                408, 504 -> Timeout()
                409 -> Conflict(usableBody ?: ErrorMessages.CONFLICT)
                422, 400 -> Validation(usableBody ?: ErrorMessages.VALIDATION)
                in 500..599 -> Server(
                    message = usableBody ?: ErrorMessages.SERVER_ERROR,
                    statusCode = statusCode
                )
                else -> Unknown(
                    message = usableBody
                        ?: fallbackAction?.let { action ->
                            if (action.contains("fail", ignoreCase = true)) {
                                "$action. Please try again."
                            } else {
                                "$action failed. Please try again."
                            }
                        }
                        ?: ErrorMessages.UNKNOWN,
                    causeMessage = "HTTP $statusCode"
                )
            }
        }
    }
}

class AppException(
    val error: AppError,
    cause: Throwable? = null
) : Exception(error.message, cause) {
    companion object {
        fun from(throwable: Throwable): AppException =
            if (throwable is AppException) throwable
            else AppException(ErrorMapper.map(throwable), throwable)

        fun fromHttp(statusCode: Int, body: String? = null, fallbackAction: String? = null): AppException =
            AppException(AppError.fromHttp(statusCode, body, fallbackAction))

        fun notSignedIn(): AppException = AppException(AppError.NotSignedIn())
    }
}

fun Throwable.toAppError(): AppError =
    (this as? AppException)?.error ?: ErrorMapper.map(this)

fun Throwable.userMessage(): String = toAppError().message

/**
 * Prefer an intentional auth-layer message (e.g. from Firebase mapping) over the
 * generic [ErrorMessages.UNKNOWN] that [userMessage] would otherwise return.
 */
fun Throwable.authUserMessage(): String {
    val direct = message?.trim().orEmpty()
    if (direct.isNotEmpty()) return direct
    return userMessage()
}

fun Throwable.canRetry(): Boolean = toAppError().canRetry
