package com.rotd.kejasafe.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.StoreLinks
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingLightGreenBg
import com.rotd.kejasafe.web.theme.LandingPillarMint

@Composable
fun LandingTailored(
    wide: Boolean,
    horizontalPadding: Dp
) {
    val uriHandler = LocalUriHandler.current
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LandingLightGreenBg)
            .padding(horizontal = horizontalPadding, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BUILT FOR BOTH SIDES", style = MaterialTheme.typography.labelLarge, color = LandingBrandGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tailored for Safe Living &\nSmart Hosting", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            TabButton("For University Students", selectedTab == 0) { selectedTab = 0 }
            TabButton("For Property Landlords", selectedTab == 1) { selectedTab = 1 }
        }

        Spacer(modifier = Modifier.height(80.dp))

        Row(
            modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 80.dp else 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = LandingPillarMint, shape = RoundedCornerShape(4.dp)) {
                    Text("Student Guarantee", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = LandingBrandGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Never lose another deposit or\nsign into a horror story.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))

                FeatureBullet("Campus Transit Filter", "Sort by minutes walking or biking to your specific faculty buildings.")
                FeatureBullet("Joint Lease Split", "Split rent and deposit smoothly with certified housemates without single-liability stress.")
                FeatureBullet("Digital Move-in Checklist", "Capture photo logs on arrival to ensure 100% of your deposit returns upon lease conclusion.")

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { uriHandler.openUri(StoreLinks.PLAY_STORE) },
                    colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Start Finding Housing Now")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }

            if (wide) {
                Box(modifier = Modifier.weight(1.2f).height(450.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    // Image placeholder
                    Text("Interactive Image", modifier = Modifier.align(Alignment.Center), color = Color.White)

                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).background(Color.Black.copy(alpha = 0.6f)).padding(16.dp)) {
                        Text(
                            "\"Found my flat in 48 hours without being in town.\"\nElena R., Computer Science, Class of '26",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
