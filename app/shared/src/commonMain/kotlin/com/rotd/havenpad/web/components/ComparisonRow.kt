package com.rotd.havenpad.web.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.web.theme.LandingBrandGreen
import com.rotd.havenpad.web.theme.LandingTextSecondary

@Composable
fun ComparisonRow(factor: String, traditional: String, havenpad: String, isLast: Boolean = false) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(factor, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(traditional, style = MaterialTheme.typography.bodySmall, color = LandingTextSecondary)
            }
            Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = LandingBrandGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(havenpad, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}