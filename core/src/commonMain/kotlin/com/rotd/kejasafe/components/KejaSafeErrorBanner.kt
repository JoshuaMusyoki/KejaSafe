package com.rotd.kejasafe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.error.AppError
import com.rotd.kejasafe.error.ErrorMessages
import com.rotd.kejasafe.error.toAppError

/**
 * Full-width soft notice when showing cached data after a failed refresh.
 */
@Composable
fun KejaSafeOfflineBanner(
    message: String = ErrorMessages.OFFLINE_CACHED,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        if (onRetry != null) {
            KejaSafeOutlinedButton(
                text = "Retry",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Hard error state when there is no cached data to show.
 */
@Composable
fun KejaSafeErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val resolvedTitle = title ?: defaultTitleFor(message)
    val icon = when {
        message == ErrorMessages.NO_INTERNET ||
            message == ErrorMessages.OFFLINE_NO_CACHE ||
            message.contains("internet", ignoreCase = true) ||
            message.contains("offline", ignoreCase = true) -> Icons.Outlined.WifiOff
        else -> Icons.Outlined.ErrorOutline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        if (onRetry != null) {
            KejaSafePrimaryButton(
                text = "Retry",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KejaSafeErrorState(
    error: Throwable,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val appError = error.toAppError()
    KejaSafeErrorState(
        message = appError.message,
        title = titleFor(appError),
        modifier = modifier,
        onRetry = if (appError.canRetry) onRetry else null
    )
}

private fun titleFor(error: AppError): String = when (error) {
    is AppError.NoInternet, is AppError.OfflineNoCache -> "No connection"
    is AppError.Timeout -> "Request timed out"
    is AppError.ServerUnavailable, is AppError.Server -> "Server issue"
    is AppError.Unauthorized, is AppError.NotSignedIn -> "Sign in required"
    is AppError.Forbidden -> "Access denied"
    is AppError.NotFound -> "Not found"
    is AppError.Conflict -> "Conflict"
    is AppError.Validation -> "Check your details"
    is AppError.Unknown -> "Something went wrong"
}

private fun defaultTitleFor(message: String): String = when {
    message.contains("internet", ignoreCase = true) ||
        message.contains("offline", ignoreCase = true) ||
        message.contains("connection", ignoreCase = true) -> "No connection"
    message.contains("timeout", ignoreCase = true) -> "Request timed out"
    message.contains("server", ignoreCase = true) -> "Server issue"
    message.contains("sign in", ignoreCase = true) ||
        message.contains("session", ignoreCase = true) -> "Sign in required"
    else -> "Something went wrong"
}
