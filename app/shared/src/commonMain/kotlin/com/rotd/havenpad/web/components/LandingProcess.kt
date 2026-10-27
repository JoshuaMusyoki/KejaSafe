package com.rotd.havenpad.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.web.models.StepData
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingLightGreenBg
import com.rotd.havenpad.web.theme.LandingTextSecondary

@Composable
fun LandingProcess(wide: Boolean, horizontalPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LandingLightGreenBg)
            .padding(horizontal = horizontalPadding, vertical = 80.dp)
    ) {
        Text("STEP-BY-STEP PROCESS", style = MaterialTheme.typography.labelLarge, color = LandingBrandGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "From Campus Acceptance to Moving In",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Simple, foolproof, and completely mobile-friendly. You will never pay money to an unverified stranger again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LandingTextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "From Campus Acceptance to Moving In",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Simple, foolproof, and completely mobile-friendly. You will never pay money to an unverified stranger again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LandingTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        val steps = listOf(
            StepData(
                "01",
                "Discover & Filter",
                "Set your budget, desired walking distance to your specific lecture hall, and amenities like high-speed Wi-Fi or included utilities.",
                "Max 10 mins to class",
                Icons.Default.FilterList
            ),
            StepData("02", "Live Walkthrough", "Schedule an interactive video tour hosted by the current student tenant or verified landlord. Ask questions and inspect closets in real time.", "15-min interactive slot", Icons.Default.VideoCameraFront),
            StepData("03", "Lock-In With Escrow", "Reserve the room with an encrypted deposit. The landlord cannot run with your cash; funds are held in state-regulated trust.", "100% Protected", Icons.Default.VpnKey),
            StepData("04", "Move in Confidently", "Collect physical keys, inspect your room, and sign off in the app. Escrow funds unlock only once you confirm you are satisfied.", "24/7 Dispute Help", Icons.Default.DoneAll)
        )

        if (wide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                steps.forEach { step ->
                    StepCard(step, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                steps.forEach { step ->
                    StepCard(step, Modifier.fillMaxWidth())
                }
            }
        }
    }
}