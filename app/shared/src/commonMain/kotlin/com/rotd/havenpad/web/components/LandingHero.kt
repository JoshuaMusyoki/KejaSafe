package com.rotd.havenpad.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import com.rotd.havenpad.web.StoreLinks
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingBrandSecondary
import com.rotd.havenpad.web.theme.LandingLightGreenBg
import com.rotd.havenpad.web.theme.LandingTextDark
import com.rotd.havenpad.web.theme.LandingTextSecondary
import org.jetbrains.compose.resources.painterResource
import havenpad.app.shared.generated.resources.Res
import havenpad.app.shared.generated.resources.house_tour_sample
import havenpad.app.shared.generated.resources.landing_page

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingHero(wide: Boolean, horizontalPadding: Dp) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LandingLightGreenBg)
            .padding(horizontal = horizontalPadding, vertical = if (wide) 80.dp else 40.dp)
    ) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    HeroContent(wide = true, uriHandler = uriHandler)
                }

                // Hero Card Placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .shadow(20.dp, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.house_tour_sample),
                                contentDescription = "The Moringa Road Residence",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Top right overlay
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Live Walkthrough Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("The Moringa Road Residence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("400m from Gate A", style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ksh.5000", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LandingBrandGreen)
                                Text("/month (All In)", style = MaterialTheme.typography.labelSmall, color = LandingTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LabelWithIcon("Escrow Deposit Locked", Icons.Default.Lock)
                            LabelWithIcon("Landlord Vetted (100%)", Icons.Default.VerifiedUser)
                        }
                    }
                }
            }
        } else {
            HeroContent(wide = false, uriHandler = uriHandler)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroContent(wide: Boolean, uriHandler: UriHandler) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroBadge("Campus Verified", Icons.Default.CheckCircle)
            HeroBadge("100% Escrow Protected", Icons.Default.Shield)
            HeroBadge("Live Tour Certified", Icons.Default.Videocam)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NEXT-GEN STUDENT HOUSING PROTECTION",
            style = MaterialTheme.typography.labelLarge,
            color = LandingBrandGreen,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                append("Student housing without ")
                withStyle(SpanStyle(color = LandingBrandSecondary, textDecoration = TextDecoration.Underline)) {
                    append("scams")
                }
                append(", surprises, or hidden fees.")
            },
            style = if (wide) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = LandingTextDark,
            lineHeight = if (wide) 64.sp else 44.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "HavenPad protects university students and trusted landlords with 100% physically vetted campus listings, live video walkthroughs, compatibility matching, and zero-risk escrow deposit protection.",
            style = MaterialTheme.typography.bodyLarge,
            color = LandingTextSecondary,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { uriHandler.openUri(StoreLinks.PLAY_STORE) },
                colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Explore, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore Verified Houses")
            }

            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.PlayCircle, null, tint = LandingBrandGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Watch 60-Sec Tour", color = LandingTextDark)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar stack placeholder
            Row {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (i * -8).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                "Endorsed by official student unions & housing offices across 45+ campuses.",
                style = MaterialTheme.typography.labelMedium,
                color = LandingTextSecondary
            )
        }
    }
}

@ScreenPreviews
@Composable
fun LandingHeroPreview() {
    HavenPadTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LandingHero(wide = false, horizontalPadding = 20.dp)
        }
    }
}
