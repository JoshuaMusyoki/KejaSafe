package com.rotd.havenpad.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.web.LandingViewModel
import com.rotd.havenpad.web.StoryPostState
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingTextDark
import com.rotd.havenpad.web.theme.LandingTextSecondary

/**
 * Where a signed-in student or landlord writes the story that lands on this page.
 * Visitors get a sign-in prompt instead — nothing here is hardcoded copy.
 */
@Composable
fun StoryComposer(
    isSignedIn: Boolean,
    canPost: Boolean,
    authorName: String?,
    postState: StoryPostState,
    onPostStory: (String, Int) -> Unit,
    onSignIn: () -> Unit,
    onPostStateConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            when {
                !isSignedIn -> SignInPrompt(onSignIn)
                !canPost -> Text(
                    "Stories come from students and landlords on the platform. " +
                        "Admin accounts moderate them instead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LandingTextSecondary
                )
                else -> ComposerForm(
                    authorName = authorName,
                    postState = postState,
                    onPostStory = onPostStory,
                    onPostStateConsumed = onPostStateConsumed
                )
            }
        }
    }
}

@Composable
private fun SignInPrompt(onSignIn: () -> Unit) {
    Column {
        Text(
            "Housed through HavenPad?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LandingTextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in with your student or landlord account to post your own story. " +
                "Only real accounts can publish here.",
            style = MaterialTheme.typography.bodyMedium,
            color = LandingTextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Sign in to share your story")
        }
    }
}

@Composable
private fun ComposerForm(
    authorName: String?,
    postState: StoryPostState,
    onPostStory: (String, Int) -> Unit,
    onPostStateConsumed: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    val submitting = postState is StoryPostState.Submitting

    LaunchedEffect(postState) {
        if (postState is StoryPostState.Posted) {
            content = ""
            rating = 5
        }
    }

    Column {
        Text(
            authorName?.let { "Share your story, ${it.substringBefore(' ')}" }
                ?: "Share your story",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LandingTextDark
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your rating",
                style = MaterialTheme.typography.bodySmall,
                color = LandingTextSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { index ->
                    val selected = index < rating
                    Icon(
                        imageVector = if (selected) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "${index + 1} stars",
                        tint = if (selected) Color(0xFFF59E0B) else LandingTextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(enabled = !submitting) { rating = index + 1 }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { updated ->
                if (updated.length <= LandingViewModel.MAX_STORY_LENGTH) {
                    content = updated
                }
                if (postState !is StoryPostState.Idle && postState !is StoryPostState.Submitting) {
                    onPostStateConsumed()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            enabled = !submitting,
            placeholder = { Text("How did HavenPad change your housing search?") },
            supportingText = {
                Text(
                    "${content.length}/${LandingViewModel.MAX_STORY_LENGTH} — " +
                        "at least ${LandingViewModel.MIN_STORY_LENGTH} characters",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            isError = postState is StoryPostState.Failed,
            shape = RoundedCornerShape(12.dp),
            minLines = 4
        )

        when (postState) {
            is StoryPostState.Failed -> {
                Text(
                    postState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            is StoryPostState.Posted -> {
                Text(
                    "Posted — thanks for helping other students.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LandingBrandGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            else -> Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { onPostStory(content, rating) },
            enabled = !submitting && content.trim().length >= LandingViewModel.MIN_STORY_LENGTH,
            colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (submitting) "Posting…" else "Post my story")
        }
    }
}
