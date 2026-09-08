package com.rotd.kejasafe.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.theme.WebBrandGreen

@Composable
fun LandingComparison(wide: Boolean, horizontalPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("UNMATCHED PROTECTION", style = MaterialTheme.typography.labelLarge, color = WebBrandGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Traditional Search vs. KejaSafe", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("See why over 15,000 students refuse to find off-campus housing anywhere else.", style = MaterialTheme.typography.bodyLarge, color = WebBrandGreen)

        Spacer(modifier = Modifier.height(60.dp))

        Card(
            modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Header
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).padding(24.dp)) {
                    Text("Security & Experience Factor", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                    Text("Traditional Ads & Classifieds", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = WebBrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KejaSafe Guarantee", color = WebBrandGreen, fontWeight = FontWeight.Bold)
                    }
                }

                ComparisonRow("Deposit Safety", "High risk wire / cash scams", "Protected in Regulated Escrow")
                ComparisonRow("Listing Authenticity", "Stolen photos & phantom flats", "Physical Audit + Live Walkthrough")
                ComparisonRow("Roommate Quality", "Random roll of the dice", "Habit-Matching & ID-Vetted")
                ComparisonRow("Dispute Resolution", "Costly legal small-claims", "Neutral KejaSafe Mediation", isLast = true)
            }
        }
    }
}