package com.rotd.havenpad.media

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val pdfJson = Json { ignoreUnknownKeys = true }
private val pdfBridge = BrowserPdfBridge(
    install = { evalHavenPdf(HAVEN_PDF_JS_INSTALL) },
    bind = ::bindPdfDone,
    kick = ::kickPdf,
)

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun renderPdfPageForPreview(
    bytes: ByteArray,
    pageIndex: Int,
    maxWidthPx: Int,
): PdfPageImage? {
    val raw = pdfBridge.render(Base64.encode(bytes), pageIndex, maxWidthPx.coerceAtLeast(320))
    val wire = pdfJson.decodeFromString(PdfWire.serializer(), raw)
    if (wire.error.isNotBlank()) error(wire.error)
    val bitmap = Base64.decode(wire.png).decodeToImageBitmap() ?: return null
    return PdfPageImage(bitmap, wire.pages.coerceAtLeast(1))
}

@Serializable
private data class PdfWire(
    val pages: Int = 0,
    val png: String = "",
    val error: String = "",
)

private class BrowserPdfBridge(
    private val install: () -> Unit,
    private val bind: ((String) -> Unit) -> Unit,
    private val kick: (Int, String, Int, Int) -> Unit,
) {
    private val waiters = mutableMapOf<Int, CompletableDeferred<String>>()
    private var nextId = 0
    private var ready = false

    suspend fun render(base64: String, pageIndex: Int, maxWidthPx: Int): String {
        if (!ready) {
            install()
            bind { payload ->
                val split = payload.indexOf('\n')
                if (split <= 0) return@bind
                val id = payload.substring(0, split).toIntOrNull() ?: return@bind
                waiters.remove(id)?.complete(payload.substring(split + 1))
            }
            ready = true
        }
        val id = ++nextId
        val deferred = CompletableDeferred<String>()
        waiters[id] = deferred
        kick(id, base64, pageIndex, maxWidthPx)
        return withTimeout(30_000) { deferred.await() }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(code) => { eval(code); }")
private external fun evalHavenPdf(code: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(callback) => { globalThis.__havenPdfDone = callback; }")
private external fun bindPdfDone(callback: (String) -> Unit)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(id, b64, page, maxW) => { globalThis.__havenRenderPdf(id, b64, page, maxW); }")
private external fun kickPdf(id: Int, b64: String, page: Int, maxW: Int)
