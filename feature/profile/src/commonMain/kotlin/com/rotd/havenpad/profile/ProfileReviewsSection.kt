package com.rotd.havenpad.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.components.ReviewStars
import com.rotd.havenpad.MyReviews
import com.rotd.havenpad.Reputation
import com.rotd.havenpad.ReviewReportStatus
import com.rotd.havenpad.ReviewSubject
import com.rotd.havenpad.StayReview
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.ReviewClient
import kotlinx.coroutines.launch

@Composable
fun ProfileReviewsSection(modifier: Modifier = Modifier) {
    var mine by remember { mutableStateOf<MyReviews?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        ReviewClient.mine().fold(
            onSuccess = { mine = it },
            onFailure = { error = it.userMessage() },
        )
    }

    val about = mine?.aboutMe
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (about?.subject == ReviewSubject.STUDENT) "Student reviews" else "Landlord reviews",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = reputationHeadline(about),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        mine?.followUps?.forEach { review ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "HavenPad followed up on your review of ${review.listingTitle.ifBlank { "a house" }}: ${review.followUpNote}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        about?.reviews?.forEach { review ->
            ProfileReviewCard(
                review = review,
                onReply = { text ->
                    scope.launch {
                        ReviewClient.reply(review.id, text).fold(
                            onSuccess = { refresh++ },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
                onReport = { reason ->
                    scope.launch {
                        ReviewClient.report(review.id, reason).fold(
                            onSuccess = { refresh++ },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileReviewCard(
    review: StayReview,
    onReply: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    var reply by remember(review.id, review.reply) { mutableStateOf(review.reply) }
    var reason by remember(review.id) { mutableStateOf("") }
    var reporting by remember(review.id) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = review.authorName.ifBlank { "HavenPad user" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                ReviewStars(review.rating)
            }
            if (review.listingTitle.isNotBlank()) {
                Text(review.listingTitle, style = MaterialTheme.typography.labelMedium)
            }
            if (review.body.isNotBlank()) {
                Text(review.body, style = MaterialTheme.typography.bodyMedium)
            }
            if (review.reply.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Reply") },
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onReply(reply) }),
                    )
                    IconButton(onClick = { onReply(reply) }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
                    }
                }
            } else {
                Text(
                    text = "Your reply: ${review.reply}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (review.reportStatus == ReviewReportStatus.OPEN ||
                review.reportStatus == ReviewReportStatus.FOLLOWED_UP
            ) {
                Text(
                    text = "Reported to HavenPad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                TextButton(onClick = { reporting = !reporting }) { Text("Report review") }
                if (reporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth().imePadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("What is wrong?") },
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onReport(reason) }),
                        )
                        IconButton(onClick = { onReport(reason) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send report")
                        }
                    }
                }
            }
        }
    }
}

internal fun reputationHeadline(about: Reputation?): String = when {
    about == null -> "Loading your rating…"
    about.reviewCount <= 0 -> "No reviews yet."
    else -> buildString {
        append("${about.percent}%")
        about.rank?.let { append(" · Rank #${it}") }
        append(" · ${about.reviewCount} review")
        if (about.reviewCount != 1) append("s")
    }
}
