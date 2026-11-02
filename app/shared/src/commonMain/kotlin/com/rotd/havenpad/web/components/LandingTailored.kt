package com.rotd.havenpad.web.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import com.rotd.havenpad.web.StoreLinks
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingLightGreenBg
import com.rotd.havenpad.web.theme.LandingPillarMint
import havenpad.app.shared.generated.resources.Res
import havenpad.app.shared.generated.resources.landing_page
import havenpad.app.shared.generated.resources.students_img
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalLayoutApi::class)
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
        Text(
            text = "BUILT FOR BOTH SIDES",
            style = MaterialTheme.typography.labelLarge,
            color = LandingBrandGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tailored for Safe Living &\nSmart Hosting",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (wide) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                TabButton("For University Students", selectedTab == 0) { selectedTab = 0 }
                TabButton("For Property Landlords", selectedTab == 1) { selectedTab = 1 }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                TabButton("For University Students", selectedTab == 0) { selectedTab = 0 }
                TabButton("For Property Landlords", selectedTab == 1) { selectedTab = 1 }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        if (wide) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Content Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        TailoredContent(uriHandler)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(450.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.students_img),
                        contentDescription = "Students using HavenPad",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Testimonial Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "\"Found my flat in 48 hours without being in town.\"",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Elena R., Computer Science, Class of '26",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        TailoredContent(uriHandler)
                    }
                }

                // Image for narrow screens
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.landing_page),
                        contentDescription = "Students using HavenPad",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "\"Found my flat in 48 hours without being in town.\"",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Elena R., Computer Science, Class of '26",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TailoredContent(uriHandler: UriHandler) {
    Surface(color = LandingPillarMint, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = "Student Guarantee",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LandingBrandGreen,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Never lose another deposit or\nsign into a horror story.",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(24.dp))

    FeatureBullet("Campus Transit Filter", "Sort by minutes walking or biking to your specific faculty buildings.")
    Spacer(modifier = Modifier.height(12.dp))
    FeatureBullet("Joint Lease Split", "Split rent and deposit smoothly with certified housemates without single-liability stress.")
    Spacer(modifier = Modifier.height(12.dp))
    FeatureBullet("Digital Move-in Checklist", "Capture photo logs on arrival to ensure 100% of your deposit returns upon lease conclusion.")

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { uriHandler.openUri(StoreLinks.PLAY_STORE) },
        colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(52.dp)
    ) {
        Text("Start Finding Housing Now")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@ScreenPreviews
@Composable
fun LandingTailoredPreview() {
    HavenPadTheme {
        LandingTailored(
            wide = true,
            horizontalPadding = 20.dp
        )
    }
}