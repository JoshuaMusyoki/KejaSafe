package com.rotd.havenpad.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingTextSecondary

enum class LandingTab {
    HOME, FEATURES, POLICY
}

@Composable
fun LandingNavbar(
    onLogin: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToPolicy: () -> Unit,
    onLogoClick: () -> Unit = {},
    onExploreListings: () -> Unit = onLogin,
    signedIn: Boolean = false,
    selectedTab: LandingTab? = null,
    wide: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (wide) 80.dp else 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLogoClick() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo Placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LandingBrandGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "HavenPad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == LandingTab.HOME) LandingBrandGreen else LandingBrandGreen // Logo is always green but maybe slightly different weight or treatment? User said highliht with green color.
                )

                if (wide) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            "Features",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTab == LandingTab.FEATURES) LandingBrandGreen else LandingTextSecondary,
                            fontWeight = if (selectedTab == LandingTab.FEATURES) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigateToFeatures() }
                        )
                        Text(
                            "Policies & Privacy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTab == LandingTab.POLICY) LandingBrandGreen else LandingTextSecondary,
                            fontWeight = if (selectedTab == LandingTab.POLICY) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigateToPolicy() }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (wide) {
                    OutlinedButton(
                        onClick = onExploreListings,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = LandingTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explore Listings", color = LandingTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = LandingBrandGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (signedIn) "Open dashboard" else "Sign in")
                }

                if (!wide) {
                    IconButton(onClick = onExploreListings) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            }
        }
    }
}
