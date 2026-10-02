package com.rotd.havenpad.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.DraftExtraCharge
import com.rotd.havenpad.LandmarkSuggestion
import com.rotd.havenpad.PendingListingPhoto
import com.rotd.havenpad.theme.HavenPadTheme

@Composable
fun HavenPadFormField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HavenPadDropdownField(
    label: String,
    selected: T,
    options: List<T>,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    HavenPadFormField(label = label, modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = optionLabel(selected),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Create Listing — form fields only. Host screen should pin the Continue action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateListingBasicsForm(
    title: String,
    onTitleChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    proximityLabel: String,
    proximityOptions: List<String>,
    onProximitySelected: (String) -> Unit,
    university: String,
    onUniversityChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAppBar: Boolean = true,
    pricePerMonth: String = "",
    onPriceChange: ((String) -> Unit)? = null,
    propertyTypeLabel: String = "Bedsitter",
    propertyTypeOptions: List<String> = emptyList(),
    onPropertyTypeSelected: ((String) -> Unit)? = null,
    unitsCount: String = "1",
    onUnitsCountChange: ((String) -> Unit)? = null,
    garbageCollection: String = "",
    onGarbageChange: ((String) -> Unit)? = null,
    water: String = "",
    onWaterChange: ((String) -> Unit)? = null,
    electricityIsToken: Boolean = true,
    onElectricityIsTokenChange: ((Boolean) -> Unit)? = null,
    electricityMonthly: String = "",
    onElectricityMonthlyChange: ((String) -> Unit)? = null,
    deposit: String = "",
    onDepositChange: ((String) -> Unit)? = null,
    extraCharges: List<DraftExtraCharge> = emptyList(),
    onAddExtraCharge: (() -> Unit)? = null,
    onExtraChargeNameChange: ((String, String) -> Unit)? = null,
    onExtraChargeAmountChange: ((String, String) -> Unit)? = null,
    onRemoveExtraCharge: ((String) -> Unit)? = null,
    monthlyEstimateLabel: String? = null,
    photos: List<PendingListingPhoto> = emptyList(),
    existingPhotos: List<com.rotd.havenpad.ListingPhotoResponse> = emptyList(),
    onAddPhotosClick: (() -> Unit)? = null,
    onRemovePhoto: ((String) -> Unit)? = null,
    onRemoveExistingPhoto: ((String) -> Unit)? = null,
    landmarkQuery: String = "",
    onLandmarkQueryChange: ((String) -> Unit)? = null,
    placeSuggestions: List<LandmarkSuggestion> = emptyList(),
    onLandmarkSelected: ((LandmarkSuggestion) -> Unit)? = null,
    isSearchingPlaces: Boolean = false,
    selectedLandmark: String = "",
    distanceMeters: String = "",
    onDistanceChange: ((String) -> Unit)? = null,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    showContinueButton: Boolean = true,
    onContinue: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showAppBar) {
            HavenPadAppBar(showProfile = false)
            Spacer(modifier = Modifier.height(8.dp))
        }

        HavenPadHeadline(text = "Property Basics")
        Spacer(modifier = Modifier.height(HavenPadTheme.spacing.elementGap))

        HavenPadFormField(label = "Property Title") {
            HavenPadTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "e.g. Modern Studio near Campus Center"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        HavenPadFormField(label = "Full Address") {
            HavenPadTextField(
                value = address,
                onValueChange = onAddressChange,
                placeholder = "123 University Ave, Boston, MA",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        if (onLandmarkQueryChange != null) {
            Spacer(modifier = Modifier.height(14.dp))
            HavenPadFormField(label = "Nearby landmark") {
                HavenPadTextField(
                    value = landmarkQuery,
                    onValueChange = onLandmarkQueryChange,
                    placeholder = "Road, market, or landmark",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Text(
                    text = "Suggestions appear as you type. Pick one, then say how far the house is.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (isSearchingPlaces) {
                    Text(
                        text = "Searching places…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (placeSuggestions.isNotEmpty() && onLandmarkSelected != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        placeSuggestions.forEach { suggestion ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLandmarkSelected(suggestion) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = suggestion.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (suggestion.description.isNotBlank() && suggestion.description != suggestion.name) {
                                        Text(
                                            text = suggestion.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectedLandmark.isNotBlank()) {
                    Text(
                        text = "Selected: $selectedLandmark",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (onDistanceChange != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HavenPadFormField(label = "Distance from that landmark (meters)") {
                    HavenPadTextField(
                        value = distanceMeters,
                        onValueChange = { onDistanceChange(it.filter { c -> c.isDigit() }) },
                        placeholder = "e.g. 200"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        HavenPadDropdownField(
            label = "Proximity to University",
            selected = proximityLabel,
            options = proximityOptions,
            onSelected = onProximitySelected,
            optionLabel = { it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        HavenPadFormField(label = "Primary University Nearby") {
            HavenPadTextField(
                value = university,
                onValueChange = onUniversityChange,
                placeholder = "e.g. Northeastern University"
            )
        }

        if (onPriceChange != null) {
            Spacer(modifier = Modifier.height(14.dp))
            HavenPadFormField(label = "Price per month (KES)") {
                HavenPadTextField(
                    value = pricePerMonth,
                    onValueChange = onPriceChange,
                    placeholder = "e.g. 15000"
                )
            }
        }

        if (onPropertyTypeSelected != null && propertyTypeOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            HavenPadDropdownField(
                label = "Property type",
                selected = propertyTypeLabel,
                options = propertyTypeOptions,
                onSelected = onPropertyTypeSelected,
                optionLabel = { it }
            )
        }

        if (onUnitsCountChange != null) {
            Spacer(modifier = Modifier.height(14.dp))
            HavenPadFormField(label = "Number of units") {
                HavenPadTextField(
                    value = unitsCount,
                    onValueChange = onUnitsCountChange,
                    placeholder = "e.g. 3"
                )
            }
        }

        if (onGarbageChange != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Bills & deposits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "These amounts apply when you generate a tenant bill. Totals update below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            HavenPadFormField(label = "Garbage collection (KES / mo)") {
                HavenPadTextField(
                    value = garbageCollection,
                    onValueChange = onGarbageChange,
                    placeholder = "e.g. 300"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HavenPadFormField(label = "Water (KES / mo)") {
                HavenPadTextField(
                    value = water,
                    onValueChange = onWaterChange ?: {},
                    placeholder = "e.g. 500"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Electricity is prepaid token",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (electricityIsToken) {
                            "Tenant tops up token — not billed monthly"
                        } else {
                            "Fixed amount included each month"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = electricityIsToken,
                    onCheckedChange = { onElectricityIsTokenChange?.invoke(it) }
                )
            }
            if (!electricityIsToken && onElectricityMonthlyChange != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HavenPadFormField(label = "Electricity (KES / mo)") {
                    HavenPadTextField(
                        value = electricityMonthly,
                        onValueChange = onElectricityMonthlyChange,
                        placeholder = "e.g. 1500"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (onDepositChange != null) {
                HavenPadFormField(label = "Security deposit (KES, one-time)") {
                    HavenPadTextField(
                        value = deposit,
                        onValueChange = onDepositChange,
                        placeholder = "e.g. 15000"
                    )
                }
            }
            if (onAddExtraCharge != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Other bills",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                extraCharges.forEach { charge ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HavenPadTextField(
                            value = charge.name,
                            onValueChange = { onExtraChargeNameChange?.invoke(charge.id, it) },
                            placeholder = "Name",
                            modifier = Modifier.weight(1.2f)
                        )
                        HavenPadTextField(
                            value = charge.amount,
                            onValueChange = {
                                onExtraChargeAmountChange?.invoke(
                                    charge.id,
                                    it.filter { c -> c.isDigit() || c == '.' }
                                )
                            },
                            placeholder = "KES",
                            modifier = Modifier.weight(0.8f)
                        )
                        IconButton(onClick = { onRemoveExtraCharge?.invoke(charge.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove charge")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onAddExtraCharge,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Add other bill")
                }
            }
            if (!monthlyEstimateLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = monthlyEstimateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (onAddPhotosClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            HavenPadFormField(label = "House Photos") {
                OutlinedButton(
                    onClick = onAddPhotosClick,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photos.isEmpty()) "Upload house photos" else "Add more photos")
                }

                if (existingPhotos.isNotEmpty() || photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // FlowRow avoids nested LazyRow scroll fights that can hide/block Continue.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        existingPhotos.forEach { photo ->
                            val label = photo.url.substringAfterLast('/').ifBlank { "Photo" }
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(100.dp)
                                    )
                                },
                                trailingIcon = if (onRemoveExistingPhoto != null) {
                                    {
                                        IconButton(
                                            onClick = { onRemoveExistingPhoto(photo.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove existing photo",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                        photos.forEach { photo ->
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = photo.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(100.dp)
                                    )
                                },
                                trailingIcon = if (onRemovePhoto != null) {
                                    {
                                        IconButton(
                                            onClick = { onRemovePhoto(photo.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    val status = buildList {
                        if (existingPhotos.isNotEmpty()) {
                            add("${existingPhotos.size} existing")
                        }
                        if (photos.isNotEmpty()) {
                            add("${photos.size} new to upload")
                        }
                    }.joinToString(" · ")
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        StudentInsightCard()

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (showContinueButton && onContinue != null) {
            Spacer(modifier = Modifier.height(24.dp))
            ContinueListingButton(
                isSubmitting = isSubmitting,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContinueListingButton(
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HavenPadPrimaryButton(
        text = if (isSubmitting) "Saving..." else "Continue",
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = modifier,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isSubmitting) "Saving..." else "Continue")
                if (!isSubmitting) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
