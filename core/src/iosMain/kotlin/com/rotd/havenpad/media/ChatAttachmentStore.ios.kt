package com.rotd.havenpad.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual fun createChatAttachmentStore(): ChatAttachmentStore = IosChatAttachmentStore()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosChatAttachmentStore : ChatAttachmentStore {
    private val dir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        )
        val base = paths.firstOrNull() as? String ?: "."
        val path = "$base/chat_attachments"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        path
    }

    override fun save(id: String, bytes: ByteArray) {
        bytes.toNsData().writeToFile("$dir/${safeName(id)}", atomically = true)
    }

    override fun load(id: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile("$dir/${safeName(id)}") ?: return null
        return data.toByteArray()
    }

    override fun delete(id: String) {
        NSFileManager.defaultManager.removeItemAtPath("$dir/${safeName(id)}", error = null)
    }

    private fun safeName(id: String): String = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNsData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val count = length.toInt()
    if (count == 0) return ByteArray(0)
    val out = ByteArray(count)
    val src = bytes
    if (src != null) {
        out.usePinned { dest ->
            memcpy(dest.addressOf(0), src, length)
        }
    }
    return out
}
