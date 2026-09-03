package com.rotd.kejasafe.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Moving semi-transparent identity watermark to deter casual screen recording / redistribution.
 * Does not prevent OS-level capture.
 */
@Composable
fun LiveWatermark(
    listingTitle: String,
    viewerName: String,
    viewerId: String,
    modifier: Modifier = Modifier,
) {
    var elapsedSec by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsedSec += 1
        }
    }
    val clock = formatElapsed(elapsedSec)
    val transition = rememberInfiniteTransition(label = "wm")
    val driftX by transition.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wmX",
    )
    val driftY by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wmY",
    )
    val line = buildString {
        append(listingTitle.ifBlank { "KejaSafe tour" })
        append(" · ")
        append(viewerName.ifBlank { "viewer" })
        append(" · ")
        append(viewerId.take(8))
        append(" · LIVE ")
        append(clock)
    }
    Box(modifier = modifier.fillMaxSize()) {
        WatermarkLine(
            text = line,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = driftX.dp, y = driftY.dp)
                .rotate(-18f)
        )
        WatermarkLine(
            text = line,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .offset(x = (-driftX).dp, y = driftY.dp)
                .rotate(12f)
        )
        WatermarkLine(
            text = "LIVE · do not record",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset(x = driftX.dp, y = (-driftY).dp)
        )
    }
}

@Composable
private fun WatermarkLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.38f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun formatElapsed(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
