package com.rotd.havenpad.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import com.rotd.havenpad.web.components.LandingFooter
import com.rotd.havenpad.web.components.LandingNavbar
import com.rotd.havenpad.web.components.LandingTab
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingTextSecondary


@Composable
fun FeaturesScreen(
    onNavigateToLanding: () -> Unit,
    onNavigateToPolicy: () -> Unit,
    onSignIn: () -> Unit = {},
    wideOverride: Boolean? = null,
) {
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = wideOverride ?: (maxWidth >= 1024.dp)
        val horizontalPadding = if (wide) 80.dp else 20.dp
        
        Scaffold(
            topBar = {
                LandingNavbar(
                    onLogin = onSignIn,
                    onNavigateToFeatures = { /* Already here */ },
                    onNavigateToPolicy = onNavigateToPolicy,
                    onLogoClick = onNavigateToLanding,
                    selectedTab = LandingTab.FEATURES,
                    wide = wide
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Engineered for Student Protection",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = LandingBrandGreen,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    FeatureDetail(
                        "Physical Vetting",
                        "Unlike standard classifieds, every property on HavenPad is physically visited by our field agents. We verify that the house actually exists, matches its photos, and is suitable for student living."
                    )
                    FeatureDetail(
                        "Escrow Payments",
                        "Your rent and deposit are held in a secure, state-regulated escrow account. Funds are only released to the landlord after you've moved in and verified the keys. Zero risk of deposit theft."
                    )
                    FeatureDetail(
                        "Live Video Walkthroughs",
                        "Tour properties from anywhere in the world. Schedule 15-minute high-definition interactive video tours with current tenants or verified landlords."
                    )
                    FeatureDetail(
                        "Compatibility Matching",
                        "Our algorithm pairs you with roommates based on study habits, sleep schedules, cleanliness levels, and institutional background."
                    )
                    FeatureDetail(
                        "Guarantor Support",
                        "Built-in tools for parents and guardians to securely sign off on leases and manage rent payments directly through the app."
                    )
                }
                
                LandingFooter(wide, horizontalPadding)
            }
        }
    }
}

@Composable
private fun FeatureDetail(title: String, body: String) {
    Column(
        modifier = Modifier
            .widthIn(max = 800.dp)
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = LandingBrandGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = LandingTextSecondary, lineHeight = 28.sp)
    }
}

@ScreenPreviews
@Composable
fun FeaturesScreenPreview() {
    HavenPadTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 1024.dp
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FeaturesScreen(
                    onNavigateToLanding = {},
                    onNavigateToPolicy = {},
                    wideOverride = wide
                )
            }
        }
    }
}
