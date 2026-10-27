package com.rotd.havenpad.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.ReviewReportStatus
import com.rotd.havenpad.ReviewSubject
import com.rotd.havenpad.StayReview

@Composable
fun AdminReviewReports(
    reports: List<StayReview>,
    busy: Boolean,
    error: String? = null,
    onFollowUp: (String, String) -> Unit,
    onDismiss: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reports.isEmpty()) {
        Text(
            text = error?.takeIf { it.isNotBlank() }
                ?: "No open reports. Open the reviewed house, tap Report review, and send what is wrong.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(8.dp),
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(reports, key = { it.id }) { review ->
            ReportCard(
                review = review,
                busy = busy,
                onFollowUp = { note -> onFollowUp(review.id, note) },
                onDismiss = { onDismiss(review.id) },
                onDelete = { onDelete(review.id) },
            )
        }
    }
}

@Composable
private fun ReportCard(
    review: StayReview,
    busy: Boolean,
    onFollowUp: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    var note by rememberSaveable(review.id) { mutableStateOf(review.followUpNote) }
    val about = if (review.subject == ReviewSubject.STUDENT) {
        "Landlord review of ${review.studentName.ifBlank { "a student" }}"
    } else {
        "Student review of ${review.landlordName.ifBlank { "a landlord" }}"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(about, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "${review.authorName.ifBlank { "Author" }} · ${review.rating}/5 · ${review.listingTitle}",
                style = MaterialTheme.typography.labelMedium,
            )
            if (review.body.isNotBlank()) {
                Text(review.body, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Report: ${review.reportReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            if (review.reportStatus == ReviewReportStatus.FOLLOWED_UP) {
                Text(
                    text = "Follow-up already sent.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What you told ${review.authorName.ifBlank { "the author" }}") },
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onFollowUp(note) },
                    enabled = !busy,
                ) { Text("Save follow-up") }
                OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Keep") }
            }
            Button(
                onClick = onDelete,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete review") }
        }
    }
}
