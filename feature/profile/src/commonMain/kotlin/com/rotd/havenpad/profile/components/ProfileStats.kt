package com.rotd.havenpad.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.profile.kycStatusLabel
import com.rotd.havenpad.profile.models.ProfileStat
import com.rotd.havenpad.profile.opensKyc
import com.rotd.havenpad.profile.shortValue

fun profileStats(
    user: User,
    onEditProfile: () -> Unit,
    onOpenKyc: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenAdmin: () -> Unit,
): List<ProfileStat> {
    val kyc = ProfileStat(
        value = kycStatusLabel(user.kycStatus),
        label = "Verification",
        icon = Icons.Default.Verified,
        badge = if (user.kycStatus == KycStatus.VERIFIED) null else "Action",
        onClick = if (user.role.opensKyc()) onOpenKyc else null,
    )
    return when (user.role) {
        UserRole.STUDENT -> listOf(
            ProfileStat(
                value = user.yearOfStudy?.toString() ?: "—",
                label = "Year",
                icon = Icons.Default.School,
                onClick = onEditProfile,
            ),
            ProfileStat(
                value = user.roommatePreferences.size.toString(),
                label = "Preferences",
                icon = Icons.Default.Group,
                badge = if (user.roommatePreferences.isEmpty()) "Add" else null,
                onClick = onEditProfile,
            ),
            ProfileStat(
                value = shortValue(user.studentNumber),
                label = "Student no.",
                icon = Icons.Default.Badge,
            ),
            kyc,
        )
        UserRole.LANDLORD -> listOf(
            kyc,
            ProfileStat("View", "Bookings", Icons.Default.Event, onClick = onOpenBookings),
            ProfileStat("View", "Listings", Icons.Default.HomeWork, onClick = onOpenHome),
            ProfileStat(
                value = shortValue(user.businessName, empty = "Add"),
                label = "Business",
                icon = Icons.Default.Apartment,
                onClick = onEditProfile,
            ),
        )
        UserRole.AGENT -> listOf(
            ProfileStat(
                value = shortValue(user.serviceArea),
                label = "Service area",
                icon = Icons.Default.Place,
            ),
            ProfileStat(
                value = shortValue(user.licenseNumber),
                label = "License",
                icon = Icons.Default.Badge,
            ),
            kyc,
            ProfileStat(
                value = shortValue(user.phone, empty = "Add"),
                label = "Phone",
                icon = Icons.Default.Phone,
                onClick = onEditProfile,
            ),
        )
        UserRole.ADMIN -> listOf(
            ProfileStat("Open", "Admin", Icons.Default.Shield, onClick = onOpenAdmin),
            kyc,
            ProfileStat("View", "Listings", Icons.Default.HomeWork, onClick = onOpenHome),
            ProfileStat(
                value = shortValue(user.phone, empty = "Add"),
                label = "Phone",
                icon = Icons.Default.Person,
                onClick = onEditProfile,
            ),
        )
    }
}