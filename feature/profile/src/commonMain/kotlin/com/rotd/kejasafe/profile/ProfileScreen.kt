package com.rotd.kejasafe.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeCard
import com.rotd.kejasafe.components.KejaSafeFloatingNavBar
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.ThemeController
import com.rotd.kejasafe.theme.ThemeMode

@Composable
fun ProfileScreen(
    selectedTab: KejaSafeNavTab = KejaSafeNavTab.PROFILE,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onCreateListingClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-enter Profile (e.g. after Edit) must show the latest session/server user.
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val isLandlord =
        state.user?.role == UserRole.LANDLORD || state.user?.role == UserRole.ADMIN

    Scaffold(
        topBar = {
            KejaSafeHeader(
                title = "Profile",
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            KejaSafeFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = if (isLandlord) onCreateListingClick else null
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.user == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.user == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        KejaSafePrimaryButton(text = "Retry", onClick = { viewModel.loadProfile() })
                    }
                }
                state.user != null -> {
                    ProfileContent(
                        user = state.user!!,
                        onEditProfile = onEditProfile,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    user: User,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val isStudent = user.role == UserRole.STUDENT
    val isLandlord = user.role == UserRole.LANDLORD || user.role == UserRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user.fullName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = roleSubtitle(user),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoleBadge(user = user)
            Spacer(modifier = Modifier.height(24.dp))
            KejaSafePrimaryButton(
                text = "Edit Profile",
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = "Verification Status", icon = Icons.Default.Shield)
            Spacer(modifier = Modifier.height(12.dp))

            if (isStudent) {
                VerificationItem(
                    title = "University ID",
                    subtitle = kycSubtitle(user.kycStatus, "Submit student ID for verification"),
                    status = kycStatusLabel(user.kycStatus),
                    statusPositive = user.kycStatus == KycStatus.VERIFIED,
                    icon = Icons.Default.Badge
                )
                Spacer(modifier = Modifier.height(12.dp))
                VerificationItem(
                    title = "Student Email",
                    subtitle = user.email,
                    status = if (user.kycStatus == KycStatus.VERIFIED) "Verified" else "Linked",
                    statusPositive = true,
                    icon = Icons.Default.Email
                )
            } else {
                VerificationItem(
                    title = "Landlord Account",
                    subtitle = user.businessName?.takeIf { it.isNotBlank() }
                        ?: "Property owner on KejaSafe",
                    status = kycStatusLabel(user.kycStatus),
                    statusPositive = user.kycStatus == KycStatus.VERIFIED ||
                        user.kycStatus == KycStatus.NOT_SUBMITTED,
                    icon = Icons.Default.HomeWork
                )
                Spacer(modifier = Modifier.height(12.dp))
                VerificationItem(
                    title = "Business Email",
                    subtitle = user.email,
                    status = "Linked",
                    statusPositive = true,
                    icon = Icons.Default.Email
                )
                if (!user.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    VerificationItem(
                        title = "Phone",
                        subtitle = user.phone.orEmpty(),
                        status = "On file",
                        statusPositive = true,
                        icon = Icons.Default.Phone
                    )
                }
            }
        }

        if (isStudent) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Roommate Preferences", icon = Icons.Default.Group)
                    IconButton(onClick = onEditProfile) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Preferences",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (user.roommatePreferences.isEmpty()) {
                    KejaSafeBody(
                        text = "No preferences set yet. Edit profile to add them.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.roommatePreferences.forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag) },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }
        }

        if (isLandlord) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Landlord Details", icon = Icons.Default.Business)
                Spacer(modifier = Modifier.height(12.dp))
                AccountOptionItem(
                    title = user.businessName?.takeIf { it.isNotBlank() } ?: "Add business name",
                    icon = Icons.Default.Apartment
                )
                AccountOptionItem(
                    title = user.phone?.takeIf { it.isNotBlank() } ?: "Add phone number",
                    icon = Icons.Default.Phone
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = "Appearance", icon = Icons.Default.Settings)
            Spacer(modifier = Modifier.height(12.dp))
            ThemeModeRow()
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = "Security & Account", icon = Icons.Default.Lock)
            Spacer(modifier = Modifier.height(12.dp))
            AccountOptionItem(title = "Password & Security", icon = Icons.Default.VpnKey)
            AccountOptionItem(title = "Payment Methods", icon = Icons.Default.Payment)
            AccountOptionItem(title = "Notification Preferences", icon = Icons.Default.Notifications)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ThemeModeRow() {
    val mode by ThemeController.mode.collectAsStateWithLifecycle()
    KejaSafeCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Switching theme keeps you signed in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { option ->
                    val selected = mode == option
                    val label = when (option) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                    SuggestionChip(
                        onClick = { ThemeController.setMode(option) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(user: User) {
    val (label, container, content) = when (user.role) {
        UserRole.STUDENT -> Triple(
            if (user.kycStatus == KycStatus.VERIFIED) "Verified Student" else "Student",
            Color(0xFFFDE68A).copy(alpha = 0.25f),
            Gold600
        )
        UserRole.LANDLORD -> Triple(
            if (user.kycStatus == KycStatus.VERIFIED) "Verified Landlord" else "Landlord",
            Emerald700.copy(alpha = 0.14f),
            Emerald700
        )
        UserRole.ADMIN -> Triple(
            "Admin",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = container,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, content)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = content
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun roleSubtitle(user: User): String = when (user.role) {
    UserRole.STUDENT -> {
        val major = user.major?.takeIf { it.isNotBlank() } ?: "Major not set"
        val year = user.yearOfStudy?.let { "Year $it" } ?: "Year not set"
        "$major · $year · @${user.username}"
    }
    UserRole.LANDLORD -> {
        val business = user.businessName?.takeIf { it.isNotBlank() } ?: "Property owner"
        "$business · @${user.username}"
    }
    UserRole.ADMIN -> "Administrator · @${user.username}"
}

private fun kycStatusLabel(status: KycStatus): String = when (status) {
    KycStatus.VERIFIED -> "Verified"
    KycStatus.PENDING -> "Pending"
    KycStatus.REJECTED -> "Rejected"
    KycStatus.NOT_SUBMITTED -> "Not submitted"
}

private fun kycSubtitle(status: KycStatus, notSubmitted: String): String = when (status) {
    KycStatus.VERIFIED -> "Identity verified"
    KycStatus.PENDING -> "Under review"
    KycStatus.REJECTED -> "Needs resubmission"
    KycStatus.NOT_SUBMITTED -> notSubmitted
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VerificationItem(
    title: String,
    subtitle: String,
    status: String,
    statusPositive: Boolean,
    icon: ImageVector
) {
    val statusColor = if (statusPositive) Color(0xFF10B981) else Gold600
    KejaSafeCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = statusColor
                    )
                    Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
        }
    }
}

@Composable
private fun AccountOptionItem(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
