package com.rotd.kejasafe.listing

import com.rotd.kejasafe.PropertyListing
import com.rotd.kejasafe.PropertyType

/**
 * Text match used by tenant home chips and the house search screen.
 * Covers title, area, university, and house-type labels such as "1 Bedroom".
 */
fun PropertyListing.matchesHouseQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (title.contains(q, ignoreCase = true)) return true
    if (address.contains(q, ignoreCase = true)) return true
    if (university?.contains(q, ignoreCase = true) == true) return true
    val type = propertyType ?: return false
    if (type.displayLabel().contains(q, ignoreCase = true)) return true
    return type.searchAliases().any { alias ->
        alias.contains(q, ignoreCase = true) || q.contains(alias, ignoreCase = true)
    }
}

private fun PropertyType.searchAliases(): List<String> = when (this) {
    PropertyType.SINGLE_ROOM -> listOf("single", "single room")
    PropertyType.BEDSITTER -> listOf("bedsitter", "bed sitter", "studio")
    PropertyType.ONE_BEDROOM -> listOf("1 bedroom", "1br", "one bedroom")
    PropertyType.TWO_BEDROOM -> listOf("2 bedroom", "2br", "two bedroom")
    PropertyType.THREE_BEDROOM -> listOf("3 bedroom", "3br", "three bedroom")
    PropertyType.MAISONETTE -> listOf("maisonette")
}
