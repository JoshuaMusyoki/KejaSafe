package com.rotd.havenpad.listing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.components.ContinueListingButton
import com.rotd.havenpad.components.CreateListingBasicsForm
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeaderContentInset
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.media.rememberImagePickerLauncher
import com.rotd.havenpad.theme.HavenPadTheme

private val ListingFormMaxWidth = 560.dp

@Composable
fun CreateListingScreen(
    listingId: String? = null,
    onBack: () -> Unit,
    onListingSaved: () -> Unit,
    onOpenVerification: () -> Unit = {},
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
    val canPost = isEdit || state.kycStatus == KycStatus.VERIFIED
    val waitingOnVerification = !isEdit && state.checkingVerification

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
            if (!state.isLoading && !waitingOnVerification && canPost) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ContinueListingButton(
                            isSubmitting = state.isSaving,
                            onClick = viewModel::submit,
                            modifier = Modifier
                                .widthIn(max = ListingFormMaxWidth)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = HavenPadTheme.spacing.containerMarginMobile, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            if (state.isLoading || waitingOnVerification) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = HavenPadHeaderContentInset + 48.dp)
                    )
                }
            } else if (!canPost) {
                ListingVerificationGate(
                    status = state.kycStatus,
                    onOpenVerification = onOpenVerification,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = HavenPadHeaderContentInset + 24.dp, bottom = 24.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ListingFormMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = HavenPadTheme.spacing.containerMarginMobile)
                        .padding(top = HavenPadHeaderContentInset + 8.dp, bottom = 16.dp)
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
                        campusSuggestions = state.campusSuggestions,
                        isLoadingCampuses = state.isLoadingCampuses,
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
                        landmarkQuery = state.draft.landmarkQuery,
                        onLandmarkQueryChange = viewModel::onLandmarkQueryChange,
                        placeSuggestions = state.placeSuggestions,
                        onLandmarkSelected = viewModel::selectLandmark,
                        isSearchingPlaces = state.isSearchingPlaces,
                        selectedLandmark = state.draft.landmarkName,
                        distanceMeters = state.draft.distanceMeters,
                        onDistanceChange = { value ->
                            viewModel.updateDraft { it.copy(distanceMeters = value.filter { c -> c.isDigit() }) }
                        },
                        isSubmitting = state.isSaving,
                        errorMessage = state.error ?: state.syncNote,
                        showContinueButton = false
                    )
                }
                }
            }

            HavenPadHeader(
                title = if (isEdit) "Edit Listing" else "Create a Listing",
                onBackClick = onBack,
                frosted = true,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun ListingVerificationGate(
    status: KycStatus,
    onOpenVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (status) {
        KycStatus.PENDING -> "Verification is in review"
        KycStatus.REJECTED -> "Verification needs an update"
        else -> "Verify your identity first"
    }
    val body = when (status) {
        KycStatus.PENDING ->
            "Your ID and proof of address are with the HavenPad team. You can post a house once that review is complete."
        KycStatus.REJECTED ->
            "The last documents were not accepted. Upload a clear ID and proof of address, then wait for a new review."
        else ->
            "Upload a copy of your ID and proof of address. After HavenPad verifies them, you can post a house."
    }
    val action = when (status) {
        KycStatus.PENDING -> "View verification"
        KycStatus.REJECTED -> "Update documents"
        else -> "Verify your identity"
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ListingFormMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = HavenPadTheme.spacing.containerMarginMobile)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            HavenPadPrimaryButton(
                text = action,
                onClick = onOpenVerification,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
