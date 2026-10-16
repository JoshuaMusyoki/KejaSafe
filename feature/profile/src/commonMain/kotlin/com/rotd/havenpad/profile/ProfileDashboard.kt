package com.rotd.havenpad.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.MyReviews
import com.rotd.havenpad.User
import com.rotd.havenpad.profile.components.ProfileStatGrid
import com.rotd.havenpad.profile.components.RecentReviewsCard
import com.rotd.havenpad.profile.components.profileStats

@Composable
internal fun ProfileDashboard(
    user: User,
    contentPadding: PaddingValues,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenAdmin: () -> Unit,
    onChangePhoto: () -> Unit,
    photoUploading: Boolean,
    photoMessage: String?,
) {
    var reviews by remember(user.id) { mutableStateOf<MyReviews?>(null) }
    val about = reviews?.aboutMe
    val score = if (about == null || about.reviewCount <= 0) "—" else "${about.percent}%"
    val reviewCount = about?.reviewCount?.toString() ?: "0"
    val stats = profileStats(
        user = user,
        onEditProfile = onEditProfile,
        onOpenKyc = onOpenKyc,
        onOpenBookings = onOpenBookings,
        onOpenHome = onOpenHome,
        onOpenAdmin = onOpenAdmin,
    )

    val ratings = about?.reviews.orEmpty().sortedBy { it.createdAt }.takeLast(6).map { it.rating }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val wide = maxWidth >= 840.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .then(if (wide) Modifier.fillMaxWidth() else Modifier.widthIn(max = 520.dp).fillMaxWidth())
                    .padding(
                        start = if (wide) 28.dp else 0.dp,
                        end = if (wide) 28.dp else 0.dp,
                        bottom = 24.dp,
                        top = if (wide) 20.dp else 0.dp,
                    ),
            ) {
                ProfileMasthead(
                    user = user,
                    score = score,
                    reviewCount = reviewCount,
                    onEditProfile = onEditProfile,
                    onOpenSettings = onOpenSettings,
                    onChangePhoto = onChangePhoto,
                    photoUploading = photoUploading,
                    wide = wide,
                )
                if (!photoMessage.isNullOrBlank()) {
                    Text(
                        text = photoMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = if (wide) 0.dp else 20.dp, vertical = 4.dp),
                    )
                }
                ProfileStatGrid(
                    stats = stats,
                    columns = if (wide) 4 else 2,
                    modifier = Modifier.padding(horizontal = if (wide) 0.dp else 20.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RecentReviewsCard(ratings = ratings, modifier = Modifier.weight(1f))
                        ProfileReviewsSection(
                            modifier = Modifier.weight(1f),
                            showHeading = false,
                            onLoaded = { reviews = it },
                        )
                    }
                } else {
                    RecentReviewsCard(
                        ratings = ratings,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileReviewsSection(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        showHeading = false,
                        onLoaded = { reviews = it },
                    )
                }
            }
        }
    }
}