package com.rotd.kejasafe.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.ScreenPreviews
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingBrandSecondary
import com.rotd.kejasafe.web.theme.LandingLightGreenBg
import com.rotd.kejasafe.web.theme.LandingPillarMint
import com.rotd.kejasafe.web.theme.LandingTextSecondary

@Composable
fun LandingComparison(wide: Boolean, horizontalPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Unmatched Protection",
            style = MaterialTheme.typography.labelLarge,
            color = LandingBrandGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Traditional Search vs. KejaSafe",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "See why over 15,000 students refuse to find off-campus housing anywhere else.",
            style = MaterialTheme.typography.bodyLarge,
            color = LandingBrandSecondary
        )

        Spacer(modifier = Modifier.height(60.dp))

        if (wide) {
            Card(
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Security & Experience Factor",
                            modifier = Modifier.weight(1.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Traditional Ads & Classifieds",
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .weight(1.2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KejaSafe Guarantee",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ComparisonRow("Deposit Safety", "High risk wire / cash scams", "Protected in Regulated Escrow")
                    ComparisonRow("Listing Authenticity", "Stolen photos & phantom flats", "Physical Audit + Live Walkthrough")
                    ComparisonRow("Roommate Quality", "Random roll of the dice", "Habit-Matching & ID-Vetted")
                    ComparisonRow("Dispute Resolution", "Costly legal small-claims", "Neutral KejaSafe Mediation", isLast = true)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                val items = listOf(
                    Triple("Deposit Safety", "High risk wire / cash scams", "Protected in Regulated Escrow"),
                    Triple("Listing Authenticity", "Stolen photos & phantom flats", "Physical Audit + Live Walkthrough"),
                    Triple("Roommate Quality", "Random roll of the dice", "Habit-Matching & ID-Vetted"),
                    Triple("Dispute Resolution", "Costly legal small-claims", "Neutral KejaSafe Mediation")
                )
                items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.first,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = LandingBrandGreen
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LandingTextSecondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LandingBrandGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.third,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LandingBrandGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
fun LandingComparisonPreview() {
    KejaSafeTheme {
        BoxWithConstraints {
            val wide = maxWidth >= 800.dp
            LandingComparison(
                wide = wide,
                horizontalPadding = if (wide) 80.dp else 20.dp
            )
        }
    }
}
