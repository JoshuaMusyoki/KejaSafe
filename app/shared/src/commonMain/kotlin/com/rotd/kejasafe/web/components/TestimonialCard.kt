package com.rotd.kejasafe.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingPillarMint
import com.rotd.kejasafe.web.theme.LandingTextDark
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@Composable
fun TestimonialCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "\"I transferred from overseas and had heard so many horror stories about fake landlords demanding upfront cash transfers. KejaSafe held my deposit in escrow until I personally unlocked the door. Zero anxiety.\"",
                style = MaterialTheme.typography.bodyMedium,
                color = LandingTextDark,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(LandingPillarMint), contentAlignment = Alignment.Center) {
                    Text("SK", fontWeight = FontWeight.Bold, color = LandingBrandGreen)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Sophia Kim", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Biochemistry, 2nd Year", style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
                }
            }
        }
    }
}