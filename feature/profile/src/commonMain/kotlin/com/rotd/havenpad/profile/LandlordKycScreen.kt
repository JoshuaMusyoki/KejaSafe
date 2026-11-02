package com.rotd.havenpad.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.LandlordKycRequest
import com.rotd.havenpad.PickedImage
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadOutlinedButton
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.HavenPadTextField
import com.rotd.havenpad.media.decodeToImageBitmap
import com.rotd.havenpad.media.rememberImagePickerLauncher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun LandlordKycScreen(
    allowSkip: Boolean,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: LandlordKycViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var documentType by remember { mutableStateOf("National ID") }
    var documentNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var idImage by remember { mutableStateOf<PickedImage?>(null) }
    var addressImage by remember { mutableStateOf<PickedImage?>(null) }
    var idPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var addressPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    val pickId = rememberImagePickerLauncher { images ->
        val image = images.firstOrNull() ?: return@rememberImagePickerLauncher
        if (image.bytes.size > 3 * 1024 * 1024) {
            formError = "The ID photo must be 3MB or smaller."
            return@rememberImagePickerLauncher
        }
        idImage = image
        formError = null
    }
    val pickAddress = rememberImagePickerLauncher { images ->
        val image = images.firstOrNull() ?: return@rememberImagePickerLauncher
        if (image.bytes.size > 3 * 1024 * 1024) {
            formError = "The address photo must be 3MB or smaller."
            return@rememberImagePickerLauncher
        }
        addressImage = image
        formError = null
    }

    LaunchedEffect(idImage) {
        idPreview = idImage?.bytes?.decodeToImageBitmap()
    }
    LaunchedEffect(addressImage) {
        addressPreview = addressImage?.bytes?.decodeToImageBitmap()
    }
    LaunchedEffect(state.record, prefilled) {
        val record = state.record ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        if (record.documentType == "Passport" || record.documentType == "National ID") {
            documentType = record.documentType
        }
        if (record.documentNumber.isNotBlank()) documentNumber = record.documentNumber
        if (record.residentialAddress.isNotBlank()) address = record.residentialAddress
        prefilled = true
    }
    LaunchedEffect(state.done) {
        if (state.done) onFinished()
    }

    Scaffold(
        topBar = {
            HavenPadHeader(
                title = "Identity and address",
                onBackClick = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HavenPadBody(
                    text = "Upload a photo of your ID and a photo that shows where you live, such as a utility bill or lease. You can skip this and add it later from your profile."
                        .takeIf { allowSkip }
                        ?: "Upload a photo of your ID and a photo that shows where you live, such as a utility bill or lease. HavenPad reviews this before the landlord badge is verified."
                )
                val record = state.record
                if (record?.status == KycStatus.PENDING) {
                    Text("Your documents are with the HavenPad team.", color = MaterialTheme.colorScheme.primary)
                }
                if (record?.status == KycStatus.VERIFIED) {
                    Text("This landlord account is verified. Sending new documents puts it back in review.", color = MaterialTheme.colorScheme.primary)
                }
                record?.reviewNote?.takeIf { it.isNotBlank() && record.status == KycStatus.REJECTED }?.let { note ->
                    Text(note, color = MaterialTheme.colorScheme.error)
                }
                if (state.loading && record == null) {
                    CircularProgressIndicator()
                }
                DocumentNumberField(
                    documentType = documentType,
                    onDocumentType = { documentType = it },
                    number = documentNumber,
                    onNumberChange = { documentNumber = it },
                )
                HavenPadTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Residential address",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                )
                DocumentPicker(
                    title = "Copy of ID",
                    fileName = idImage?.fileName ?: if (record?.hasIdCopy == true) "A copy is already on file" else "No photo yet",
                    preview = idPreview,
                    onPick = pickId,
                )
                DocumentPicker(
                    title = "Proof of address",
                    fileName = addressImage?.fileName ?: if (record?.hasAddressProof == true) "A copy is already on file" else "No photo yet",
                    preview = addressPreview,
                    onPick = pickAddress,
                )
                val message = formError ?: state.error
                if (!message.isNullOrBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                HavenPadPrimaryButton(
                    text = if (state.submitting) "Sending…" else "Submit for review",
                    onClick = {
                        val id = idImage
                        val proof = addressImage
                        when {
                            documentNumber.trim().length < 4 ->
                                formError = if (documentType == "Passport") {
                                    "Enter the passport number."
                                } else {
                                    "Enter the ID number."
                                }
                            address.trim().length < 8 -> formError = "Enter the full residential address."
                            id == null -> formError = "Add a photo of the ID."
                            proof == null -> formError = "Add a photo of your proof of address."
                            else -> {
                                formError = null
                                viewModel.submit(
                                    LandlordKycRequest(
                                        documentType = documentType,
                                        documentNumber = documentNumber.trim(),
                                        residentialAddress = address.trim(),
                                        idImageBase64 = Base64.encode(id.bytes),
                                        idImageName = id.fileName,
                                        addressProofBase64 = Base64.encode(proof.bytes),
                                        addressProofName = proof.fileName,
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.submitting,
                )
                if (allowSkip) {
                    HavenPadOutlinedButton(
                        text = "Skip for now",
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.submitting,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentNumberField(
    documentType: String,
    onDocumentType: (String) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val passport = documentType == "Passport"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        val typeAnchor = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        HavenPadTextField(
            value = number,
            onValueChange = onNumberChange,
            label = if (passport) "Passport number" else "ID number",
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    modifier = typeAnchor.padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (passport) "Passport" else "National ID",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("ID number") },
                onClick = {
                    onDocumentType("National ID")
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Passport number") },
                onClick = {
                    onDocumentType("Passport")
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun DocumentPicker(
    title: String,
    fileName: String,
    preview: ImageBitmap?,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HavenPadOutlinedButton(
            text = "Choose photo",
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
