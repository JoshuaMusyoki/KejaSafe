package com.rotd.havenpad.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.User
import com.rotd.havenpad.components.UserPhoto
import com.rotd.havenpad.media.ProfilePhotoLoader

@Composable
internal fun ProfileMasthead(
    user: User,
    score: String,
    reviewCount: String,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangePhoto: () -> Unit,
    photoUploading: Boolean,
    wide: Boolean = false,
) {
    if (wide) {
        WideProfileMasthead(
            user = user,
            score = score,
            reviewCount = reviewCount,
            onEditProfile = onEditProfile,
            onOpenSettings = onOpenSettings,
            onChangePhoto = onChangePhoto,
            photoUploading = photoUploading,
        )
        return
    }
    val avatarSize = 96.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(156.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            HeaderActions(
                user = user,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
            BannerIdentity(
                user = user,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 112.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        Column(
            modifier = Modifier
                .offset(y = -(avatarSize / 2))
                .padding(horizontal = 4.dp),
        ) {
            ProfileAvatar(
                user = user,
                size = avatarSize,
                uploading = photoUploading,
                onClick = onChangePhoto,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileCount(value = score, label = "Score")
                Spacer(modifier = Modifier.width(16.dp))
                ProfileCount(value = reviewCount, label = "Reviews")
                Spacer(modifier = Modifier.width(16.dp))
                EditProfilePill(onClick = onEditProfile)
            }
        }
    }
}

@Composable
private fun WideProfileMasthead(
    user: User,
    score: String,
    reviewCount: String,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangePhoto: () -> Unit,
    photoUploading: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(148.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            HeaderActions(
                user = user,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
            BannerIdentity(
                user = user,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 140.dp, end = 24.dp, bottom = 18.dp),
            )
        }
        Row(
            modifier = Modifier
                .offset(y = (-36).dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                user = user,
                size = 104.dp,
                uploading = photoUploading,
                onClick = onChangePhoto,
            )
            Spacer(modifier = Modifier.weight(1f))
            ProfileCount(value = score, label = "Score")
            Spacer(modifier = Modifier.width(28.dp))
            ProfileCount(value = reviewCount, label = "Reviews")
            Spacer(modifier = Modifier.width(28.dp))
            EditProfilePill(onClick = onEditProfile)
        }
    }
}

@Composable
private fun HeaderActions(
    user: User,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserPhoto(
            imageUrl = user.profilePictureUrl,
            fallback = nameInitials(user.fullName),
            modifier = Modifier
                .size(40.dp)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            background = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        )
        RoundIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
            container = MaterialTheme.colorScheme.surface,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BannerIdentity(user: User, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = user.fullName,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = user.email,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileAvatar(
    user: User,
    size: Dp,
    uploading: Boolean,
    onClick: () -> Unit,
) {
    var bitmap by remember(user.profilePictureUrl) {
        mutableStateOf(ProfilePhotoLoader.peek(user.profilePictureUrl))
    }
    LaunchedEffect(user.profilePictureUrl) {
        bitmap = ProfilePhotoLoader.load(user.profilePictureUrl)
    }
    Box(
        modifier = Modifier.size(size).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val photo = bitmap
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = nameInitials(user.fullName),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (uploading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Add photo",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EditProfilePill(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Edit",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
