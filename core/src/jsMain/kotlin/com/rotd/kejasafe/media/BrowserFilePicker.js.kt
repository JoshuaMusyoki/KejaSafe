package com.rotd.kejasafe.media

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import com.rotd.kejasafe.PickedImage

private val pickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

internal fun openBrowserFilePicker(
    accept: String,
    onPicked: (List<PickedImage>) -> Unit,
) {
    val body = document.body ?: return
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.multiple = true
    input.style.display = "none"
    input.addEventListener("change", {
        val selected = input.files
        input.remove()
        if (selected == null || selected.length == 0) return@addEventListener
        val pending = Array(selected.length) { index -> selected[index] }
        readFiles(pending.filterNotNull(), onPicked)
    })
    input.addEventListener("cancel", { input.remove() })
    body.appendChild(input)
    input.click()
}

private fun readFiles(files: List<File>, onPicked: (List<PickedImage>) -> Unit) {
    if (files.isEmpty()) return
    val images = arrayOfNulls<PickedImage>(files.size)
    var remaining = files.size
    files.forEachIndexed { index, file ->
        readFile(file) { image ->
            images[index] = image
            remaining -= 1
            if (remaining == 0) {
                val ready = images.filterNotNull()
                if (ready.isNotEmpty()) {
                    pickerScope.launch { onPicked(ready) }
                }
            }
        }
    }
}

private fun readFile(file: File, onDone: (PickedImage?) -> Unit) {
    val reader = FileReader()
    reader.onload = {
        val bytes = (reader.result as? ArrayBuffer)?.toByteArray()
        if (bytes == null || bytes.isEmpty()) {
            onDone(null)
        } else {
            val name = file.name.ifBlank { "image.jpg" }
            onDone(
                PickedImage(
                    fileName = name,
                    bytes = bytes,
                    mimeType = mimeTypeForPickedFile(name, file.type),
                )
            )
        }
    }
    reader.onerror = { onDone(null) }
    reader.readAsArrayBuffer(file)
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { index -> view[index] }
}
