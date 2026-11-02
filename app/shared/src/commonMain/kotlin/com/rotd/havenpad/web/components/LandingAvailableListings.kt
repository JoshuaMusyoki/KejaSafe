package com.rotd.havenpad.web.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.components.TenantPropertyCardShimmer
import com.rotd.havenpad.web.formatCount
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingTextSecondary

/** Listings landlords actually published, newest first. */
@Composable
fun LandingAvailableListings(
    wide: Boolean,
    horizontalPadding: Dp,
    listings: List<PropertyListing>,
    loading: Boolean,
    error: String?,
    onExploreAll: () -> Unit,
    onInspect: (PropertyListing) -> Unit
) {
    val featured = listings.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 100.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    "FRESHLY VERIFIED",
                    style = MaterialTheme.typography.labelLarge,
                    color = LandingBrandGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Available for Upcoming Semester",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (listings.isNotEmpty()) {
                Text(
                    "View all ${formatCount(listings.size)} campus listings →",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LandingBrandGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onExploreAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        when {
            loading && featured.isEmpty() -> {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        repeat(3) { TenantPropertyCardShimmer(Modifier.weight(1f)) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(2) { TenantPropertyCardShimmer(Modifier.fillMaxWidth()) }
                    }
                }
            }

            featured.isEmpty() -> {
                Text(
                    error
                        ?: "No verified listings yet. Landlords publishing on HavenPad show up here first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LandingTextSecondary
                )
            }

            wide -> {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    featured.forEach { listing ->
                        PropertyCard(
                            listing = listing,
                            modifier = Modifier.weight(1f),
                            onInspect = { onInspect(listing) }
                        )
                    }
                    // Keep the row balanced when fewer than three listings exist.
                    repeat(3 - featured.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(featured, key = { it.id }) { listing ->
                        PropertyCard(
                            listing = listing,
                            modifier = Modifier.width(300.dp),
                            onInspect = { onInspect(listing) }
                        )
                    }
                }
            }
        }
    }
}
