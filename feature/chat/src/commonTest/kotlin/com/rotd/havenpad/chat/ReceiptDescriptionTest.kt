package com.rotd.havenpad.chat

import com.rotd.havenpad.chat.components.receiptDescription
import com.rotd.havenpad.domain.chat.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptDescriptionTest {
    @Test
    fun eachReceiptHasASpokenLabel() {
        assertEquals("Sending", receiptDescription(MessageStatus.SENDING))
        assertEquals("Sent", receiptDescription(MessageStatus.SENT))
        assertEquals("Delivered", receiptDescription(MessageStatus.DELIVERED))
        assertEquals("Read", receiptDescription(MessageStatus.READ))
        assertEquals("Failed", receiptDescription(MessageStatus.FAILED))
    }
}
