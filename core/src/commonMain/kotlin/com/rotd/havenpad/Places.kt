package com.rotd.havenpad

import kotlinx.serialization.Serializable

@Serializable
data class LandmarkSuggestion(
    val placeId: String,
    val name: String,
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class LandmarkDetails(
    val placeId: String,
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    /** Approximate meters from the building to the nearest classified road. */
    val distanceToRoadMeters: Int? = null,
    /** Name of that road when Mapbox knows it. */
    val roadName: String? = null,
)

@Serializable
data class PlaceSearchResponse(
    val suggestions: List<LandmarkSuggestion> = emptyList(),
    /** False when the server has no Google Places key and the device should search locally. */
    val configured: Boolean = true
)

@Serializable
data class CampusSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Meters from the listing pin when known. */
    val distanceMeters: Int? = null,
    val placeId: String? = null,
)

@Serializable
data class CampusSearchResponse(
    val campuses: List<CampusSuggestion> = emptyList(),
    val configured: Boolean = true,
)
