package com.rotd.havenpad.profile

import com.rotd.havenpad.Reputation
import com.rotd.havenpad.ReviewSubject
import kotlin.test.Test
import kotlin.test.assertEquals

class ReputationHeadlineTest {
    @Test
    fun headlineWaitsThenCountsThenRanks() {
        assertEquals("Loading your rating…", reputationHeadline(null))
        val empty = Reputation(userId = "u", subject = ReviewSubject.LANDLORD)
        assertEquals("No reviews yet.", reputationHeadline(empty))
        val ranked = empty.copy(percent = 80, reviewCount = 1, rank = 1)
        assertEquals("80% · Rank #1 · 1 review", reputationHeadline(ranked))
    }
}
