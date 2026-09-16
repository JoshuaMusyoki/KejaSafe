package com.rotd.kejasafe.web.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.CommunityStory
import com.rotd.kejasafe.PlatformStats
import com.rotd.kejasafe.web.StoryPostState
import com.rotd.kejasafe.web.formatCount
import com.rotd.kejasafe.web.formatRating
import com.rotd.kejasafe.web.theme.LandingTextSecondary
import com.rotd.kejasafe.web.theme.WebBrandGreen

/** Real stories from `GET /stories`, with the composer that feeds them. */
@Composable
fun LandingTestimonials(
    wide: Boolean,
    horizontalPadding: Dp,
    stories: List<CommunityStory>,
    stats: PlatformStats?,
    loading: Boolean,
    error: String?,
    currentUserId: String?,
    isSignedIn: Boolean,
    canPost: Boolean,
    authorName: String?,
    postState: StoryPostState,
    onPostStory: (String, Int) -> Unit,
    onDeleteStory: (String) -> Unit,
    onSignIn: () -> Unit,
    onPostStateConsumed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "TESTIMONIALS",
            style = MaterialTheme.typography.labelLarge,
            color = WebBrandGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Real Stories from Verified\nStudents and Landlords",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (stats != null && stats.stories > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "${formatCount(stats.stories)} stories • ${formatRating(stats.averageRating)} average rating",
                style = MaterialTheme.typography.bodyMedium,
                color = LandingTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        when {
            loading && stories.isEmpty() -> Text(
                "Loading stories…",
                style = MaterialTheme.typography.bodyMedium,
                color = LandingTextSecondary
            )

            stories.isEmpty() -> Text(
                error ?: "No stories yet — be the first student or landlord to post one.",
                style = MaterialTheme.typography.bodyMedium,
                color = LandingTextSecondary,
                textAlign = TextAlign.Center
            )

            wide -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                stories.take(3).forEach { story ->
                    TestimonialCard(
                        story = story,
                        modifier = Modifier.weight(1f),
                        onDelete = if (story.authorId == currentUserId) {
                            { onDeleteStory(story.id) }
                        } else null
                    )
                }
                repeat(3 - stories.take(3).size) { Spacer(modifier = Modifier.weight(1f)) }
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                stories.take(2).forEach { story ->
                    TestimonialCard(
                        story = story,
                        modifier = Modifier.fillMaxWidth(),
                        onDelete = if (story.authorId == currentUserId) {
                            { onDeleteStory(story.id) }
                        } else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        StoryComposer(
            isSignedIn = isSignedIn,
            canPost = canPost,
            authorName = authorName,
            postState = postState,
            onPostStory = onPostStory,
            onSignIn = onSignIn,
            onPostStateConsumed = onPostStateConsumed,
            modifier = if (wide) Modifier.widthIn(max = 720.dp) else Modifier.fillMaxWidth()
        )
    }
}
