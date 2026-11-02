package com.rotd.havenpad.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.rotd.havenpad.web.models.PillarData
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingPillarMint
import com.rotd.havenpad.web.theme.LandingTextSecondary

@Composable
fun PillarCard(data: PillarData, modifier: Modifier) {
    Card(
        modifier = modifier.heightIn(min = 320.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(LandingPillarMint), contentAlignment = Alignment.Center) {
                Icon(data.icon, null, tint = LandingBrandGreen)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("PILLAR", style = MaterialTheme.typography.labelSmall, color = LandingTextSecondary, fontWeight = FontWeight.Bold)
            Text(data.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(data.desc, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(data.badge, style = MaterialTheme.typography.labelSmall, color = LandingTextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, null, tint = LandingBrandGreen, modifier = Modifier.size(12.dp))
            }
        }
    }
}