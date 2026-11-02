package com.rotd.havenpad.profile.models

import androidx.compose.ui.graphics.vector.ImageVector

data class ProfileStat(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: (() -> Unit)? = null,
)
