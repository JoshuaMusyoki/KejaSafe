package com.rotd.havenpad.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated shimmer brush used for listing / house loading placeholders.
 */
@Composable
fun rememberShimmerBrush(
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
    highlightColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerShift"
    )
    val start = Offset(x = shift * 700f - 200f, y = 0f)
    val end = Offset(x = start.x + 280f, y = 220f)
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = start,
        end = end
    )
}

@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/** Bill card skeleton used while rent bills are loading. */
@Composable
fun BillCardShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    HavenPadCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBlock(modifier = Modifier.width(120.dp).height(28.dp), brush = brush)
                ShimmerBlock(modifier = Modifier.width(64.dp).height(24.dp), brush = brush)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp), brush = brush)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.45f).height(14.dp), brush = brush)
            Spacer(modifier = Modifier.height(16.dp))
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShimmerBlock(modifier = Modifier.width(100.dp).height(14.dp), brush = brush)
                    ShimmerBlock(modifier = Modifier.width(56.dp).height(14.dp), brush = brush)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                brush = brush,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun BillsListShimmer(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) { BillCardShimmer() }
    }
}

@Composable
fun TenantPropertyCardShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    HavenPadCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                brush = brush
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(110.dp)
                        .height(22.dp),
                    brush = brush
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    brush = brush
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                    brush = brush
                )
            }
        }
    }
}

/** Landlord listing card skeleton — matches [LandlordListingCard] geometry. */
@Composable
fun LandlordListingCardShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    HavenPadCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                shape = RoundedCornerShape(16.dp),
                brush = brush
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    brush = brush
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp),
                    brush = brush
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShimmerBlock(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        brush = brush
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        brush = brush
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    brush = brush
                )
            }
        }
    }
}

@Composable
fun ListingMetricShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    HavenPadCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBlock(
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp),
                brush = brush
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(
                modifier = Modifier
                    .width(72.dp)
                    .height(28.dp),
                brush = brush
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                shape = RoundedCornerShape(50),
                brush = brush
            )
        }
    }
}

@Composable
fun TenantListingsShimmer(
    count: Int = 3,
    itemSpacing: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        repeat(count) {
            TenantPropertyCardShimmer()
        }
    }
}

@Composable
fun LandlordListingsShimmer(
    count: Int = 2,
    itemSpacing: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        repeat(count) {
            LandlordListingCardShimmer()
        }
    }
}
