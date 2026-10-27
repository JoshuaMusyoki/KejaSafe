package com.rotd.havenpad.media

actual fun createChatAttachmentStore(): ChatAttachmentStore = MemoryChatAttachmentStore()
