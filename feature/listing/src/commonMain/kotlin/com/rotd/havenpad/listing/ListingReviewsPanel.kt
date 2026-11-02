package com.rotd.havenpad.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.components.ReviewStars
import com.rotd.havenpad.ListingReviewPage
import com.rotd.havenpad.ReviewComposer
import com.rotd.havenpad.ReviewReportStatus
import com.rotd.havenpad.ReviewSubject
import com.rotd.havenpad.StayReview
import com.rotd.havenpad.SubmitReviewRequest
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.ReviewClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.launch

@Composable
fun ListingReviewsPanel(
    listingId: String,
    modifier: Modifier = Modifier,
) {
    var page by remember(listingId) { mutableStateOf<ListingReviewPage?>(null) }
    var composer by remember(listingId) { mutableStateOf<ReviewComposer?>(null) }
    var error by remember(listingId) { mutableStateOf<String?>(null) }
    var info by remember(listingId) { mutableStateOf<String?>(null) }
    var refresh by remember(listingId) { mutableIntStateOf(0) }
    var studentId by remember(listingId) { mutableStateOf("") }
    val signedIn = SessionManager.currentUser.value != null
    val scope = rememberCoroutineScope()

    LaunchedEffect(listingId, refresh) {
        ReviewClient.listingReviews(listingId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.userMessage() },
        )
        if (SessionManager.token.value != null) {
            ReviewClient.composer(listingId).onSuccess { composer = it }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Landlord reviews",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        val loaded = page
        Text(
            text = when {
                loaded == null -> "Loading reviews…"
                loaded.reviewCount <= 0 -> "No reviews for this house yet."
                else -> buildString {
                    append("${loaded.percent}%")
                    loaded.rank?.let { append(" · Rank #$it") }
                    append(" · ${loaded.reviewCount} review")
                    if (loaded.reviewCount != 1) append("s")
                    append(" on this house")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        loaded?.reviews?.forEach { review ->
            ReviewCard(
                review = review,
                canReply = composer?.isHouseLandlord == true,
                canReport = signedIn && review.authorId != SessionManager.currentUser.value?.id,
                onReply = { text ->
                    scope.launch {
                        ReviewClient.reply(review.id, text).fold(
                            onSuccess = {
                                info = "Reply saved."
                                refresh++
                            },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
                onReport = { reason ->
                    scope.launch {
                        ReviewClient.report(review.id, reason).fold(
                            onSuccess = {
                                info = "Reported. HavenPad will follow up with ${review.authorName.ifBlank { "the student" }}."
                                error = null
                                refresh++
                            },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
            )
        }
        val form = composer
        if (form?.canReviewHouse == true) {
            ReviewForm(
                title = "Rate this landlord",
                action = "Publish review",
                onSubmit = { rating, body ->
                    scope.launch {
                        ReviewClient.submit(
                            SubmitReviewRequest(
                                listingId = listingId,
                                subject = ReviewSubject.LANDLORD,
                                rating = rating,
                                body = body,
                            )
                        ).fold(
                            onSuccess = {
                                info = "Review published."
                                error = null
                                refresh++
                            },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
            )
        } else if (!signedIn) {
            Text(
                text = "Sign in to review this landlord after a confirmed stay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        form?.myHouseReview?.followUpNote?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = "HavenPad follow-up: $note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val students = form?.students.orEmpty()
        LaunchedEffect(students) {
            if (studentId.isBlank()) {
                students.firstOrNull()?.let { studentId = it.studentId }
            }
        }
        if (students.isNotEmpty()) {
            Text(
                text = "Review a student who stayed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                students.forEach { student ->
                    TextButton(onClick = { studentId = student.studentId }) {
                        Text(
                            student.studentName.ifBlank { "Student" },
                            fontWeight = if (student.studentId == studentId) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
            ReviewForm(
                title = "Stars for this student",
                action = "Save student review",
                onSubmit = { rating, body ->
                    scope.launch {
                        ReviewClient.submit(
                            SubmitReviewRequest(
                                listingId = listingId,
                                subject = ReviewSubject.STUDENT,
                                studentId = studentId,
                                rating = rating,
                                body = body,
                            )
                        ).fold(
                            onSuccess = {
                                info = "Student review saved."
                                error = null
                                refresh++
                            },
                            onFailure = { error = it.userMessage() },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: StayReview,
    canReply: Boolean,
    canReport: Boolean,
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
                    text = review.authorName.ifBlank { "Student" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                ReviewStars(review.rating)
            }
            if (review.body.isNotBlank()) {
                Text(review.body, style = MaterialTheme.typography.bodyMedium)
            }
            if (review.reply.isNotBlank()) {
                Text(
                    text = "Reply: ${review.reply}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (canReply) {
                CommentField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = "Reply to this review",
                    onSend = { onReply(reply) },
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
            } else if (canReport) {
                TextButton(onClick = { reporting = !reporting }) { Text("Report review") }
                if (reporting) {
                    CommentField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = "What is wrong?",
                        onSend = { onReport(reason) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewForm(
    title: String,
    action: String,
    onSubmit: (rating: Int, body: String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(5) }
    var body by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        ReviewStars(rating = rating, onSelect = { rating = it })
        CommentField(
            value = body,
            onValueChange = { body = it },
            label = "Comment",
            sendLabel = action,
            onSend = { onSubmit(rating, body) },
        )
    }
}

@Composable
private fun CommentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSend: () -> Unit,
    sendLabel: String = "Send",
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { event ->
                    if (event.isFocused) {
                        scope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
            label = { Text(label) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        IconButton(onClick = onSend) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = sendLabel)
        }
    }
}
