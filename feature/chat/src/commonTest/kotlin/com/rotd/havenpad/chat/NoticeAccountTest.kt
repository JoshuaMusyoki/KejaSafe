package com.rotd.havenpad.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoticeAccountTest {
    @Test
    fun inboxShowsOnlyTheSignedInAccount() {
        val notices = listOf(
            notice("rent", "student-1"),
            notice("hello", "landlord-2"),
            notice("old", ""),
        )
        assertEquals(listOf("rent"), noticesForAccount(notices, "student-1").map { it.id })
        assertEquals(listOf("hello"), noticesForAccount(notices, "landlord-2").map { it.id })
        assertTrue(noticesForAccount(notices, "").isEmpty())
        assertTrue(noticesForAccount(notices, "someone-else").isEmpty())
    }

    private fun notice(id: String, ownerUserId: String) = ChatNotice(
        id = id,
        title = id,
        body = id,
        timestamp = 0L,
        peerUserId = "",
        listingId = "",
        listingTitle = "",
        bookingId = "",
        displayName = "",
        peerIsLandlord = false,
        ownerUserId = ownerUserId,
    )
}
