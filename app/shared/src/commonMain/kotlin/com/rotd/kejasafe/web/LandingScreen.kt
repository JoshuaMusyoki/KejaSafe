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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.components.LandingAvailableListings
import com.rotd.kejasafe.web.components.LandingBottomCTA
import com.rotd.kejasafe.web.components.LandingComparison
import com.rotd.kejasafe.web.components.LandingFooter
import com.rotd.kejasafe.web.components.LandingHero
import com.rotd.kejasafe.web.components.LandingNavbar
import com.rotd.kejasafe.web.components.LandingPillars
import com.rotd.kejasafe.web.components.LandingProcess
import com.rotd.kejasafe.web.components.LandingStats
import com.rotd.kejasafe.web.components.LandingTailored
import com.rotd.kejasafe.web.components.LandingTestimonials
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LandingScreen(
    onNavigateToFeatures: () -> Unit,
    onNavigateToPolicy: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        visible = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1024.dp
        val horizontalPadding = if (wide) 80.dp else 20.dp

        Scaffold(
            topBar = { 
                LandingNavbar(
                    onLogin = { uriHandler.openUri(StoreLinks.PLAY_STORE) },
                    onNavigateToFeatures = onNavigateToFeatures,
                    onNavigateToPolicy = onNavigateToPolicy,
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
                LandingStats(wide, horizontalPadding)
                LandingPillars(wide, horizontalPadding)
                LandingProcess(wide, horizontalPadding)
                LandingComparison(wide, horizontalPadding)
                LandingTailored(wide, horizontalPadding)
                LandingAvailableListings(wide, horizontalPadding)
                LandingTestimonials(wide, horizontalPadding)
                LandingBottomCTA(wide, horizontalPadding)
                LandingFooter(wide, horizontalPadding)
            }
        }
    }
}
