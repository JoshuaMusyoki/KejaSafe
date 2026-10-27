package com.rotd.havenpad.trust

/** A neighborhood name is only trustworthy inside this radius. */
const val PRECISE_PLACE_METERS = 200f

/**
 * Tight GPS may show the neighborhood. A coarser fix shows the city only,
 * and a fix with no city name shows nothing so the UI can say "near you".
 */
fun browsePlaceName(neighborhood: String?, city: String?, accuracyMeters: Float?): String? {
    val area = neighborhood?.trim()?.takeIf { it.isNotEmpty() }
    val town = city?.trim()?.takeIf { it.isNotEmpty() }
    val precise = accuracyMeters != null && accuracyMeters > 0f && accuracyMeters <= PRECISE_PLACE_METERS
    val label = if (precise) {
        listOfNotNull(area, town).distinct().joinToString(", ")
    } else {
        town.orEmpty()
    }
    return label.ifBlank { null }
}
