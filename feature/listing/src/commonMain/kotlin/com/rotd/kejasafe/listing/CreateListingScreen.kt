package com.rotd.kejasafe.listing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.rotd.kejasafe.CampusProximity
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.components.ContinueListingButton
import com.rotd.kejasafe.components.CreateListingBasicsForm
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeHeaderContentInset
import com.rotd.kejasafe.media.rememberImagePickerLauncher
import com.rotd.kejasafe.rememberLocationPermissionState
import com.rotd.kejasafe.theme.KejaSafeTheme

@Composable
fun CreateListingScreen(
    listingId: String? = null,
    onBack: () -> Unit,
    onListingSaved: () -> Unit,
    viewModel: CreateListingViewModel = koinViewModel(key = listingId ?: "new") {
        parametersOf(listingId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickPhotos = rememberImagePickerLauncher(onImagesPicked = viewModel::addPickedPhotos)
    val proximityOptions = CampusProximity.entries
    val labels = proximityOptions.associateWith { it.displayLabel() }
    val propertyTypes = PropertyType.entries
    val typeLabels = propertyTypes.associateWith { it.displayLabel() }
    val scrollState = rememberScrollState()
    val isEdit = !listingId.isNullOrBlank()

    val locationPermission = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.requestDeviceLocation()
        }
    }

    // Clear any sticky post-save state before collecting save events (avoids open→flash→close).
    LaunchedEffect(listingId) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(viewModel) {
        viewModel.savedEvents.collect {
            onListingSaved()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!state.isLoading) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    ContinueListingButton(
                        isSubmitting = state.isSaving,
                        onClick = viewModel::submit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = KejaSafeHeaderContentInset + 48.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
                        .padding(top = KejaSafeHeaderContentInset + 8.dp, bottom = 16.dp)
                ) {
                    CreateListingBasicsForm(
                        title = state.draft.title,
                        onTitleChange = { value -> viewModel.updateDraft { it.copy(title = value) } },
                        address = state.draft.address,
                        onAddressChange = { value -> viewModel.updateDraft { it.copy(address = value) } },
                        proximityLabel = state.draft.proximity.displayLabel(),
                        proximityOptions = proximityOptions.map { it.displayLabel() },
                        onProximitySelected = { label ->
                            proximityOptions.firstOrNull { labels[it] == label }?.let { proximity ->
                                viewModel.updateDraft { it.copy(proximity = proximity) }
                            }
                        },
                        university = state.draft.university,
                        onUniversityChange = { value -> viewModel.updateDraft { it.copy(university = value) } },
                        showAppBar = false,
                        pricePerMonth = state.draft.pricePerMonth,
                        onPriceChange = { value ->
                            viewModel.updateDraft {
                                it.copy(pricePerMonth = value.filter { c -> c.isDigit() || c == '.' })
                            }
                        },
                        propertyTypeLabel = state.draft.propertyType.displayLabel(),
                        propertyTypeOptions = propertyTypes.map { it.displayLabel() },
                        onPropertyTypeSelected = { label ->
                            propertyTypes.firstOrNull { typeLabels[it] == label }?.let { type ->
                                viewModel.updateDraft { it.copy(propertyType = type) }
                            }
                        },
                        unitsCount = state.draft.unitsCount,
                        onUnitsCountChange = { value ->
                            viewModel.updateDraft {
                                it.copy(unitsCount = value.filter { c -> c.isDigit() })
                            }
                        },
                        garbageCollection = state.draft.garbageCollection,
                        onGarbageChange = { value ->
                            viewModel.updateDraft {
                                it.copy(garbageCollection = value.filter { c -> c.isDigit() || c == '.' })
                            }
                        },
                        water = state.draft.water,
                        onWaterChange = { value ->
                            viewModel.updateDraft {
                                it.copy(water = value.filter { c -> c.isDigit() || c == '.' })
                            }
                        },
                        electricityIsToken = state.draft.electricityIsToken,
                        onElectricityIsTokenChange = { checked ->
                            viewModel.updateDraft { it.copy(electricityIsToken = checked) }
                        },
                        electricityMonthly = state.draft.electricityMonthly,
                        onElectricityMonthlyChange = { value ->
                            viewModel.updateDraft {
                                it.copy(electricityMonthly = value.filter { c -> c.isDigit() || c == '.' })
                            }
                        },
                        deposit = state.draft.deposit,
                        onDepositChange = { value ->
                            viewModel.updateDraft {
                                it.copy(deposit = value.filter { c -> c.isDigit() || c == '.' })
                            }
                        },
                        extraCharges = state.draft.extraCharges,
                        onAddExtraCharge = viewModel::addExtraCharge,
                        onExtraChargeNameChange = viewModel::updateExtraChargeName,
                        onExtraChargeAmountChange = viewModel::updateExtraChargeAmount,
                        onRemoveExtraCharge = viewModel::removeExtraCharge,
                        monthlyEstimateLabel = state.draft.estimateSummary(),
                        photos = state.pendingPhotos,
                        existingPhotos = state.existingPhotos,
                        onAddPhotosClick = pickPhotos,
                        onRemovePhoto = viewModel::removePendingPhoto,
                        onRemoveExistingPhoto = viewModel::removeExistingPhoto,
                        latitude = state.draft.latitude,
                        onLatitudeChange = { lat -> viewModel.updateDraft { it.copy(latitude = lat) } },
                        longitude = state.draft.longitude,
                        onLongitudeChange = { lng -> viewModel.updateDraft { it.copy(longitude = lng) } },
                        onGetLocationClick = {
                            if (locationPermission.isGranted) {
                                viewModel.requestDeviceLocation()
                            } else {
                                locationPermission.launchPermissionRequest()
                            }
                        },
                        isSubmitting = state.isSaving,
                        errorMessage = state.error ?: state.syncNote,
                        showContinueButton = false
                    )
                }
            }

            KejaSafeHeader(
                title = if (isEdit) "Edit Listing" else "Create a Listing",
                onBackClick = onBack,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
