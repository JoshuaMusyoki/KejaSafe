package com.rotd.kejasafe.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.rotd.kejasafe.ListingAnalytics
import com.rotd.kejasafe.ListingDayStat
import com.rotd.kejasafe.components.InsightCard
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeCard
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeHeaderContentInset
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.ListingStatusBadge
import com.rotd.kejasafe.components.NewInquiriesBadge
import com.rotd.kejasafe.components.PerformanceMetricCard
import com.rotd.kejasafe.components.ViewsMetricBadge
import com.rotd.kejasafe.listing.viewModel.ListingAnalyticsViewModel
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.Slate900

@Composable
fun ListingAnalyticsScreen(
    listingId: String,
    onBack: () -> Unit,
    onEditListing: (String) -> Unit = {},
    viewModel: ListingAnalyticsViewModel = koinViewModel(key = "analytics-$listingId") {
        parametersOf(listingId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.analytics == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.analytics == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(top = KejaSafeHeaderContentInset + 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        KejaSafePrimaryButton(text = "Retry", onClick = { viewModel.load() })
                    }
                }
                state.analytics != null -> {
                    AnalyticsContent(
                        analytics = state.analytics!!,
                        onEditListing = { onEditListing(listingId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            KejaSafeHeader(
                title = "Analytics",
                onBackClick = onBack,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    analytics: ListingAnalytics,
    onEditListing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val units = analytics.unitsCount.coerceAtLeast(1)
    val filledSegments = (
        (analytics.activeBookings.toFloat() / units) * 3f
        ).toInt().coerceIn(0, 3)
    val priceLabel = analytics.pricePerMonth?.let {
        "${analytics.currency} ${it.toInt()}/mo"
    }
    val typeLabel = analytics.propertyType?.let {
        when (it) {
            com.rotd.kejasafe.PropertyType.SINGLE_ROOM -> "Single room"
            com.rotd.kejasafe.PropertyType.BEDSITTER -> "Bedsitter"
            com.rotd.kejasafe.PropertyType.ONE_BEDROOM -> "1 bedroom"
            com.rotd.kejasafe.PropertyType.TWO_BEDROOM -> "2 bedroom"
            com.rotd.kejasafe.PropertyType.THREE_BEDROOM -> "3 bedroom"
            com.rotd.kejasafe.PropertyType.MAISONETTE -> "Maisonette"
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
            .padding(top = KejaSafeHeaderContentInset + 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = analytics.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ListingStatusBadge(status = analytics.status)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = analytics.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val meta = listOfNotNull(
                analytics.university,
                priceLabel,
                typeLabel,
                "${analytics.unitsCount} unit${if (analytics.unitsCount == 1) "" else "s"}"
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                KejaSafeBody(text = meta)
            }
        }

        PerformanceMetricCard(
            title = "House Viewers",
            value = formatWithCommas(analytics.totalViews),
            valueColor = Emerald700,
            badge = { ViewsMetricBadge(deltaPercent = analytics.viewsDeltaPercent) },
            progress = (analytics.totalViews / 20f).coerceIn(0.08f, 1f),
            footer = "People who viewed this listing"
        )

        PerformanceMetricCard(
            title = "Inquiries",
            value = analytics.inquiries.toString(),
            valueColor = Gold600,
            accentColor = Gold600,
            badge = if (analytics.hasNewInquiries) {
                { NewInquiriesBadge() }
            } else {
                null
            },
            footer = if (analytics.conversionRatePercent > 0) {
                "${analytics.conversionRatePercent}% viewer → inquiry conversion"
            } else {
                "Unique tenants who requested a booking"
            }
        )

        PerformanceMetricCard(
            title = "Active Bookings",
            value = analytics.activeBookings.toString(),
            valueColor = Slate900,
            trailingLabel = "Units: $units",
            segmentedProgress = filledSegments to 3,
            footer = "${analytics.occupancyPercent}% occupancy"
        )

        WeeklyViewsCard(weekly = analytics.weekly)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                label = "Conversion",
                value = "${analytics.conversionRatePercent}%",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Occupancy",
                value = "${analytics.occupancyPercent}%",
                modifier = Modifier.weight(1f)
            )
        }

        InsightCard(
            title = insightTitle(analytics),
            message = insightMessage(analytics)
        )

        KejaSafePrimaryButton(
            text = "Edit listing",
            onClick = onEditListing,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WeeklyViewsCard(weekly: List<ListingDayStat>) {
    val maxViews = weekly.maxOfOrNull { it.views }?.coerceAtLeast(1) ?: 1

    KejaSafeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Views this week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily student traffic for this house",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (weekly.all { it.views == 0 }) {
                Text(
                    text = "No views recorded yet. Once students open this listing, daily activity will show here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekly.forEach { day ->
                        val fraction = (day.views.toFloat() / maxViews).coerceIn(0.06f, 1f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = day.views.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(Emerald700)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    KejaSafeCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Emerald700
            )
        }
    }
}

private fun insightTitle(analytics: ListingAnalytics): String {
    return when {
        analytics.totalViews == 0 -> "Get your first views"
        analytics.conversionRatePercent >= 12 -> "Strong conversion"
        analytics.occupancyPercent >= 80 -> "Nearly full"
        else -> "Grow student interest"
    }
}

private fun insightMessage(analytics: ListingAnalytics): String {
    return when {
        analytics.totalViews == 0 ->
            "Verified students search by university and proximity. Add clear photos and a campus-friendly title to start the funnel."
        analytics.conversionRatePercent >= 12 ->
            "This house converts well. Keep response time under a day and open live inspection slots to lock bookings."
        analytics.occupancyPercent >= 80 ->
            "Most rooms are filled. Consider raising rate slightly or opening a waitlist for the next term."
        analytics.inquiries == 0 ->
            "People are viewing but not inquiring. Lead with price, unit type, and walk time from campus in the title."
        else ->
            "Listings with 4+ house photos and a nearby university tag tend to earn more inquiries week over week."
    }
}

private fun formatWithCommas(value: Int): String =
    value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
