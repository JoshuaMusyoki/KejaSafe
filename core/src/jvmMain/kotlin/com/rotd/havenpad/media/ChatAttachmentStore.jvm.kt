package com.rotd.havenpad.media

import java.io.File

actual fun createChatAttachmentStore(): ChatAttachmentStore = JvmChatAttachmentStore()

class JvmChatAttachmentStore : ChatAttachmentStore {
    private val dir: File by lazy {
        File(System.getProperty("java.io.tmpdir"), "havenpad_chat_attachments").apply { mkdirs() }
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
