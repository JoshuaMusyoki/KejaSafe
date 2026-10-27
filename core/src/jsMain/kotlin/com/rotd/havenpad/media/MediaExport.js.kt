package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val browserExportFile: (String, String, String, Boolean) -> String = js(
    """function(name, mime, b64, share) {
        function bytesOf(data) {
            var binary = atob(data);
            var out = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
            return out;
        }
        var blob = new Blob([bytesOf(b64)], { type: mime || "application/octet-stream" });
        var file = new File([blob], name, { type: mime || "application/octet-stream" });
        if (share && navigator.share && navigator.canShare && navigator.canShare({ files: [file] })) {
            navigator.share({ files: [file], title: name }).catch(function () {});
            return "Choose WhatsApp or another app";
        }
        var url = URL.createObjectURL(blob);
        var link = document.createElement("a");
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(function () { URL.revokeObjectURL(url); }, 1500);
        return share
            ? "Sharing opens from a phone browser. The file was downloaded instead."
            : "Saved to your downloads";
    }"""
)

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun rememberMediaExport(): MediaExport = remember {
    MediaExport(
        download = { fileName, mimeType, bytes ->
            browserExportFile(fileName, mimeType, Base64.encode(bytes), false)
        },
        share = { fileName, mimeType, bytes ->
            browserExportFile(fileName, mimeType, Base64.encode(bytes), true)
        },
    )
}

@Composable
actual fun ExpandedPdfPreview(bytes: ByteArray, modifier: Modifier) {
    PdfDocumentViewer(bytes = bytes, modifier = modifier)
}
