package com.rotd.kejasafe.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import com.rotd.kejasafe.web.components.LandingAvailableListings
import com.rotd.kejasafe.web.components.LandingBottomCTA
import com.rotd.kejasafe.web.components.LandingComparison
import com.rotd.kejasafe.web.components.LandingFooter
import com.rotd.kejasafe.web.components.LandingHero
import com.rotd.kejasafe.web.components.LandingNavbar
import com.rotd.kejasafe.web.components.LandingPillars
import com.rotd.kejasafe.web.components.LandingProcess
import com.rotd.kejasafe.web.components.LandingStats
import com.rotd.kejasafe.web.components.LandingTab
import com.rotd.kejasafe.web.components.LandingTailored
import com.rotd.kejasafe.web.components.LandingTestimonials
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.ScreenPreviews
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
    val scrollState = rememberScrollState()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val postState by viewModel.postState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = wideOverride ?: (maxWidth >= 1024.dp)
        val horizontalPadding = if (wide) 80.dp else 20.dp

        Scaffold(
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
                    onInspect = { onSignIn() }
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
                    onPostStory = viewModel::postStory,
                    onDeleteStory = viewModel::deleteStory,
                    onSignIn = onSignInToShareStory,
                    onPostStateConsumed = viewModel::consumePostState
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
    KejaSafeTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 1024.dp
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LandingScreen(
                    onNavigateToFeatures = {},
                    onNavigateToPolicy = {},
                    wideOverride = wide
                )
            }
        }
    }
}
