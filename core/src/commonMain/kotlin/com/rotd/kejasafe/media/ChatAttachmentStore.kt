package com.rotd.kejasafe.media

/**
 * Local file store for chat image/PDF payloads.
 * Message JSON only keeps [attachmentId]; bytes live here.
 */
interface ChatAttachmentStore {
    fun save(id: String, bytes: ByteArray)
    fun load(id: String): ByteArray?
    fun delete(id: String)
}

expect fun createChatAttachmentStore(): ChatAttachmentStore

class MemoryChatAttachmentStore : ChatAttachmentStore {
    private val data = mutableMapOf<String, ByteArray>()

    override fun save(id: String, bytes: ByteArray) {
        data[id] = bytes
    }

    override fun load(id: String): ByteArray? = data[id]

    override fun delete(id: String) {
        data.remove(id)
    }
}

const val MAX_CHAT_ATTACHMENT_MB = 30
const val MAX_CHAT_ATTACHMENT_BYTES = MAX_CHAT_ATTACHMENT_MB * 1024 * 1024

fun ByteArray.looksLikePdf(): Boolean {
    if (size < 5) return false
    val limit = minOf(size, 1024) - 4
    if (limit < 0) return false
    for (i in 0..limit) {
        if (this[i] == '%'.code.toByte() &&
            this[i + 1] == 'P'.code.toByte() &&
            this[i + 2] == 'D'.code.toByte() &&
            this[i + 3] == 'F'.code.toByte()
        ) {
            return true
        }
    }
    return false
}
