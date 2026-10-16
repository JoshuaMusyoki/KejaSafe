package com.rotd.havenpad.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.profile.ProfileStatCard
import com.rotd.havenpad.profile.models.ProfileStat

@Composable
fun ProfileStatGrid(
    stats: List<ProfileStat>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val perRow = columns.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { stat ->
                    ProfileStatCard(
                        value = stat.value,
                        label = stat.label,
                        icon = stat.icon,
                        badge = stat.badge,
                        onClick = stat.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(perRow - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}