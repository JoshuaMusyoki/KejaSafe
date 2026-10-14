package com.rotd.havenpad.trust

import kotlin.math.roundToInt

/** One owner's score. Rank 1 is the best average. Owners with no ratings are omitted. */
data class HouseScore(
    val percent: Int,
    val count: Int,
    val rank: Int,
)

/**
 * Rank each owner by the average of their own ratings.
 * A landlord's other houses do not inherit a score from a house that was reviewed.
 */
fun rankByAverageRating(ratingsByOwner: Map<String, List<Int>>): Map<String, HouseScore> {
    val ranked = ratingsByOwner.mapNotNull { (id, ratings) ->
        if (id.isBlank() || ratings.isEmpty()) return@mapNotNull null
        val percent = ((ratings.average() / 5.0) * 100.0).roundToInt().coerceIn(0, 100)
        id to (percent to ratings.size)
    }.sortedWith(
        compareByDescending<Pair<String, Pair<Int, Int>>> { it.second.first }
            .thenByDescending { it.second.second },
    )
    return ranked.mapIndexed { index, (id, stats) ->
        id to HouseScore(percent = stats.first, count = stats.second, rank = index + 1)
    }.toMap()
}
