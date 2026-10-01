package com.rotd.kejasafe.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.LocationProvider
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeBottomSheet
import com.rotd.kejasafe.components.KejaSafeHeadline
import com.rotd.kejasafe.components.KejaSafeOutlinedButton
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationOnboardingSheet(
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPickManual: (lat: Double, lng: Double, name: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoadingHubs by remember { mutableStateOf(true) }
    var nearbyResidential by remember { mutableStateOf<List<Pair<String, Pair<Double, Double>>>>(emptyList()) }

    // Look up each place's own coordinates. Never reuse the device pin for a different area.
    LaunchedEffect(Unit) {
        isLoadingHubs = true
        scope.launch {
            val provider = LocationProvider()
            try {
                val userLocation = provider.getCurrentLocation()
                val query = if (userLocation != null) {
                    provider.getPlaceName(userLocation.latitude, userLocation.longitude) ?: "Campus"
                } else {
                    "Campus"
                }
                nearbyResidential = provider.findPlaces(query).map { place ->
                    place.name to (place.latitude to place.longitude)
                }
            } catch (e: Exception) {
                nearbyResidential = emptyList()
            } finally {
                isLoadingHubs = false
            }
        }
    }

    KejaSafeBottomSheet(onDismissRequest = onDismiss) {
        KejaSafeHeadline(text = "Find houses near you")
        Spacer(modifier = Modifier.height(8.dp))
        KejaSafeBody(text = "We can show you the best student housing sorted by distance from your current location or a specific campus.")

        Spacer(modifier = Modifier.height(24.dp))

        KejaSafePrimaryButton(
            text = "Use my current location",
            onClick = onUseCurrentLocation,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Or pick a campus hub",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingHubs) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (nearbyResidential.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nearbyResidential.take(4).forEach { (name, coords) ->
                    KejaSafeOutlinedButton(
                        text = name,
                        onClick = { onPickManual(coords.first, coords.second, name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Maybe later")
        }
    }
}
