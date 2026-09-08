package com.rotd.kejasafe.web.components

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
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@Composable
fun LandingNavbar(
    onLogin: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToPolicy: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    "KejaSafe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LandingBrandGreen
                )

                if (wide) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            "Features",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LandingTextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigateToFeatures() }
                        )
                        Text(
                            "Policies & Privacy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LandingTextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onNavigateToPolicy() }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (wide) {
                    OutlinedButton(
                        onClick = { },
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
                    Text("Get the App")
                }

                if (!wide) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            }
        }
    }
}
