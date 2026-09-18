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
import com.rotd.kejasafe.PlatformStats
import com.rotd.kejasafe.web.formatCount
import com.rotd.kejasafe.web.formatMoney
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingTextSecondary

/** Counters straight from `GET /stats`; dashes until the first response lands. */
@Composable
fun LandingStats(wide: Boolean, horizontalPadding: Dp, stats: PlatformStats?) {
    val statValues = listOf(
        (stats?.studentsHoused?.let(::formatCount) ?: PLACEHOLDER) to "Students Safely Housed",
        (stats?.verifiedListings?.let(::formatCount) ?: PLACEHOLDER) to "Verified Listings",
        (stats?.universitiesCovered?.let(::formatCount) ?: PLACEHOLDER) to "Universities Covered",
        (stats?.protectedInEscrow?.let { formatMoney(it) } ?: PLACEHOLDER) to "Rent Settled in Escrow"
    )

    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            statValues.forEachIndexed { index, stat ->
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        stat.first,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = LandingBrandGreen
                    )
                    Text(stat.second, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
                }
                if (index < statValues.size - 1) {
                    Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color(0xFFE5E7EB)))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            statValues.forEach { stat ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stat.first,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = LandingBrandGreen
                    )
                    Text(stat.second, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
                }
            }
        }
    }
}

private const val PLACEHOLDER = "—"
