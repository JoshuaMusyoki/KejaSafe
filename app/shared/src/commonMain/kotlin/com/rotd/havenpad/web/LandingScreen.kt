package com.rotd.havenpad.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.web.components.LandingAvailableListings
import com.rotd.havenpad.web.components.LandingBottomCTA
import com.rotd.havenpad.web.components.LandingComparison
import com.rotd.havenpad.web.components.LandingFooter
import com.rotd.havenpad.web.components.LandingHero
import com.rotd.havenpad.web.components.LandingNavbar
import com.rotd.havenpad.web.components.LandingPillars
import com.rotd.havenpad.web.components.LandingProcess
import com.rotd.havenpad.web.components.LandingStats
import com.rotd.havenpad.web.components.LandingTab
import com.rotd.havenpad.web.components.LandingTailored
import com.rotd.havenpad.web.components.LandingTestimonials
import com.rotd.havenpad.User
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LandingScreen(
    onNavigateToFeatures: () -> Unit,
    onNavigateToPolicy: () -> Unit,
    onSignIn: () -> Unit = {},
    /** Signing in from the story composer should come back here, not to the dashboard. */
    onSignInToShareStory: () -> Unit = onSignIn,
    wideOverride: Boolean? = null,
    viewModel: LandingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val postState by viewModel.postState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LandingScreenContent(
        state = state,
        postState = postState,
        currentUser = currentUser,
        onNavigateToFeatures = onNavigateToFeatures,
        onNavigateToPolicy = onNavigateToPolicy,
        onSignIn = onSignIn,
        onSignInToShareStory = onSignInToShareStory,
        wideOverride = wideOverride,
        onPostStory = viewModel::postStory,
        onDeleteStory = viewModel::deleteStory,
        onPostStateConsumed = viewModel::consumePostState
    )
}

@Composable
private fun LandingScreenContent(
    state: LandingUiState,
    postState: StoryPostState,
    currentUser: User?,
    onNavigateToFeatures: () -> Unit,
    onNavigateToPolicy: () -> Unit,
    onSignIn: () -> Unit,
    onSignInToShareStory: () -> Unit,
    wideOverride: Boolean?,
    onPostStory: (String, Int) -> Unit,
    onDeleteStory: (String) -> Unit,
    onPostStateConsumed: () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = wideOverride ?: (maxWidth >= 800.dp)
        val horizontalPadding = if (wide) 80.dp else 20.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                LandingNavbar(
                    onLogin = onSignIn,
                    onNavigateToFeatures = onNavigateToFeatures,
                    onNavigateToPolicy = onNavigateToPolicy,
                    onLogoClick = { /* Already here */ },
                    onExploreListings = onSignIn,
                    signedIn = currentUser != null,
                    selectedTab = LandingTab.HOME,
                    wide = wide
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
            ) {
                LandingHero(wide, horizontalPadding)
                LandingStats(wide, horizontalPadding, state.stats)
                LandingPillars(wide, horizontalPadding)
                LandingProcess(wide, horizontalPadding)
                LandingComparison(wide, horizontalPadding)
                LandingTailored(wide, horizontalPadding)
                LandingAvailableListings(
                    wide = wide,
                    horizontalPadding = horizontalPadding,
                    listings = state.listings,
                    loading = state.loadingListings,
                    error = state.listingsError,
                    onExploreAll = onSignIn,
                    onInspect = { _ -> onSignIn() }
                )
                LandingTestimonials(
                    wide = wide,
                    horizontalPadding = horizontalPadding,
                    stories = state.stories,
                    stats = state.stats,
                    loading = state.loadingStories,
                    error = state.storiesError,
                    currentUserId = currentUser?.id,
                    isSignedIn = currentUser != null,
                    canPost = currentUser.canPostStory(),
                    authorName = currentUser?.fullName,
                    postState = postState,
                    onPostStory = onPostStory,
                    onDeleteStory = onDeleteStory,
                    onSignIn = onSignInToShareStory,
                    onPostStateConsumed = onPostStateConsumed
                )
                LandingBottomCTA(wide, horizontalPadding)
                LandingFooter(wide, horizontalPadding)
            }
        }
    }
}

@ScreenPreviews
@Composable
fun LandingScreenPreview() {
    HavenPadTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LandingScreenContent(
                state = LandingUiState(),
                postState = StoryPostState.Idle,
                currentUser = null,
                onNavigateToFeatures = {},
                onNavigateToPolicy = {},
                onSignIn = {},
                onSignInToShareStory = {},
                wideOverride = null,
                onPostStory = { _, _ -> },
                onDeleteStory = { _ -> },
                onPostStateConsumed = {}
            )
        }
    }
}
