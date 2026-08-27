package com.rotd.kejasafe.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.navigation.KejaSafeDestination

enum class KejaSafeNavTab {
    HOME, CHAT, BILLS, PROFILE
}

data class KejaSafeNavItem(
    val tab: KejaSafeNavTab,
    val label: String,
    val icon: ImageVector
)

val DefaultKejaSafeNavItems = listOf(
    KejaSafeNavItem(KejaSafeNavTab.HOME, "Home", Icons.Default.Home),
    KejaSafeNavItem(KejaSafeNavTab.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
    KejaSafeNavItem(KejaSafeNavTab.BILLS, "Bills", Icons.Default.Payments),
    KejaSafeNavItem(KejaSafeNavTab.PROFILE, "Profile", Icons.Default.Person)
)

fun homeDestinationForRole(role: UserRole?): KejaSafeDestination = when (role) {
    UserRole.LANDLORD, UserRole.ADMIN -> KejaSafeDestination.LandlordHome
    else -> KejaSafeDestination.TenantHome
}

fun destinationForTab(tab: KejaSafeNavTab, role: UserRole?): KejaSafeDestination = when (tab) {
    KejaSafeNavTab.HOME -> homeDestinationForRole(role)
    KejaSafeNavTab.CHAT -> KejaSafeDestination.Chat
    KejaSafeNavTab.BILLS -> KejaSafeDestination.Rent
    KejaSafeNavTab.PROFILE -> KejaSafeDestination.Profile
}

fun tabForDestination(destination: KejaSafeDestination): KejaSafeNavTab? = when (destination) {
    KejaSafeDestination.Home,
    KejaSafeDestination.TenantHome,
    KejaSafeDestination.LandlordHome -> KejaSafeNavTab.HOME
    KejaSafeDestination.Chat -> KejaSafeNavTab.CHAT
    KejaSafeDestination.Rent -> KejaSafeNavTab.BILLS
    KejaSafeDestination.Profile -> KejaSafeNavTab.PROFILE
    else -> null
}

/**
 * Floating pill-shaped bottom navigation used across main app pages.
 * Colors follow [MaterialTheme] so light/dark both look correct.
 */
@Composable
fun KejaSafeFloatingNavBar(
    selectedTab: KejaSafeNavTab,
    onTabSelected: (KejaSafeNavTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Landlord shortcut — opens create listing. Hidden when null. */
    onCreateListingClick: (() -> Unit)? = null,
    items: List<KejaSafeNavItem> = DefaultKejaSafeNavItems
) {
    val colors = MaterialTheme.colorScheme
    val barColor = colors.surface
    val barBorder = colors.outlineVariant.copy(alpha = 0.55f)
    val inactive = colors.onSurfaceVariant
    val activeLabel = colors.onSurface
    val pillShape = RoundedCornerShape(40.dp)

    // Landlord FAB shrinks the pill; keep item padding tight so "Profile" stays on one line.
    val hasFab = onCreateListingClick != null
    val itemHorizontalPadding = if (hasFab) 2.dp else 6.dp
    val pillHorizontalPadding = if (hasFab) 4.dp else 8.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .background(barColor)
                .border(1.dp, barBorder, pillShape)
                .padding(horizontal = pillHorizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTabSelected(item.tab) }
                        .padding(horizontal = itemHorizontalPadding, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colors.primary
                                else colors.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) colors.onPrimary else inactive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (selected) activeLabel else inactive,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (onCreateListingClick != null) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(onClick = onCreateListingClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new listing",
                    tint = colors.onPrimary
                )
            }
        }
    }
}
