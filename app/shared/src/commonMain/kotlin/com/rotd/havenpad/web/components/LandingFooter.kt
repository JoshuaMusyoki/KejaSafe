package com.rotd.havenpad.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@Composable
fun LandingFooter(wide: Boolean, horizontalPadding: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(LandingBrandGreen))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HavenPad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Campus-verified student accommodation, escrow deposit protection, and verified video inspection tours built to safeguard students and landlords.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LandingTextSecondary,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(20.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("100% Escrow Protected & Verified Housing", style = MaterialTheme.typography.labelSmall, color = Color(0xFF92400E))
                    }
                }
            }

            if (wide) {
                FooterColumn("Product", listOf("Search Houses", "Roommate Finder", "Live Inspection", "Escrow Pay"), Modifier.weight(1f))
                FooterColumn("For Landlords", listOf("List Property", "Dashboard", "Verification"), Modifier.weight(1f))
                FooterColumn("Campus Support", listOf("Cambridge", "State University", "FAQs"), Modifier.weight(1f))
                FooterColumn("Legal & Safety", listOf("Trust Standards", "Terms of Service", "Privacy Policy", "Student Guarantor Guidelines"), Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
        HorizontalDivider(color = Color(0xFFF3F4F6))
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("© 2025 HavenPad Inc. All rights reserved.", style = MaterialTheme.typography.labelSmall, color = LandingTextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = LandingTextSecondary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Student Protection Shield", style = MaterialTheme.typography.labelSmall, color = LandingTextSecondary)
            }
        }
    }
}

@Composable
fun FooterColumn(title: String, items: List<String>, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            Text(item, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary, modifier = Modifier.padding(vertical = 4.dp).clickable { })
        }
    }
}
