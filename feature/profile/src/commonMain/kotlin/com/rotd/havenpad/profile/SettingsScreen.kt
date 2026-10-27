package com.rotd.havenpad.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.theme.ThemeController
import com.rotd.havenpad.theme.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    if (state.showBiometricPasswordPrompt) {
        BiometricPasswordDialog(
            onConfirm = viewModel::confirmEnableBiometric,
            onDismiss = viewModel::dismissBiometricPasswordPrompt,
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        when {
            state.isLoading && state.user == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.user != null -> {
                SettingsContent(
                    user = state.user!!,
                    padding = padding,
                    biometricAvailable = state.biometricAvailable,
                    biometricEnabled = state.biometricEnabled,
                    biometricMessage = state.biometricMessage,
                    accountMessage = state.accountMessage,
                    deviceNames = state.devices.map { device ->
                        if (device.thisDevice) "${device.name} · This device" else device.name
                    },
                    onBack = onBack,
                    onEditProfile = onEditProfile,
                    onOpenNotifications = onOpenNotifications,
                    onOpenKyc = onOpenKyc,
                    onOpenAdmin = onOpenAdmin,
                    onLogout = onLogout,
                    onBiometricToggle = viewModel::onBiometricToggle,
                    onResetPassword = viewModel::sendPasswordReset,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    user: User,
    padding: PaddingValues,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    biometricMessage: String?,
    accountMessage: String?,
    deviceNames: List<String>,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onResetPassword: () -> Unit,
) {
    var showTheme by remember { mutableStateOf(false) }
    val themeMode by ThemeController.mode.collectAsStateWithLifecycle()
    if (showTheme) {
        ThemeModeDialog(
            selected = themeMode,
            onSelect = {
                ThemeController.setMode(it)
                showTheme = false
            },
            onDismiss = { showTheme = false },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
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
                    .padding(horizontal = if (wide) 28.dp else 20.dp, vertical = if (wide) 20.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsTopBar(onBack = onBack, wide = wide)
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AccountSettingsCard(
                                user = user,
                                themeLabel = themeMode.label(),
                                onEditProfile = onEditProfile,
                                onOpenNotifications = onOpenNotifications,
                                onAppearance = { showTheme = true },
                            )
                            VerificationSettingsCard(
                                user = user,
                                onOpenKyc = onOpenKyc,
                                onOpenAdmin = onOpenAdmin,
                            )
                            SettingsBanner(
                                title = bannerTitle(user),
                                subtitle = bannerSubtitle(user),
                                icon = Icons.Default.Verified,
                                onClick = bannerClick(user, onEditProfile, onOpenKyc, onOpenAdmin),
                            )
                        }
                        SecuritySettingsCard(
                            biometricAvailable = biometricAvailable,
                            biometricEnabled = biometricEnabled,
                            biometricMessage = biometricMessage,
                            accountMessage = accountMessage,
                            deviceNames = deviceNames,
                            onBiometricToggle = onBiometricToggle,
                            onResetPassword = onResetPassword,
                            onLogout = onLogout,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    AccountSettingsCard(
                        user = user,
                        themeLabel = themeMode.label(),
                        onEditProfile = onEditProfile,
                        onOpenNotifications = onOpenNotifications,
                        onAppearance = { showTheme = true },
                    )
                    VerificationSettingsCard(
                        user = user,
                        onOpenKyc = onOpenKyc,
                        onOpenAdmin = onOpenAdmin,
                    )
                    SettingsBanner(
                        title = bannerTitle(user),
                        subtitle = bannerSubtitle(user),
                        icon = Icons.Default.Verified,
                        onClick = bannerClick(user, onEditProfile, onOpenKyc, onOpenAdmin),
                    )
                    SecuritySettingsCard(
                        biometricAvailable = biometricAvailable,
                        biometricEnabled = biometricEnabled,
                        biometricMessage = biometricMessage,
                        accountMessage = accountMessage,
                        deviceNames = deviceNames,
                        onBiometricToggle = onBiometricToggle,
                        onResetPassword = onResetPassword,
                        onLogout = onLogout,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AccountSettingsCard(
    user: User,
    themeLabel: String,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onAppearance: () -> Unit,
) {
    SettingsCard {
        SettingsRow(
            icon = Icons.Default.Email,
            title = "Email",
            trailing = user.email,
            trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onEditProfile,
        )
        SettingsRow(
            icon = Icons.Default.Person,
            title = "Username",
            trailing = "@${user.username}",
            trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onEditProfile,
        )
        SettingsRow(
            icon = Icons.Default.Phone,
            title = "Phone",
            trailing = user.phone?.takeIf { it.isNotBlank() } ?: "Add",
            trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onEditProfile,
        )
        SettingsRow(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            onClick = onOpenNotifications,
        )
        SettingsRow(
            icon = Icons.Default.WbSunny,
            title = "Appearance",
            trailing = themeLabel,
            trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onAppearance,
        )
    }
}

@Composable
private fun VerificationSettingsCard(
    user: User,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    SettingsCard {
        SettingsRow(
            icon = Icons.Default.Verified,
            title = "Verification",
            trailing = kycStatusLabel(user.kycStatus),
            trailingColor = kycColor(user.kycStatus),
            showChevron = user.role.opensKyc(),
            onClick = if (user.role.opensKyc()) onOpenKyc else null,
        )
        if (user.role == UserRole.ADMIN) {
            SettingsRow(
                icon = Icons.Default.Shield,
                title = "Admin desk",
                onClick = onOpenAdmin,
            )
        }
    }
}

@Composable
private fun SecuritySettingsCard(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    biometricMessage: String?,
    accountMessage: String?,
    deviceNames: List<String>,
    onBiometricToggle: (Boolean) -> Unit,
    onResetPassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        if (biometricAvailable) {
            SettingsRow(
                icon = Icons.Default.Fingerprint,
                title = "Biometric login",
                showChevron = false,
                end = {
                    Switch(checked = biometricEnabled, onCheckedChange = onBiometricToggle)
                },
            )
        }
        if (!biometricMessage.isNullOrBlank()) {
            Text(
                text = biometricMessage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsRow(
            icon = Icons.Default.VpnKey,
            title = "Password reset",
            trailing = "Email",
            trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onResetPassword,
        )
        if (!accountMessage.isNullOrBlank()) {
            Text(
                text = accountMessage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Signed-in devices",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (deviceNames.isEmpty()) {
            Text(
                text = "This device will appear here after you sign in.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            deviceNames.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        SettingsRow(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Log out",
            showChevron = false,
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onLogout,
        )
    }
}

private fun bannerClick(
    user: User,
    onEditProfile: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenAdmin: () -> Unit,
): () -> Unit = when {
    user.role.opensKyc() && user.kycStatus != KycStatus.VERIFIED -> onOpenKyc
    user.role == UserRole.STUDENT -> onEditProfile
    user.role == UserRole.ADMIN -> onOpenAdmin
    else -> onEditProfile
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit, wide: Boolean = false) {
    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                container = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        BoxRow(onBack = onBack)
    }
}

@Composable
private fun BoxRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            container = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        Text(
            text = "Settings",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun SettingsBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            BoxIcon(icon)
        }
    }
}

@Composable
private fun BoxIcon(icon: ImageVector) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ThemeModeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    TextButton(onClick = { onSelect(mode) }) {
                        Text(
                            text = mode.label(),
                            fontWeight = if (mode == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun BiometricPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank(),
            ) { Text("Enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun kycColor(status: KycStatus) = when (status) {
    KycStatus.VERIFIED -> MaterialTheme.colorScheme.primary
    KycStatus.PENDING -> MaterialTheme.colorScheme.secondary
    KycStatus.REJECTED -> MaterialTheme.colorScheme.error
    KycStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun bannerTitle(user: User): String = when {
    user.kycStatus != KycStatus.VERIFIED && user.role.opensKyc() -> "Finish verification"
    user.role == UserRole.STUDENT -> "Roommate preferences"
    user.role == UserRole.LANDLORD -> "Your business profile"
    user.role == UserRole.AGENT -> "Your agent profile"
    else -> "Admin desk"
}

private fun bannerSubtitle(user: User): String = when {
    user.kycStatus == KycStatus.PENDING && user.role.opensKyc() -> "Your documents are in review."
    user.kycStatus != KycStatus.VERIFIED && user.role.opensKyc() -> "Add your documents so people can trust you."
    user.role == UserRole.STUDENT -> {
        val count = user.roommatePreferences.size
        if (count == 0) "Add who you want to live with." else "$count preferences saved."
    }
    user.role == UserRole.LANDLORD -> user.businessName?.takeIf { it.isNotBlank() } ?: "Add the name shown on your listings."
    user.role == UserRole.AGENT -> user.serviceArea?.takeIf { it.isNotBlank() } ?: "Add the area you cover."
    else -> "Review landlords, students, and reports."
}
