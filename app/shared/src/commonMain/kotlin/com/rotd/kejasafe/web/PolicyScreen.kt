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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Policies & Privacy") },
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
            Column(modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth()) {
                Text(
                    "Trust & Safety Policy",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = LandingBrandGreen
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                PolicySection(
                    "Data Protection",
                    "We collect minimal data required to verify your identity and facilitate housing. Your official student IDs and institutional emails are encrypted and never shared with third parties without your explicit consent."
                )
                
                PolicySection(
                    "Escrow Governance",
                    "All funds held in escrow are maintained in specialized trust accounts. KejaSafe acts as a neutral mediator. Funds are strictly released based on our 24-hour physical inspection rule."
                )
                
                PolicySection(
                    "Landlord Accountability",
                    "Landlords on our platform must pass physical site audits and background checks. Any landlord found engaging in bait-and-switch tactics or deposit withholding is permanently banned from the ecosystem."
                )
                
                PolicySection(
                    "Community Guidelines",
                    "Respect and safety are our priorities. We maintain a zero-tolerance policy for harassment, discrimination, or fraud within the roommate matching and messaging features."
                )
                
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    "Last Updated: October 2025",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LandingBrandGreen)
        Spacer(modifier = Modifier.height(8.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, color = LandingTextSecondary, lineHeight = 24.sp)
    }
}
