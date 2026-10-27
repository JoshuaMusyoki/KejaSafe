package com.rotd.havenpad

import com.rotd.havenpad.utils.calculateDistance
import kotlin.math.roundToInt

data class CampusHub(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Curated campuses for Nairobi-area student housing. Used as selectable
 * suggestions on create-listing and related forms.
 */
object KenyaCampuses {
    val all: List<CampusHub> = listOf(
        CampusHub("University of Nairobi — Main Campus", -1.2797, 36.8163),
        CampusHub("University of Nairobi — Chiromo", -1.2736, 36.8075),
        CampusHub("University of Nairobi — Kikuyu", -1.2667, 36.6667),
        CampusHub("Technical University of Kenya", -1.2917, 36.8225),
        CampusHub("Kenyatta University", -1.1775, 36.9360),
        CampusHub("Jomo Kenyatta University of Agriculture and Technology", -1.0915, 37.0140),
        CampusHub("Strathmore University", -1.3094, 36.8125),
        CampusHub("United States International University — Africa", -1.2185, 36.8790),
        CampusHub("Multimedia University of Kenya", -1.3760, 36.7660),
        CampusHub("Cooperative University of Kenya", -1.3660, 36.7700),
        CampusHub("Catholic University of Eastern Africa", -1.3100, 36.7050),
        CampusHub("KCA University", -1.2490, 36.8780),
        CampusHub("Riara University", -1.3005, 36.7655),
        CampusHub("Zetech University — Ruiru", -1.1480, 36.9600),
        CampusHub("Daystar University — Nairobi", -1.2990, 36.7820),
        CampusHub("Africa Nazarene University", -1.4500, 36.9500),
        CampusHub("Mount Kenya University — Nairobi", -1.2833, 36.8167),
        CampusHub("Kenya Methodist University — Nairobi", -1.2860, 36.8250),
        CampusHub("Tangaza University", -1.3125, 36.7055),
        CampusHub("St Paul's University — Limuru", -1.1130, 36.6430),
        CampusHub("Egerton University — Nairobi Campus", -1.2921, 36.8219),
        CampusHub("Moi University — Nairobi Campus", -1.2864, 36.8172),
        CampusHub("Chuka University", -0.3194, 37.6572),
        CampusHub("Meru University of Science and Technology", 0.0910, 37.6660),
        CampusHub("Dedan Kimathi University of Technology", -0.3960, 36.9620),
        CampusHub("Karatina University", -0.4833, 37.1333),
        CampusHub("Embu University", -0.5020, 37.4580),
    )

    fun suggestionsNear(
        latitude: Double?,
        longitude: Double?,
        limit: Int = 8,
        query: String = "",
    ): List<CampusHub> {
        val needle = query.trim().lowercase()
        val base = if (needle.isBlank()) {
            all
        } else {
            all.filter { it.name.lowercase().contains(needle) }
        }
        if (latitude == null || longitude == null) {
            return base.take(limit)
        }
        return base
            .map { campus ->
                campus to calculateDistance(latitude, longitude, campus.latitude, campus.longitude)
            }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    fun nearestName(latitude: Double, longitude: Double): String? =
        suggestionsNear(latitude, longitude, limit = 1).firstOrNull()?.name

    fun distanceLabelKm(
        fromLat: Double,
        fromLng: Double,
        campus: CampusHub,
    ): String {
        val km = calculateDistance(fromLat, fromLng, campus.latitude, campus.longitude)
        return if (km < 1.0) {
            "${(km * 1000).roundToInt()} m away"
        } else {
            val tenths = (km * 10.0).roundToInt()
            "${tenths / 10}.${tenths % 10} km away"
        }
    }
}
