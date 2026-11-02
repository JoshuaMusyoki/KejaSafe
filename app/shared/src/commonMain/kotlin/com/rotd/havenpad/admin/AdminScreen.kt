package com.rotd.havenpad.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.AdminUserDetail
import com.rotd.havenpad.AdminUserSummary
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.utils.formatEpoch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminScreen(
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    viewModel: AdminViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showReject by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HavenPadHeader(
                title = if (state.detail == null) "Admin Console" else state.detail?.user?.fullName ?: "Account",
                onBackClick = if (state.detail == null) onBack else viewModel::closeDetail,
                actions = {
                    if (state.detail == null) {
                        TextButton(onClick = onLogout) {
                            Text("Sign out")
                        }
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val detail = state.detail
            when {
                detail != null -> AccountDetail(
                    detail = detail,
                    idImage = state.idImage,
                    addressImage = state.addressImage,
                    busy = state.busy,
                    error = state.error,
                    message = state.message,
                    onVerify = viewModel::verify,
                    onReject = { showReject = true },
                    onRemove = { showRemove = true },
                )
                else -> AccountList(
                    role = state.role,
                    reportsOpen = state.section == AdminListSection.REPORTS,
                    reports = state.reports,
                    loading = state.loading,
                    users = state.users,
                    error = state.error,
                    message = state.message,
                    busy = state.busy,
                    onRole = viewModel::load,
                    onOpenReports = viewModel::openReports,
                    onOpen = viewModel::open,
                    onAdd = { showAdd = true },
                    onFollowUp = viewModel::followUp,
                    onDismissReport = viewModel::dismissReport,
                    onDeleteReview = viewModel::deleteReview,
                )
            }
        }
    }

    if (showAdd) {
        AddLandlordDialog(
            busy = state.busy,
            onDismiss = { showAdd = false },
            onAdd = { name, username, email, password ->
                viewModel.addLandlord(name, username, email, password)
                showAdd = false
            },
        )
    }
    if (showReject) {
        RejectDialog(
            busy = state.busy,
            onDismiss = { showReject = false },
            onReject = { note ->
                viewModel.reject(note)
                showReject = false
            },
        )
    }
    if (showRemove) {
        AlertDialog(
            onDismissRequest = { showRemove = false },
            title = { Text("Remove Account", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently restrict sign-in access. Listings and records remain archived.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemove = false
                        viewModel.remove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemove = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AccountList(
    role: UserRole,
    reportsOpen: Boolean,
    reports: List<com.rotd.havenpad.StayReview>,
    loading: Boolean,
    users: List<AdminUserSummary>,
    error: String?,
    message: String?,
    busy: Boolean,
    onRole: (UserRole) -> Unit,
    onOpenReports: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onFollowUp: (String, String) -> Unit,
    onDismissReport: (String) -> Unit,
    onDeleteReview: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 800.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Tab Navigation for Role Filter
        val selectedIndex = when {
            reportsOpen -> 2
            role == UserRole.LANDLORD -> 0
            else -> 1
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = role == UserRole.LANDLORD && !reportsOpen,
                    onClick = { onRole(UserRole.LANDLORD) },
                    text = { Text("Landlords", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = role == UserRole.STUDENT && !reportsOpen,
                    onClick = { onRole(UserRole.STUDENT) },
                    text = { Text("Students", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = reportsOpen,
                    onClick = onOpenReports,
                    text = { Text("Reports", fontWeight = FontWeight.SemiBold) }
                )
            }
            if (role == UserRole.LANDLORD && !reportsOpen) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }

        if (!message.isNullOrBlank()) {
            StatusBanner(message = message, isError = false)
        }
        if (!error.isNullOrBlank()) {
            StatusBanner(message = error, isError = true)
        }

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            reportsOpen -> {
                AdminReviewReports(
                    reports = reports,
                    busy = busy,
                    error = error,
                    onFollowUp = onFollowUp,
                    onDismiss = onDismissReport,
                    onDelete = onDeleteReview,
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                )
            }
            users.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (role == UserRole.LANDLORD) "No registered landlords found." else "No registered students found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        UserCard(user = user, onClick = { onOpen(user.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: AdminUserSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.fullName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KycBadge(status = user.kycStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Joined ${formatEpoch(user.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountDetail(
    detail: AdminUserDetail,
    idImage: ImageBitmap?,
    addressImage: ImageBitmap?,
    busy: Boolean,
    error: String?,
    message: String?,
    onVerify: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
) {
    val user = detail.user
    var preview by remember { mutableStateOf<Pair<String, ImageBitmap>?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 800.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // User Banner Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.role.name.lowercase().replaceFirstChar { it.titlecase() }} · @${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    KycBadge(status = user.kycStatus)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Items
                DetailRow(icon = Icons.Default.Mail, text = user.email)
                user.phone?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Default.Phone, text = it)
                }
                user.businessName?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(icon = Icons.Default.Home, text = it)
                }
                if (detail.signedInDevices.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Default.Phone,
                        text = "Devices: ${detail.signedInDevices.joinToString(", ")}",
                    )
                }
                if (detail.residentialAddress.isNotBlank()) {
                    DetailRow(icon = Icons.Default.LocationOn, text = detail.residentialAddress)
                }
                if (detail.documentNumber.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Default.Badge,
                        text = "${detail.documentType.ifBlank { "ID" }}: ${detail.documentNumber}"
                    )
                }
                detail.reviewNote?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Review Note: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        detail.documentCheck?.let { check ->
            DocumentCheckCard(score = check.score, decision = check.decision, findings = check.findings)
        }

        Text(
            text = "Verification Documents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap a photo to open it and compare the printed details.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val student = user.role == UserRole.STUDENT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DocumentCard(
                    title = if (student) "Student ID" else "ID Copy",
                    image = idImage,
                    onFile = detail.hasIdCopy,
                    onOpen = idImage?.let { bitmap ->
                        { preview = (if (student) "Student ID" else "ID Copy") to bitmap }
                    },
                )
            }
            if (!student) {
                Box(modifier = Modifier.weight(1f)) {
                    DocumentCard(
                        title = "Proof of Address",
                        image = addressImage,
                        onFile = detail.hasAddressProof,
                        onOpen = addressImage?.let { bitmap ->
                            { preview = "Proof of Address" to bitmap }
                        },
                    )
                }
            }
        }

        // Admin Action Controls
        if (user.role == UserRole.LANDLORD || student) {
            val canVerify = detail.hasIdCopy && (student || detail.hasAddressProof)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onVerify,
                    enabled = !busy && canVerify,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify")
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject")
                }
            }
        }

        OutlinedButton(
            onClick = onRemove,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Remove Account")
        }

        if (!message.isNullOrBlank()) StatusBanner(message = message, isError = false)
        if (!error.isNullOrBlank()) StatusBanner(message = error, isError = true)

        // Activity Log Section
        Text(
            text = "Account activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Account creation, devices, and documents. Messages stay private.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (detail.activities.isEmpty()) {
            Text(
                text = "No recorded activity for this account yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                detail.activities.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                formatEpoch(item.at),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
    preview?.let { (title, bitmap) ->
        DocumentPreviewDialog(title = title, image = bitmap, onDismiss = { preview = null })
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DocumentCard(
    title: String,
    image: ImageBitmap?,
    onFile: Boolean,
    onOpen: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    image != null -> Image(
                        bitmap = image,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    onFile -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("On File", style = MaterialTheme.typography.labelMedium)
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Missing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCheckCard(score: Int, decision: String, findings: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Document check · $score%", fontWeight = FontWeight.Bold)
            Text(decision, style = MaterialTheme.typography.bodyMedium)
            findings.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DocumentPreviewDialog(title: String, image: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Image(
                    bitmap = image,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 640.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun KycBadge(status: KycStatus) {
    val (bgColor, textColor, label) = when (status) {
        KycStatus.VERIFIED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Verified")
        KycStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
        KycStatus.REJECTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Rejected")
        KycStatus.NOT_SUBMITTED -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), "Not Submitted")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean) {
    val bgColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val textColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}

@Composable
private fun AddLandlordDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Landlord", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, username, email, password) },
                enabled = !busy && name.isNotBlank() && username.isNotBlank() && email.contains("@") && password.length >= 8,
            ) { Text("Create Account") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RejectDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onReject: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Documents", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Reason for rejection") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onReject(note) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Reject") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}