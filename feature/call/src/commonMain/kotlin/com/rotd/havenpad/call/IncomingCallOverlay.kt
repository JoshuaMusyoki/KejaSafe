package com.rotd.havenpad.call

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.network.CallLogBus
import com.rotd.havenpad.network.IncomingCallCenter
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.notify.startHouseTourRingSound
import com.rotd.havenpad.notify.stopHouseTourRingSound
import kotlinx.coroutines.launch

/** Covers the app while a house tour is ringing. */
@Composable
fun IncomingCallOverlay(onAnswer: (sessionId: String) -> Unit) {
    val ringing by IncomingCallCenter.ringing.collectAsStateWithLifecycle()
    val call = ringing ?: return
    val scope = rememberCoroutineScope()
    DisposableEffect(call.sessionId) {
        startHouseTourRingSound()
        onDispose { stopHouseTourRingSound() }
    }
    HouseTourRingScreen(
        callerName = call.peerName.ifBlank { "House tour" },
        listingTitle = call.listingTitle,
        onAnswer = {
            stopHouseTourRingSound()
            IncomingCallCenter.suppress(call.sessionId)
            onAnswer(call.sessionId)
        },
        onDecline = {
            val id = call.sessionId
            stopHouseTourRingSound()
            IncomingCallCenter.clear()
            scope.launch {
                LiveTourClient.endTour(id)
                CallLogBus.ended()
            }
        },
    )
}

@Composable
fun HouseTourRingScreen(
    callerName: String,
    listingTitle: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06140F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Text(
                text = "House tour",
                color = Color(0xFF7DFFC3),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(scale)
                    .background(Color(0xFF00855D), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = callerName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = callerName,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (listingTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = listingTitle,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Incoming video call",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(56.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAnswer,
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Answer")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Answer", color = Color.White)
                }
            }
        }
    }
}
