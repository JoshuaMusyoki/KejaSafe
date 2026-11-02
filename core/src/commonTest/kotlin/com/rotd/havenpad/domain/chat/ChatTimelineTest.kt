package com.rotd.havenpad.domain.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatTimelineTest {
    @Test
    fun groupsConsecutiveMessagesOnSameDay() {
        val day = 17_000L * 86_400_000L
        val messages = listOf(
            sample("1", day + 1_000),
            sample("2", day + 2_000),
            sample("3", day + 86_400_000L + 500),
        )
        val timeline = buildChatTimeline(messages, nowMillis = day + 86_400_000L + 10_000)

        assertEquals(5, timeline.size)
        val first = timeline[0] as ChatTimelineItem.DayHeader
        assertEquals("Yesterday", first.label)
        assertEquals("1", (timeline[1] as ChatTimelineItem.Bubble).message.id)
        assertEquals("2", (timeline[2] as ChatTimelineItem.Bubble).message.id)
        val second = timeline[3] as ChatTimelineItem.DayHeader
        assertEquals("Today", second.label)
        assertEquals("3", (timeline[4] as ChatTimelineItem.Bubble).message.id)
    }

    @Test
    fun sentCanUpgradeToDeliveredAndReadButNotDowngrade() {
        assertTrue(MessageStatus.SENT.canUpgradeTo(MessageStatus.DELIVERED))
        assertTrue(MessageStatus.DELIVERED.canUpgradeTo(MessageStatus.READ))
        assertFalse(MessageStatus.READ.canUpgradeTo(MessageStatus.DELIVERED))
        assertFalse(MessageStatus.FAILED.canUpgradeTo(MessageStatus.READ))
    }

    private fun sample(id: String, timestamp: Long) = ApartmentChatMessage(
        id = id,
        listingId = "l",
        bookingId = "b",
        senderId = "a",
        receiverId = "c",
        content = id,
        timestamp = timestamp,
        status = MessageStatus.SENT,
    )
}
