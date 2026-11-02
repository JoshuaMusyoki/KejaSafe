package com.rotd.havenpad.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeadline
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews

@Composable
fun RoleChoiceScreen(
    onBack: () -> Unit,
    onContinue: (UserRole) -> Unit,
) {
    var selected by remember { mutableStateOf<UserRole?>(null) }
    Scaffold(
        topBar = {
            HavenPadHeader(title = "Create account", onBackClick = onBack)
        }
    ) { padding ->
        AuthFormScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "HavenPad",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            HavenPadHeadline(text = "What brings you to HavenPad?")
            HavenPadBody(text = "Choose how you'd like to start. Each role asks for its own details.")
            Spacer(modifier = Modifier.height(8.dp))
            RoleCard(
                title = "I'm a student",
                body = "Find a verified house near campus and share your university details.",
                icon = Icons.Default.Person,
                selected = selected == UserRole.STUDENT,
                onClick = { selected = UserRole.STUDENT },
            )
            RoleCard(
                title = "I'm a landlord",
                body = "List your property and share the ID and address we need to verify you.",
                icon = Icons.Default.Home,
                selected = selected == UserRole.LANDLORD,
                onClick = { selected = UserRole.LANDLORD },
            )
            RoleCard(
                title = "I'm an agent",
                body = "Connect students and landlords. Share your agency, licence, and area.",
                icon = Icons.Default.Place,
                selected = selected == UserRole.AGENT,
                onClick = { selected = UserRole.AGENT },
            )
            Spacer(modifier = Modifier.height(8.dp))
            HavenPadPrimaryButton(
                text = "Continue",
                onClick = { selected?.let(onContinue) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected != null,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    body: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ScreenPreviews
@Composable
fun RoleChoiceScreenPreview() {
    HavenPadTheme {
        RoleChoiceScreen(
            onBack = {},
            onContinue = {},
        )
    }
}