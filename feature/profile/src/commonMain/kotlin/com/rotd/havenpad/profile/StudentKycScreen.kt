package com.rotd.havenpad.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.PickedImage
import com.rotd.havenpad.StudentKycRequest
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
fun StudentKycScreen(
    allowSkip: Boolean,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: StudentKycViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var university by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var idImage by remember { mutableStateOf<PickedImage?>(null) }
    var idPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    val pickId = rememberImagePickerLauncher { images ->
        val image = images.firstOrNull() ?: return@rememberImagePickerLauncher
        if (image.bytes.size > 3 * 1024 * 1024) {
            formError = "The student ID photo must be 3MB or smaller."
            return@rememberImagePickerLauncher
        }
        idImage = image
        formError = null
    }

    LaunchedEffect(idImage) {
        idPreview = idImage?.bytes?.decodeToImageBitmap()
    }
    LaunchedEffect(state.record, prefilled) {
        val record = state.record ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        if (record.documentType == "Student ID" || record.documentType.isBlank()) {
            if (record.residentialAddress.isNotBlank()) university = record.residentialAddress
            if (record.documentNumber.isNotBlank()) studentNumber = record.documentNumber
        }
        prefilled = true
    }
    LaunchedEffect(state.done) {
        if (state.done) onFinished()
    }

    Scaffold(
        topBar = {
            HavenPadHeader(
                title = "Student ID",
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
                    text = if (allowSkip) {
                        "Upload a photo of your university student ID. You can skip this and add it later from your profile."
                    } else {
                        "Upload a photo of your university student ID. HavenPad reviews it before the student badge is verified."
                    }
                )
                val record = state.record
                if (record?.status == KycStatus.PENDING && record.documentType == "Student ID") {
                    Text("Your student ID is with the HavenPad team.", color = MaterialTheme.colorScheme.primary)
                }
                if (record?.status == KycStatus.VERIFIED && record.documentType == "Student ID") {
                    Text(
                        "This student account is verified. Sending a new ID puts it back in review.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                record?.reviewNote?.takeIf { it.isNotBlank() && record.status == KycStatus.REJECTED }?.let { note ->
                    Text(note, color = MaterialTheme.colorScheme.error)
                }
                if (state.loading && record == null) {
                    CircularProgressIndicator()
                }
                HavenPadTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = "University",
                    modifier = Modifier.fillMaxWidth(),
                )
                HavenPadTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = "Student number",
                    modifier = Modifier.fillMaxWidth(),
                )
                DocumentPicker(
                    title = "Student ID",
                    fileName = idImage?.fileName
                        ?: if (record?.hasIdCopy == true && record.documentType == "Student ID") {
                            "A copy is already on file"
                        } else {
                            "No photo yet"
                        },
                    preview = idPreview,
                    onPick = pickId,
                )
                val message = formError ?: state.error
                if (!message.isNullOrBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                HavenPadPrimaryButton(
                    text = if (state.submitting) "Sending…" else "Submit for review",
                    onClick = {
                        val id = idImage
                        when {
                            university.trim().length < 2 -> formError = "Enter your university."
                            studentNumber.trim().length < 3 -> formError = "Enter your student number."
                            id == null -> formError = "Add a photo of your student ID."
                            else -> {
                                formError = null
                                viewModel.submit(
                                    StudentKycRequest(
                                        university = university.trim(),
                                        studentNumber = studentNumber.trim(),
                                        idImageBase64 = Base64.encode(id.bytes),
                                        idImageName = id.fileName,
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
