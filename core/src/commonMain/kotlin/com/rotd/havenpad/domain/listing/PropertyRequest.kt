package com.rotd.havenpad.domain.listing

import kotlinx.serialization.Serializable

@Serializable
enum class HouseSize(val displayName: String) {
    BEDSITTER("Bedsitter"),
    SINGLE_ROOM("Single Room"),
    ONE_BEDROOM("1 Bedroom"),
    TWO_BEDROOM("2 Bedroom"),
    THREE_BEDROOM("3 Bedroom"),
    FOUR_PLUS_BEDROOM("4+ Bedroom")
}

@Serializable
enum class HouseType(val displayName: String) {
    APARTMENT("Apartment"),
    BUNGALOW("Bungalow"),
    MANSIONETTE("Mansionette"),
    STUDIO("Studio"),
    TOWNHOUSE("Townhouse")
}

@Serializable
enum class ListingType(val displayName: String) {
    RENT("For Rent"),
    SALE("For Sale"),
    BNB("BnB")
}

@Serializable
enum class DistancePreference(val displayName: String, val km: Int) {
    AROUND_THERE("Just around there", 2),
    SHORT_MATATU_RIDE("A short matatu ride", 5),
    WIDER_AREA("Anywhere in the wider area", 15),
    TRAVEL_FOR_HOUSE("I can travel for the right house", 50)
}

@Serializable
data class PropertyRequest(
    val id: String = "",
    val userId: String,
    val size: HouseSize,
    val houseType: HouseType? = null,
    val listingType: ListingType,
    val area: String,
    val distance: DistancePreference,
    val budgetFrom: Long,
    val budgetTo: Long,
    val additionalInfo: String? = null,
    val agentsCanCall: Boolean,
    val createdAt: Long = 0L,
    val paymentStatus: String = "PENDING"
)

@Serializable
data class InitiatePropertyRequestPayment(
    val phoneNumber: String,
    val request: PropertyRequest
)
