package com.rotd.kejasafe.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.ListingStatus
import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.PropertyType

/**
 * Landlord-facing listing card: image, status badge, title, location, and actions.
 */
@Composable
fun LandlordListingCard(
    listing: PropertyListing,
    onEdit: () -> Unit,
    onViewAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    KejaSafeCard(modifier = modifier.fillMaxWidth()) {
        Column {
            ListingPhoto(
                imageUrl = listing.imageUrl,
                contentDescription = listing.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                ListingStatusBadge(
                    status = listing.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listing.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                listing.pricePerMonth?.let { price ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${listing.currency} ${price.toInt()}/mo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val unitMeta = listOfNotNull(
                    listing.propertyType?.let { typeLabel(it) },
                    listing.unitsCount?.let { n -> "$n unit${if (n == 1) "" else "s"}" }
                ).joinToString(" · ")
                if (unitMeta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = unitMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onViewAnalytics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            MaterialTheme.shapes.medium
                        ),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Analytics")
                }
            }
        }
    }
}

@Composable
fun TenantPropertyCard(
    listing: PropertyListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceText = listing.pricePerMonth?.let {
        "${listing.currency} ${it.toInt()}/mo"
    } ?: "Price on request"
    val typeBit = listing.propertyType?.let { typeLabel(it) }
    val unitsBit = listing.unitsCount?.let { n -> "$n unit${if (n == 1) "" else "s"}" }
    val location = listOfNotNull(listing.address, typeBit, unitsBit).joinToString(" · ")

    PropertyCard(
        imageUrl = listing.imageUrl.orEmpty(),
        price = priceText,
        title = listing.title,
        location = location,
        modifier = modifier,
        verifiedBadge = if (listing.status == ListingStatus.VERIFIED) {
            { ListingStatusBadge(status = ListingStatus.VERIFIED) }
        } else null,
        onClick = onClick
    )
}

private fun typeLabel(type: PropertyType): String = when (type) {
    PropertyType.SINGLE_ROOM -> "Single room"
    PropertyType.BEDSITTER -> "Bedsitter"
    PropertyType.ONE_BEDROOM -> "1 bedroom"
    PropertyType.TWO_BEDROOM -> "2 bedroom"
    PropertyType.THREE_BEDROOM -> "3 bedroom"
    PropertyType.MAISONETTE -> "Maisonette"
}
