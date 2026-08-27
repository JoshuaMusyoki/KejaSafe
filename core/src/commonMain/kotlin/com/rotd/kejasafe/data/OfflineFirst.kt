package com.rotd.kejasafe.data

import com.rotd.kejasafe.error.AppError
import com.rotd.kejasafe.error.AppException
import com.rotd.kejasafe.error.ErrorMessages
import com.rotd.kejasafe.error.toAppError
import com.rotd.kejasafe.network.ConnectivityMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer

/**
 * Result of an offline-first read: prefer cache, then refresh from network.
 */
data class OfflineData<T>(
    val value: T,
    /** True when [value] came from disk (may also be true briefly before a successful sync). */
    val fromCache: Boolean,
    /** Soft warning when cache is shown because the network refresh failed. */
    val syncWarning: String? = null
)

/**
 * Loads cache first (if any), then fetches remote and updates cache.
 *
 * - Network success → fresh data, cache updated, no warning
 * - Network failure + cache → cached data + [ErrorMessages.SYNC_FAILED_CACHED] / offline copy
 * - Network failure + no cache → [Result.failure]
 */
suspend fun <T> offlineFirst(
    readCache: suspend () -> T?,
    writeCache: suspend (T) -> Unit,
    fetch: suspend () -> Result<T>,
    /** Invoked as soon as a cache hit is found (before network). */
    onCacheHit: ((T) -> Unit)? = null
): Result<OfflineData<T>> {
    val cached = try {
        readCache()
    } catch (_: Exception) {
        null
    }
    if (cached != null) {
        onCacheHit?.invoke(cached)
    }

    val remote = try {
        fetch()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AppException.from(e))
    }

    val fresh = remote.getOrNull()
    if (fresh != null) {
        ConnectivityMonitor.reportOnline()
        try {
            writeCache(fresh)
        } catch (_: Exception) {
            // Cache write failures should not fail the request.
        }
        return Result.success(
            OfflineData(
                value = fresh,
                fromCache = false,
                syncWarning = null
            )
        )
    }

    val error = remote.exceptionOrNull() ?: AppException(AppError.Unknown())
    val appError = error.toAppError()
    if (appError is AppError.NoInternet ||
        appError is AppError.Timeout ||
        appError is AppError.ServerUnavailable
    ) {
        ConnectivityMonitor.reportOffline()
    }

    if (cached != null) {
        val warning = when (appError) {
            is AppError.NoInternet, is AppError.Timeout, is AppError.ServerUnavailable ->
                ErrorMessages.OFFLINE_CACHED
            else -> ErrorMessages.SYNC_FAILED_CACHED
        }
        return Result.success(
            OfflineData(
                value = cached,
                fromCache = true,
                syncWarning = warning
            )
        )
    }

    val failure = when (appError) {
        is AppError.NoInternet, is AppError.Timeout ->
            AppException(AppError.OfflineNoCache())
        else -> AppException.from(error)
    }
    return Result.failure(failure)
}

suspend fun <T> offlineFirstSerialized(
    cacheKey: String,
    serializer: KSerializer<T>,
    fetch: suspend () -> Result<T>
): Result<OfflineData<T>> = offlineFirst(
    readCache = { com.rotd.kejasafe.storage.JsonCache.get(cacheKey, serializer) },
    writeCache = { com.rotd.kejasafe.storage.JsonCache.put(cacheKey, it, serializer) },
    fetch = fetch
)
