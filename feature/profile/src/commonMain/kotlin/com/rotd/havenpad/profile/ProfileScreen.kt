package com.rotd.havenpad.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.SignedInDevice
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadCard
import com.rotd.havenpad.components.HavenPadFloatingNavBar
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.theme.Emerald700
import com.rotd.havenpad.theme.Gold600
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ThemeController
import com.rotd.havenpad.theme.ThemeMode

@Composable
fun ProfileScreen(
    selectedTab: HavenPadNavTab = HavenPadNavTab.PROFILE,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenBills: () -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onOpenKyc: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
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

    if (state.showBiometricPasswordPrompt) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissBiometricPasswordPrompt() },
            title = { Text("Enable biometric login") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Confirm your password to save login on this device.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmEnableBiometric(password) },
                    enabled = password.isNotBlank(),
                ) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBiometricPasswordPrompt() }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            HavenPadHeader(title = "Profile")
        },
        bottomBar = {
            HavenPadFloatingNavBar(
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
                        HavenPadPrimaryButton(text = "Retry", onClick = { viewModel.loadProfile() })
                    }
                }
                state.user != null -> {
                    ProfileContent(
                        user = state.user!!,
                        onEditProfile = onEditProfile,
                        onOpenNotifications = onOpenNotifications,
                        onOpenHome = onOpenHome,
                        onOpenChat = onOpenChat,
                        onOpenBills = onOpenBills,
                        onOpenBookings = onOpenBookings,
                        onOpenKyc = onOpenKyc,
                        onOpenAdmin = onOpenAdmin,
                        onResetPassword = { viewModel.sendPasswordReset() },
                        onLogout = onLogout,
                        biometricAvailable = state.biometricAvailable,
                        biometricEnabled = state.biometricEnabled,
                        biometricMessage = state.biometricMessage,
                        accountMessage = state.accountMessage,
                        devices = state.devices,
                        onBiometricToggle = { viewModel.onBiometricToggle(it) },
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
    onOpenNotifications: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenBills: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
    onResetPassword: () -> Unit,
    onLogout: () -> Unit,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    biometricMessage: String?,
    accountMessage: String?,
    devices: List<SignedInDevice> = emptyList(),
    onBiometricToggle: (Boolean) -> Unit,
) {
    val isStudent = user.role == UserRole.STUDENT
    val isLandlord = user.role == UserRole.LANDLORD || user.role == UserRole.ADMIN
    val isAdmin = user.role == UserRole.ADMIN
    val actions = profileActions(
        isStudent = isStudent,
        isLandlord = isLandlord,
        isAdmin = isAdmin,
        onOpenHome = onOpenHome,
        onOpenChat = onOpenChat,
        onOpenBills = onOpenBills,
        onOpenBookings = onOpenBookings,
        onEditProfile = onEditProfile,
        onOpenNotifications = onOpenNotifications,
        onOpenKyc = onOpenKyc,
        onOpenAdmin = onOpenAdmin,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HavenPadTheme.spacing.containerMarginMobile),
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
                Spacer(modifier = Modifier.height(16.dp))
                ProfileReviewsSection(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                HavenPadPrimaryButton(
                    text = "Edit Profile",
                    onClick = onEditProfile,
                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
                )
            }

            if (isStudent || isLandlord) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Verification Status", icon = Icons.Default.Shield)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isStudent) {
                        TwoUp(
                            wide = wide,
                            first = {
                                VerificationItem(
                                    title = "University ID",
                                    subtitle = kycSubtitle(user.kycStatus, "Submit student ID for verification"),
                                    status = kycStatusLabel(user.kycStatus),
                                    statusPositive = user.kycStatus == KycStatus.VERIFIED,
                                    icon = Icons.Default.Badge,
                                    onClick = onOpenKyc,
                                )
                            },
                            second = {
                                VerificationItem(
                                    title = "Student Email",
                                    subtitle = user.email,
                                    status = if (user.kycStatus == KycStatus.VERIFIED) "Verified" else "Linked",
                                    statusPositive = true,
                                    icon = Icons.Default.Email
                                )
                            }
                        )
                    } else {
                        TwoUp(
                            wide = wide,
                            first = {
                                VerificationItem(
                                    title = "ID and address",
                                    subtitle = kycSubtitle(user.kycStatus, "Upload your ID and proof of address"),
                                    status = kycStatusLabel(user.kycStatus),
                                    statusPositive = user.kycStatus == KycStatus.VERIFIED,
                                    icon = Icons.Default.Badge,
                                    onClick = onOpenKyc,
                                )
                            },
                            second = {
                                VerificationItem(
                                    title = "Business Email",
                                    subtitle = user.email,
                                    status = "Linked",
                                    statusPositive = true,
                                    icon = Icons.Default.Email
                                )
                            },
                            third = if (user.phone.isNullOrBlank()) {
                                null
                            } else {
                                {
                                    VerificationItem(
                                        title = "Phone",
                                        subtitle = user.phone.orEmpty(),
                                        status = "On file",
                                        statusPositive = true,
                                        icon = Icons.Default.Phone
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (actions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = if (isLandlord) "Landlord tools" else "Student tools",
                        icon = if (isLandlord) Icons.Default.Business else Icons.Default.Badge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileActionSection(actions = actions, wide = wide)
                }
            }

            if (isStudent) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Student details", icon = Icons.Default.Badge)
                        IconButton(onClick = onEditProfile) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit student details",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    AccountOptionItem(
                        title = user.major?.takeIf { it.isNotBlank() } ?: "Add major",
                        subtitle = "Course of study",
                        icon = Icons.Default.Badge,
                        onClick = onEditProfile
                    )
                    AccountOptionItem(
                        title = user.yearOfStudy?.let { "Year $it" } ?: "Add year of study",
                        subtitle = "Current year",
                        icon = Icons.Default.Event,
                        onClick = onEditProfile
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Roommate preferences", icon = Icons.Default.Group)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (user.roommatePreferences.isEmpty()) {
                        HavenPadBody(
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
                                    onClick = onEditProfile,
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
                    SectionHeader(title = "Landlord details", icon = Icons.Default.Business)
                    Spacer(modifier = Modifier.height(4.dp))
                    AccountOptionItem(
                        title = user.businessName?.takeIf { it.isNotBlank() } ?: "Add business name",
                        subtitle = "Shown on your listings",
                        icon = Icons.Default.Apartment,
                        onClick = onEditProfile
                    )
                    AccountOptionItem(
                        title = user.phone?.takeIf { it.isNotBlank() } ?: "Add phone number",
                        subtitle = "How tenants can reach you",
                        icon = Icons.Default.Phone,
                        onClick = onEditProfile
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Appearance", icon = Icons.Default.Settings)
                Spacer(modifier = Modifier.height(12.dp))
                ThemeModeRow()
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Security & account", icon = Icons.Default.Lock)
                Spacer(modifier = Modifier.height(12.dp))
                if (biometricAvailable) {
                    BiometricLoginRow(
                        enabled = biometricEnabled,
                        onToggle = onBiometricToggle,
                    )
                }
                if (!biometricMessage.isNullOrBlank()) {
                    Text(
                        text = biometricMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
                AccountOptionItem(
                    title = "Password reset",
                    subtitle = "Email a link to ${user.email}",
                    icon = Icons.Default.VpnKey,
                    onClick = onResetPassword,
                )
                Text(
                    text = "Signed-in devices",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                if (devices.isEmpty()) {
                    Text(
                        text = "This device will appear here after you sign in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    devices.forEach { device ->
                        Text(
                            text = if (device.thisDevice) "${device.name} · This device" else device.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
                if (!accountMessage.isNullOrBlank()) {
                    Text(
                        text = accountMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
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
}

private data class ProfileAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private fun profileActions(
    isStudent: Boolean,
    isLandlord: Boolean,
    isAdmin: Boolean,
    onOpenHome: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenBills: () -> Unit,
    onOpenBookings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
): List<ProfileAction> = buildList {
    if (isAdmin) {
        add(ProfileAction("Admin", "Review landlords and students", Icons.Default.Shield, onOpenAdmin))
    }
    if (isLandlord && !isAdmin) {
        add(ProfileAction("Identity and address", "ID copy and where you live", Icons.Default.Badge, onOpenKyc))
    }
    if (isLandlord) {
        add(ProfileAction("My listings", "Houses you have published", Icons.Default.HomeWork, onOpenHome))
        add(ProfileAction("Booking requests", "Review stays and requests", Icons.Default.Event, onOpenBookings))
        add(ProfileAction("Messages", "Chat with tenants", Icons.AutoMirrored.Filled.Chat, onOpenChat))
        add(ProfileAction("Bills", "Rent and payments", Icons.Default.Payment, onOpenBills))
    }
    if (isStudent) {
        add(ProfileAction("Student ID", "University ID for verification", Icons.Default.Badge, onOpenKyc))
        add(ProfileAction("Browse houses", "Find a place near campus", Icons.Default.Home, onOpenHome))
        add(ProfileAction("Rent and bills", "Payments and stays", Icons.Default.Payment, onOpenBills))
        add(ProfileAction("Messages", "Chat with landlords", Icons.AutoMirrored.Filled.Chat, onOpenChat))
        add(ProfileAction("Roommate preferences", "Who you want to live with", Icons.Default.Group, onEditProfile))
    }
    add(ProfileAction("Notifications", "Message alerts", Icons.Default.Notifications, onOpenNotifications))
}

@Composable
private fun TwoUp(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: (@Composable () -> Unit)? = null,
) {
    if (!wide) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            first()
            second()
            third?.invoke()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) { first() }
                Box(modifier = Modifier.weight(1f)) { second() }
            }
            third?.invoke()
        }
    }
}

@Composable
private fun ProfileActionSection(
    actions: List<ProfileAction>,
    wide: Boolean,
) {
    if (!wide) {
        Column(modifier = Modifier.fillMaxWidth()) {
            actions.forEach { action ->
                AccountOptionItem(
                    title = action.title,
                    subtitle = action.subtitle,
                    icon = action.icon,
                    onClick = action.onClick,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { action ->
                        Box(modifier = Modifier.weight(1f)) {
                            ProfileActionCard(action)
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActionCard(action: ProfileAction) {
    HavenPadCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = action.onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ThemeModeRow() {
    val mode by ThemeController.mode.collectAsStateWithLifecycle()
    HavenPadCard {
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
        UserRole.AGENT -> Triple(
            if (user.kycStatus == KycStatus.VERIFIED) "Verified Agent" else "Agent",
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
    UserRole.AGENT -> {
        val agency = user.businessName?.takeIf { it.isNotBlank() } ?: "Agent"
        val area = user.serviceArea?.takeIf { it.isNotBlank() }
        if (area == null) "$agency · @${user.username}" else "$agency · $area · @${user.username}"
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
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
) {
    val statusColor = if (statusPositive) Color(0xFF10B981) else Gold600
    HavenPadCard(onClick = onClick) {
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
private fun BiometricLoginRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Biometric login", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Use fingerprint or Face ID on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AccountOptionItem(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
