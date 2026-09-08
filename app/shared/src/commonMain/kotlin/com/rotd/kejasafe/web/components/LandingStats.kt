package com.rotd.kejasafe.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@Composable
 fun LandingStats(wide: Boolean, horizontalPadding: Dp) {
    val stats = listOf(
        "15,000+" to "Students Safely Housed",
        "99.8%" to "Scam Prevention Success",
        "45+" to "Universities Covered",
        "$4.2M" to "Protected in Escrow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 60.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { index, stat ->
            Column(horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally, modifier = if (!wide) Modifier.weight(1f) else Modifier) {
                Text(stat.first, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = LandingBrandGreen)
                Text(stat.second, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
            }
            if (index < stats.size - 1 && wide) {
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color(0xFFE5E7EB)))
            }
        }
    }
}