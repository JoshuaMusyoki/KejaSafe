package com.rotd.havenpad.media

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val evalHavenPdf: (String) -> Unit = js("function(code) { eval(code); }")
private val bindPdfDone: ((String) -> Unit) -> Unit = js(
    "function(callback) { globalThis.__havenPdfDone = callback; }"
)
private val kickPdf: (Int, String, Int, Int) -> Unit = js(
    "function(id, b64, page, maxW) { globalThis.__havenRenderPdf(id, b64, page, maxW); }"
)

private val pdfJson = Json { ignoreUnknownKeys = true }
private val pdfBridge = JsPdfBridge()

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun renderPdfPageForPreview(
    bytes: ByteArray,
    pageIndex: Int,
    maxWidthPx: Int,
): PdfPageImage? {
    val raw = pdfBridge.render(Base64.encode(bytes), pageIndex, maxWidthPx.coerceAtLeast(320))
    val wire = pdfJson.decodeFromString(PdfWireJs.serializer(), raw)
    if (wire.error.isNotBlank()) error(wire.error)
    val bitmap = Base64.decode(wire.png).decodeToImageBitmap() ?: return null
    return PdfPageImage(bitmap, wire.pages.coerceAtLeast(1))
}

@Serializable
private data class PdfWireJs(
    val pages: Int = 0,
    val png: String = "",
    val error: String = "",
)

private class JsPdfBridge {
    private val waiters = mutableMapOf<Int, CompletableDeferred<String>>()
    private var nextId = 0
    private var ready = false

    suspend fun render(base64: String, pageIndex: Int, maxWidthPx: Int): String {
        if (!ready) {
            evalHavenPdf(HAVEN_PDF_JS_INSTALL)
            bindPdfDone { payload ->
                val split = payload.indexOf('\n')
                if (split <= 0) return@bindPdfDone
                val id = payload.substring(0, split).toIntOrNull() ?: return@bindPdfDone
                waiters.remove(id)?.complete(payload.substring(split + 1))
            }
            ready = true
        }
        val id = ++nextId
        val deferred = CompletableDeferred<String>()
        waiters[id] = deferred
        kickPdf(id, base64, pageIndex, maxWidthPx)
        return withTimeout(30_000) { deferred.await() }
    }
}
