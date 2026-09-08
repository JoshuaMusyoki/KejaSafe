package com.rotd.kejasafe.web.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.web.models.ListingMock
import com.rotd.kejasafe.web.theme.WebBrandGreen

@Composable
fun LandingAvailableListings(wide: Boolean, horizontalPadding: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 100.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("FRESHLY VERIFIED", style = MaterialTheme.typography.labelLarge, color = WebBrandGreen, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Available for Upcoming Semester", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            Text("View all 480+ campus listings →", style = MaterialTheme.typography.bodyMedium, color = WebBrandGreen, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { })
        }

        Spacer(modifier = Modifier.height(60.dp))

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PropertyCard(
                    ListingMock(
                        "Alder Quad - Studio Suite",
                        "$790",
                        "5 min to Med Faculty",
                        "Bills Included"
                    ), Modifier.weight(1f))
                PropertyCard(ListingMock("The Crescent Flatshares", "$640", "8 min to Law School", "2 Rooms Open"), Modifier.weight(1f))
                PropertyCard(ListingMock("Foundry Park Lofts", "$920", "12 min via Campus Shuttle", "Private Loft"), Modifier.weight(1f))
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(3) { i ->
                    PropertyCard(ListingMock("Listing $i", "$800", "5 min away", "Verified"), Modifier.width(300.dp))
                }
            }
        }
    }
}
