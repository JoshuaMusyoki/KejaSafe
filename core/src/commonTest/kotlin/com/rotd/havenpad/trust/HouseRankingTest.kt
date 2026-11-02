package com.rotd.havenpad.trust

import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.reviewRankLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HouseRankingTest {
    @Test
    fun aReviewOnOneHouseDoesNotScoreTheLandlordsOtherHouse() {
        val scores = rankByAverageRating(
            mapOf(
                "joymat" to listOf(4),
                "jaymoh" to emptyList(),
            ),
        )

        assertEquals(HouseScore(percent = 80, count = 1, rank = 1), scores["joymat"])
        assertNull(scores["jaymoh"])
    }

    @Test
    fun higherPercentRanksFirstAndTiesBreakOnMoreReviews() {
        val scores = rankByAverageRating(
            mapOf(
                "quiet" to listOf(5),
                "busy" to listOf(5, 5),
                "plain" to listOf(3),
            ),
        )

        assertEquals(1, scores.getValue("busy").rank)
        assertEquals(2, scores.getValue("quiet").rank)
        assertEquals(3, scores.getValue("plain").rank)
        assertEquals(100, scores.getValue("busy").percent)
    }

    @Test
    fun labelHidesRankUntilTheHouseHasAReview() {
        val unreviewed = PropertyListing(id = "jaymoh", title = "Jaymoh", address = "Nairobi")
        val reviewed = unreviewed.copy(id = "joymat", reviewPercent = 80, reviewCount = 1, landlordRank = 1)

        assertEquals("No reviews yet", unreviewed.reviewRankLabel())
        assertEquals("80% · Rank #1 · 1 review", reviewed.reviewRankLabel())
    }
}
