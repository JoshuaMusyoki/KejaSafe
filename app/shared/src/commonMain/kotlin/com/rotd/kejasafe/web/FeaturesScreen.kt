package com.rotd.kejasafe.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KejaSafe Features") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = LandingBrandGreen
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
                "Unlike standard classifieds, every property on KejaSafe is physically visited by our field agents. We verify that the house actually exists, matches its photos, and is suitable for student living."
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
