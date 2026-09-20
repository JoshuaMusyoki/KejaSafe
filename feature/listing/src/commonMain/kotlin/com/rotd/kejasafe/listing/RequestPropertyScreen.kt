package com.rotd.kejasafe.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.components.KejaSafeDropdownField
import com.rotd.kejasafe.components.KejaSafeFormField
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.KejaSafeTextField
import com.rotd.kejasafe.data.PropertyRequestRepository
import com.rotd.kejasafe.domain.listing.DistancePreference
import com.rotd.kejasafe.domain.listing.HouseSize
import com.rotd.kejasafe.domain.listing.HouseType
import com.rotd.kejasafe.domain.listing.ListingType
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.ScreenPreviews

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RequestPropertyScreen(
    viewModel: RequestPropertyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Request a property", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KejaSafePrimaryButton(
                        text = if (state.isSubmitting) "Processing..." else "Pay KES 200 & place request",
                        onClick = { viewModel.submit() },
                        enabled = !state.isSubmitting && !state.completed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Describe the property you need. Every agent in that area is notified and uploads matches straight to you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            KejaSafeDropdownField(
                label = "What size?",
                selected = state.size,
                options = HouseSize.entries,
                onSelected = { viewModel.updateSize(it) },
                optionLabel = { it.displayName }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KejaSafeDropdownField(
                    label = "What kind of house? (optional)",
                    selected = state.houseType,
                    options = HouseType.entries + listOf(null),
                    onSelected = { viewModel.updateHouseType(it) },
                    optionLabel = { it?.displayName ?: "Any type" }
                )

                Text(
                    text = "e.g. a 1 Bedroom Apartment rather than a 1 Bedroom Bung..",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingType.entries.forEach { type ->
                    val selected = state.listingType == type
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateListingType(type) },
                        label = { Text(type.displayName) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            KejaSafeFormField(label = "Area") {
                KejaSafeTextField(
                    value = state.area,
                    onValueChange = { viewModel.updateArea(it) },
                    placeholder = "e.g. Rungiri, Kabete",
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How far can you go?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "from ${state.area}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DistancePreference.entries.forEach { dist ->
                        val selected = state.distance == dist
                        InputChip(
                            selected = selected,
                            onClick = { viewModel.updateDistance(dist) },
                            label = { Text(dist.displayName) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Text(
                text = "We will only send you houses within ${state.distance.km} km of there.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KejaSafeFormField(label = "Budget from (KES)", modifier = Modifier.weight(1f)) {
                    KejaSafeTextField(
                        value = state.budgetFrom,
                        onValueChange = { viewModel.updateBudgetFrom(it) },
                        placeholder = "10000"
                    )
                }
                KejaSafeFormField(label = "to (KES)", modifier = Modifier.weight(1f)) {
                    KejaSafeTextField(
                        value = state.budgetTo,
                        onValueChange = { viewModel.updateBudgetTo(it) },
                        placeholder = "15000"
                    )
                }
            }

            KejaSafeFormField(label = "Anything else? (optional)") {
                KejaSafeTextField(
                    value = state.additionalInfo,
                    onValueChange = { viewModel.updateAdditionalInfo(it) },
                    placeholder = "e.g. Near the main road, with balcony...",
                    singleLine = false,
                    modifier = Modifier.heightIn(min = 100.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agents can call me",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Off = you're only contacted through app notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.agentsCanCall,
                    onCheckedChange = { viewModel.updateAgentsCanCall(it) }
                )
            }

            if (!state.completed) {
                KejaSafeFormField(label = "M-Pesa Phone Number") {
                    KejaSafeTextField(
                        value = state.phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it) },
                        placeholder = "07xxxxxxxx"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3E0))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF57C00)
                    )
                    Text(
                        text = "KES 200. refunded in full if no agent responds within 14 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            state.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@ScreenPreviews
@Composable
private fun RequestPropertyScreenPreview() {
    KejaSafeTheme {
        RequestPropertyScreen(
            viewModel = RequestPropertyViewModel(PropertyRequestRepository()),
            onBack = {}
        )
    }
}
