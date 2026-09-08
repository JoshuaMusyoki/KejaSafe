package com.rotd.kejasafe.web.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.models.PillarData
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@Composable
fun LandingPillars(wide: Boolean, horizontalPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("OUR STANDARD OF TRUST", style = MaterialTheme.typography.labelLarge, color = LandingBrandGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("How KejaSafe Solves Student Housing", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Moving for college shouldn't feel like navigating a minefield of bogus classifieds, phantom landlords, and lost deposits. We re-engineered the process from search to keys.",
            style = MaterialTheme.typography.bodyLarge,
            color = LandingTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 800.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        val pillars = listOf(
            PillarData(
                "Verified Property Inspections",
                "In-person physical audits and live interactive video inspections eliminate deceptive photography, catfished properties, and non-existent buildings.",
                "Physical Stamp Included",
                Icons.Default.Verified
            ),
            PillarData("Scam-Free Escrow Protection", "Your security deposit and rent are vaulted in a regulated escrow account. Funds are released to the landlord only 24 hours after you inspect and accept your keys.", "Zero Cash Hand-offs", Icons.Default.GppGood),
            PillarData("Student & Guarantor ID Check", "Every user is authenticated via official student email and institutional ID. Both students and property managers are verified to ensure safe, accountable living spaces.", ".edu & State ID Vetted", Icons.Default.AssignmentInd),
            PillarData("Smart Roommate Matcher", "Find compatible housemates by matching sleep routines, study discipline, noise comfort, cleanliness levels, and course schedules without awkward friction.", "Algorithmic Compatibility", Icons.Default.Groups)
        )

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                pillars.forEach { pillar ->
                    PillarCard(pillar, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                pillars.forEach { pillar ->
                    PillarCard(pillar, Modifier.fillMaxWidth())
                }
            }
        }
    }
}