package com.rotd.kejasafe.web.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.ListingStatus
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.components.ListingPhoto
import com.rotd.kejasafe.listing.displayLabel
import com.rotd.kejasafe.web.formatCount
import com.rotd.kejasafe.web.formatMoney
import com.rotd.kejasafe.web.theme.LandingBrandGreen
import com.rotd.kejasafe.web.theme.LandingPillarMint
import com.rotd.kejasafe.web.theme.LandingTextDark
import com.rotd.kejasafe.web.theme.LandingTextSecondary

/** A real listing from `GET /listings`. */
@Composable
fun PropertyCard(
    listing: PropertyListing,
    modifier: Modifier,
    onInspect: () -> Unit = {}
) {
    val openUnits = ((listing.unitsCount ?: 0) - listing.activeBookings).coerceAtLeast(0)
    val tag = when {
        openUnits > 1 -> "$openUnits units open"
        openUnits == 1 -> "1 unit open"
        else -> listing.propertyType?.displayLabel() ?: "Fully booked"
    }
    val price = listing.pricePerMonth
        ?.let { formatMoney(it, listing.currency) }
        ?: "Price on request"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            ListingPhoto(
                imageUrl = listing.imageUrl ?: listing.imageUrls.firstOrNull(),
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentDescription = listing.title
            ) {
                if (listing.status == ListingStatus.VERIFIED) {
                    Surface(
                        modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "KejaSafe Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = LandingTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                listing.proximity?.let { proximity ->
                    Surface(
                        modifier = Modifier.padding(12.dp).align(Alignment.BottomStart),
                        color = LandingBrandGreen.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                proximity.displayLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) { append(price) }
                        if (listing.pricePerMonth != null) append(" / mo")
                    })
                    Surface(color = LandingPillarMint, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            tag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = LandingBrandGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        listing.address.ifBlank { null },
                        listing.university?.ifBlank { null },
                        listing.propertyType?.displayLabel()
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = LandingTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = LandingTextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${formatCount(listing.totalViews)} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = LandingTextSecondary
                        )
                    }
                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Inspect", color = LandingTextDark, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
