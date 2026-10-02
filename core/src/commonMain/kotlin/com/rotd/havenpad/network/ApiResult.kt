package com.rotd.havenpad.network

import com.rotd.havenpad.error.AppException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

/**
 * Shared Result helpers so API clients return typed [AppException]s
 * instead of raw platform exceptions / status dumps.
 */
internal suspend fun <T> runApi(
    block: suspend () -> Result<T>
): Result<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(AppException.from(e))
}

internal fun <T> notSignedIn(): Result<T> =
    Result.failure(AppException.notSignedIn())

internal suspend fun <T> httpFailure(
    response: HttpResponse,
    action: String
): Result<T> {
    val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
    return Result.failure(
        AppException.fromHttp(
            statusCode = response.status.value,
            body = body,
            fallbackAction = action
        )
    )
}

internal fun <T> httpFailure(
    status: HttpStatusCode,
    body: String,
    action: String
): Result<T> = Result.failure(
    AppException.fromHttp(
        statusCode = status.value,
        body = body,
        fallbackAction = action
    )
)
