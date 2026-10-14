package com.rotd.havenpad.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadFormField
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.HavenPadTextField
import com.rotd.havenpad.theme.HavenPadTheme

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val draft = state.draft

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(viewModel) {
        viewModel.savedEvents.collect {
            onSaved()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HavenPadHeader(
                title = "Edit Profile",
                onBackClick = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading && user == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator()
                }
            }
            user == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "Profile unavailable",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                val isStudent = user.role == UserRole.STUDENT
                val isLandlord = user.role == UserRole.LANDLORD || user.role == UserRole.ADMIN

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(horizontal = HavenPadTheme.spacing.containerMarginMobile)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    HavenPadBody(
                        text = "Update your ${roleLabel(user.role).lowercase()} profile details.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    HavenPadFormField(label = "Full name") {
                        HavenPadTextField(
                            value = draft.fullName,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(fullName = value) }
                            },
                            placeholder = "Your full name"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HavenPadFormField(label = "Username") {
                        HavenPadTextField(
                            value = draft.username,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(username = value) }
                            },
                            placeholder = "username"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HavenPadFormField(label = "Phone") {
                        HavenPadTextField(
                            value = draft.phone,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(phone = value) }
                            },
                            placeholder = "+254 7XX XXX XXX"
                        )
                    }

                    if (isLandlord) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HavenPadFormField(label = "Business / company name") {
                            HavenPadTextField(
                                value = draft.businessName,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(businessName = value) }
                                },
                                placeholder = "e.g. Oakwood Housing Ltd"
                            )
                        }
                    }

                    if (isStudent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HavenPadFormField(label = "Major") {
                            HavenPadTextField(
                                value = draft.major,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(major = value) }
                                },
                                placeholder = "e.g. Computer Science"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HavenPadFormField(label = "Year of study") {
                            HavenPadTextField(
                                value = draft.yearOfStudy,
                                onValueChange = { value ->
                                    viewModel.updateDraft {
                                        it.copy(yearOfStudy = value.filter { ch -> ch.isDigit() })
                                    }
                                },
                                placeholder = "e.g. 3"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HavenPadFormField(label = "Roommate preferences") {
                            HavenPadTextField(
                                value = draft.roommatePreferencesText,
                                onValueChange = { value ->
                                    viewModel.updateDraft {
                                        it.copy(roommatePreferencesText = value)
                                    }
                                },
                                placeholder = "Quiet, Non-smoker, Early riser",
                                singleLine = false
                            )
                        }
                    }

                    if (!state.error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HavenPadPrimaryButton(
                        text = if (state.isSaving) "Saving..." else "Save changes",
                        onClick = viewModel::saveProfile,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                }
            }
        }
    }
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.STUDENT -> "Student"
    UserRole.LANDLORD -> "Landlord"
    UserRole.AGENT -> "Agent"
    UserRole.ADMIN -> "Admin"
}
