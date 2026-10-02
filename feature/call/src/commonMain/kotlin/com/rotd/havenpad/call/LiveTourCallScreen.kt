package com.rotd.havenpad.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.LiveTourStatus
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeaderContentInset
import com.rotd.havenpad.components.HavenPadPrimaryButton
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LiveTourCallScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: LiveTourCallViewModel = koinViewModel(key = "live-tour-$sessionId") {
        parametersOf(sessionId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val media = rememberLiveTourMediaSession()
    val connection by media.connectionState.collectAsStateWithLifecycle()
    val statusMessage by media.statusMessage.collectAsStateWithLifecycle()
    val micOn by media.micEnabled.collectAsStateWithLifecycle()
    val camOn by media.cameraEnabled.collectAsStateWithLifecycle()
    val requestJoin = rememberLiveTourPermissionGate {
        viewModel.join(media)
    }

    LaunchedEffect(state.ended) {
        if (state.ended) onBack()
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                state.error != null && state.session == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error.orEmpty(), color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = viewModel::refresh) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    val session = state.session
                    val canJoin = session?.status == LiveTourStatus.READY ||
                        session?.status == LiveTourStatus.ACTIVE
                    val videoOn = state.mediaType == LiveTourMediaType.VIDEO

                    Box(modifier = Modifier.fillMaxSize()) {
                        LiveTourVideoPane(
                            session = media,
                            showRemote = true,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (videoOn) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(
                                        top = HavenPadHeaderContentInset + 8.dp,
                                        end = 12.dp
                                    )
                                    .size(width = 120.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                LiveTourVideoPane(
                                    session = media,
                                    showRemote = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        LiveWatermark(
                            listingTitle = state.listingTitle,
                            viewerName = state.viewerName,
                            viewerId = state.viewerId,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = statusMessage
                                    ?: when (session?.status) {
                                        LiveTourStatus.SCHEDULED -> "Waiting for landlord to Confirm"
                                        LiveTourStatus.READY -> "Ready — join when both of you are free"
                                        LiveTourStatus.ACTIVE -> "Tour in progress"
                                        else -> "Live tour"
                                    },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!state.error.isNullOrBlank()) {
                                Text(
                                    text = state.error.orEmpty(),
                                    color = Color(0xFFFF8A80),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            when {
                                connection == LiveTourConnectionState.Connected ||
                                    connection == LiveTourConnectionState.Connecting -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { media.setMicEnabled(!micOn) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (micOn) Icons.Default.Mic else Icons.Default.MicOff,
                                                contentDescription = "Mic",
                                                tint = Color.White
                                            )
                                        }
                                        if (videoOn) {
                                            IconButton(
                                                onClick = { media.setCameraEnabled(!camOn) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = if (camOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                                    contentDescription = "Camera",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                        FloatingActionButton(
                                            onClick = { viewModel.endCall(media) },
                                            containerColor = Color(0xFFE53935),
                                            contentColor = Color.White
                                        ) {
                                            Icon(Icons.Default.CallEnd, contentDescription = "End")
                                        }
                                    }
                                }
                                canJoin -> {
                                    HavenPadPrimaryButton(
                                        text = if (state.isJoining) "Joining…" else "Join live tour",
                                        onClick = requestJoin,
                                        enabled = !state.isJoining,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "Landlord will Confirm this scheduled request first.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = viewModel::refresh) {
                                        Text("Refresh status")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HavenPadHeader(
                title = state.listingTitle.ifBlank { "Live tour" },
                onBackClick = {
                    media.disconnect()
                    onBack()
                },
                frosted = false,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
