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
    val longitude: Double
)

@Serializable
data class PlaceSearchResponse(
    val suggestions: List<LandmarkSuggestion> = emptyList(),
    /** False when the server has no Google Places key and the device should search locally. */
    val configured: Boolean = true
)
