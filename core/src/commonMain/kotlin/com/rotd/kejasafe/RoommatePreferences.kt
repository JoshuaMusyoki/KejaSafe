package com.rotd.kejasafe

/**
 * Shared encode/decode for free-text roommate preference tags.
 * Accepts `,` `;` `|` and newlines as separators.
 */
object RoommatePreferences {
    private val separators = Regex("[,;|\\n\\r]+")

    fun normalize(raw: String?): List<String> =
        raw.orEmpty()
            .split(separators)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun normalize(tags: List<String>): List<String> =
        tags.map { it.trim() }.filter { it.isNotEmpty() }

    /** Lowercased set for overlap scoring. */
    fun normalizeSet(tags: List<String>): Set<String> =
        normalize(tags).map { it.lowercase() }.toSet()

    fun encode(tags: List<String>): String? =
        normalize(tags).takeIf { it.isNotEmpty() }?.joinToString(",")

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 0.5
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble().coerceAtLeast(1.0)
        return intersection / union
    }
}
