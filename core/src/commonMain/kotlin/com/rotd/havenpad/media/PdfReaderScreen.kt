package com.rotd.havenpad.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    fileName: String,
    bytes: ByteArray,
    onBack: () -> Unit,
    onShare: ((fileName: String, bytes: ByteArray) -> String)? = null,
    onDownload: ((fileName: String, bytes: ByteArray) -> String)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName.ifBlank { "PDF" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onDownload != null) {
                        IconButton(
                            onClick = {
                                val message = runCatching { onDownload(fileName, bytes) }
                                    .getOrElse { it.message ?: "Could not save the file" }
                                scope.launch { snackbar.showSnackbar(message) }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                    if (onShare != null) {
                        IconButton(
                            onClick = {
                                val message = runCatching { onShare(fileName, bytes) }
                                    .getOrElse { it.message ?: "Could not save PDF" }
                                scope.launch { snackbar.showSnackbar(message) }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Save / share PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        PdfDocumentViewer(
            bytes = bytes,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 8.dp),
        )
    }
}
