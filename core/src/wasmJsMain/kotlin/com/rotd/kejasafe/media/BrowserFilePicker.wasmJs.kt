package com.rotd.kejasafe.media

import com.rotd.kejasafe.PickedImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val pickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private val wireJson = Json { ignoreUnknownKeys = true }

private var onPayload: (String) -> Unit = {}
private var bridgeInstalled = false

internal fun openBrowserFilePicker(
    accept: String,
    onPicked: (List<PickedImage>) -> Unit,
) {
    ensurePickerBridge()
    val batch = mutableListOf<PickedImage>()
    onPayload = { payload ->
        if (payload.isEmpty()) {
            onPayload = {}
            val images = batch.toList()
            if (images.isNotEmpty()) {
                pickerScope.launch { onPicked(images) }
            }
        } else {
            decodePickedFile(payload)?.let { batch.add(it) }
        }
    }
    jsOpenFilePicker(accept)
}

private fun ensurePickerBridge() {
    if (bridgeInstalled) return
    bindPickerCallback { payload -> onPayload(payload) }
    bridgeInstalled = true
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodePickedFile(payload: String): PickedImage? = runCatching {
    val wire = wireJson.decodeFromString(PickedFileWire.serializer(), payload)
    val bytes = Base64.decode(wire.data)
    if (bytes.isEmpty()) return null
    val name = wire.name.ifBlank { "image.jpg" }
    PickedImage(
        fileName = name,
        bytes = bytes,
        mimeType = mimeTypeForPickedFile(name, wire.type),
    )
}.getOrNull()

@Serializable
private data class PickedFileWire(
    val name: String,
    val type: String,
    val data: String,
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(callback) => {
        globalThis.__kejasafeOnPickedFile = callback;
    }"""
)
private external fun bindPickerCallback(callback: (String) -> Unit)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(accept) => {
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.multiple = true;
        input.style.display = 'none';
        var deliver = function (payload) {
            var callback = globalThis.__kejasafeOnPickedFile;
            if (callback) callback(payload);
        };
        var finish = function () {
            if (input.parentNode) input.parentNode.removeChild(input);
        };
        input.addEventListener('cancel', function () {
            deliver('');
            finish();
        });
        input.onchange = async function () {
            try {
                var files = input.files ? Array.prototype.slice.call(input.files) : [];
                for (var i = 0; i < files.length; i++) {
                    var file = files[i];
                    var buf = new Uint8Array(await file.arrayBuffer());
                    var binary = '';
                    var chunk = 0x8000;
                    for (var offset = 0; offset < buf.length; offset += chunk) {
                        var slice = buf.subarray(offset, Math.min(offset + chunk, buf.length));
                        var parts = new Array(slice.length);
                        for (var j = 0; j < slice.length; j++) parts[j] = slice[j];
                        binary += String.fromCharCode.apply(null, parts);
                    }
                    deliver(JSON.stringify({
                        name: file.name || 'image.jpg',
                        type: file.type || '',
                        data: btoa(binary)
                    }));
                }
            } finally {
                deliver('');
                finish();
            }
        };
        (document.body || document.documentElement).appendChild(input);
        input.click();
    }"""
)
private external fun jsOpenFilePicker(accept: String)
