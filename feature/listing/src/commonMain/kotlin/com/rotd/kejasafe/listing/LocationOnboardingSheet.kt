package com.rotd.kejasafe.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeBottomSheet
import com.rotd.kejasafe.components.KejaSafeHeadline
import com.rotd.kejasafe.components.KejaSafeOutlinedButton
import com.rotd.kejasafe.components.KejaSafePrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationOnboardingSheet(
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPickManual: (lat: Double, lng: Double, name: String) -> Unit
) {
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
        
        // Quick hubs in Nairobi / Thika / etc.
        val hubs = listOf(
            "Nairobi CBD" to (-1.2863 to 36.8172),
            "Juja (JKUAT)" to (-1.1018 to 37.0144),
            "Ruaraka (USIU/PAC)" to (-1.2212 to 36.8787),
            "Madaraka (Strathmore)" to (-1.3094 to 36.8126)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hubs.forEach { (name, coords) ->
                KejaSafeOutlinedButton(
                    text = name,
                    onClick = { onPickManual(coords.first, coords.second, name) },
                    modifier = Modifier.fillMaxWidth()
                )
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
