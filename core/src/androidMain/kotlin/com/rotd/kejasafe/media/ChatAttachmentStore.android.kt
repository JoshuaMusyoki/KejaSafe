package com.rotd.kejasafe.media

import com.rotd.kejasafe.storage.AndroidAppContext
import java.io.File

actual fun createChatAttachmentStore(): ChatAttachmentStore = AndroidChatAttachmentStore()

class AndroidChatAttachmentStore : ChatAttachmentStore {
    private val dir: File by lazy {
        File(AndroidAppContext.require().filesDir, "chat_attachments").apply { mkdirs() }
    }

    override fun save(id: String, bytes: ByteArray) {
        File(dir, safeName(id)).writeBytes(bytes)
    }

    override fun load(id: String): ByteArray? {
        val file = File(dir, safeName(id))
        return if (file.exists()) file.readBytes() else null
    }

    override fun delete(id: String) {
        File(dir, safeName(id)).delete()
    }

    private fun safeName(id: String): String = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
