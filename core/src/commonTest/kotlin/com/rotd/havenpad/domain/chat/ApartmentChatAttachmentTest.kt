package com.rotd.havenpad.domain.chat

import com.rotd.havenpad.media.MemoryChatAttachmentStore
import com.rotd.havenpad.media.looksLikePdf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApartmentChatAttachmentTest {
    @Test
    fun previewTextUsesFileNameForPdf() {
        val message = ApartmentChatMessage(
            id = "1",
            listingId = "l",
            bookingId = "b",
            senderId = "a",
            receiverId = "c",
            content = "PDF",
            timestamp = 1L,
            type = ApartmentChatMessage.TYPE_PDF,
            fileName = "lease.pdf",
        )
        assertEquals("lease.pdf", message.previewText())
        assertEquals(true, message.isPdf())
    }

    @Test
    fun previewTextForImage() {
        val message = ApartmentChatMessage(
            id = "1",
            listingId = "l",
            bookingId = "b",
            senderId = "a",
            receiverId = "c",
            content = "Photo",
            timestamp = 1L,
            type = ApartmentChatMessage.TYPE_IMAGE,
        )
        assertEquals("Image", message.previewText())
        assertEquals(true, message.isImage())
    }

    @Test
    fun previewTextIgnoresDigitImageNames() {
        val message = ApartmentChatMessage(
            id = "1",
            listingId = "l",
            bookingId = "b",
            senderId = "a",
            receiverId = "c",
            content = "Image",
            timestamp = 1L,
            type = ApartmentChatMessage.TYPE_IMAGE,
            fileName = "57",
        )
        assertEquals("Image", message.previewText())
    }

    @Test
    fun previewTextKeepsRealImageFileName() {
        val message = ApartmentChatMessage(
            id = "1",
            listingId = "l",
            bookingId = "b",
            senderId = "a",
            receiverId = "c",
            content = "Image",
            timestamp = 1L,
            type = ApartmentChatMessage.TYPE_IMAGE,
            fileName = "balcony.jpg",
        )
        assertEquals("balcony.jpg", message.previewText())
    }

    @Test
    fun memoryStoreRoundTrip() {
        val store = MemoryChatAttachmentStore()
        store.save("att1", byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), store.load("att1"))
        store.delete("att1")
        assertNull(store.load("att1"))
    }

    @Test
    fun looksLikePdfDetectsHeader() {
        val pdf = "%PDF-1.4\n%âãÏÓ\n".encodeToByteArray()
        assertTrue(pdf.looksLikePdf())
        assertFalse("not a pdf".encodeToByteArray().looksLikePdf())
        assertFalse(byteArrayOf().looksLikePdf())
    }
}
