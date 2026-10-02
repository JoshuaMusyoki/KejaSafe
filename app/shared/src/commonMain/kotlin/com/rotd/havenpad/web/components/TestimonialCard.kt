package com.rotd.havenpad.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.havenpad.CommunityStory
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingPillarMint
import com.rotd.havenpad.web.theme.LandingTextDark
import com.rotd.havenpad.web.theme.LandingTextSecondary

/** A story posted through `POST /stories` by a signed-in student or landlord. */
@Composable
fun TestimonialCard(
    story: CommunityStory,
    modifier: Modifier,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RatingStars(story.rating)
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove my story",
                            tint = LandingTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "\"${story.content}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = LandingTextDark,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(LandingPillarMint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        story.authorName.initials(),
                        fontWeight = FontWeight.Bold,
                        color = LandingBrandGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        story.authorName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        story.authorRole.storyLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = LandingTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
internal fun RatingStars(rating: Int, size: Int = 16) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (index < rating) Color(0xFFF59E0B) else LandingTextSecondary,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

internal fun UserRole.storyLabel(): String = when (this) {
    UserRole.STUDENT -> "Verified Student"
    UserRole.LANDLORD -> "Verified Landlord"
    UserRole.ADMIN -> "HavenPad Team"
}

private fun String.initials(): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "KS"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}
