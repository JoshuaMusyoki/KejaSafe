package com.rotd.havenpad.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadFloatingNavBar
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.media.rememberImagePickerLauncher
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    selectedTab: HavenPadNavTab = HavenPadNavTab.PROFILE,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onOpenKyc: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onCreateListingClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val pickPhoto = rememberImagePickerLauncher { images ->
        images.firstOrNull()?.let(viewModel::uploadPhoto)
    }

    ProfileScreenContent(
        state = state,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onEditProfile = onEditProfile,
        onOpenSettings = onOpenSettings,
        onOpenHome = onOpenHome,
        onOpenBookings = onOpenBookings,
        onOpenKyc = onOpenKyc,
        onOpenAdmin = onOpenAdmin,
        onCreateListingClick = onCreateListingClick,
        onRetry = { viewModel.loadProfile() },
        onChangePhoto = pickPhoto,
    )
}

@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    selectedTab: HavenPadNavTab = HavenPadNavTab.PROFILE,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onOpenKyc: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onCreateListingClick: (() -> Unit)? = null,
    onRetry: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
) {
    val isLandlord = state.user?.role == UserRole.LANDLORD || state.user?.role == UserRole.ADMIN

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            HavenPadFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = if (isLandlord) onCreateListingClick else null,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.user == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).padding(padding),
                    )
                }
                state.error != null && state.user == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(padding).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        HavenPadPrimaryButton(text = "Retry", onClick = onRetry)
                    }
                }
                state.user != null -> {
                    ProfileDashboard(
                        user = state.user!!,
                        contentPadding = padding,
                        onEditProfile = onEditProfile,
                        onOpenSettings = onOpenSettings,
                        onOpenKyc = onOpenKyc,
                        onOpenBookings = onOpenBookings,
                        onOpenHome = onOpenHome,
                        onOpenAdmin = onOpenAdmin,
                        onChangePhoto = onChangePhoto,
                        photoUploading = state.isUploadingPhoto,
                        photoMessage = state.photoMessage,
                    )
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
fun ProfileScreenPreview() {
    HavenPadTheme {
        ProfileScreenContent(
            state = ProfileUiState(
                isLoading = false,
                user = User(
                    id = "1",
                    username = "amina",
                    fullName = "Amina Otieno",
                    email = "amina@havenpad.app",
                    role = UserRole.STUDENT,
                ),
            ),
            onTabSelected = {},
            onEditProfile = {},
            onOpenSettings = {},
            onOpenHome = {},
            onOpenBookings = {},
            onOpenKyc = {},
            onOpenAdmin = {},
        )
    }
}