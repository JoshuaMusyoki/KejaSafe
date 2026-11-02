package com.rotd.havenpad.listing

/** Home heading. A place name is only passed in after [com.rotd.havenpad.trust.browsePlaceName] accepts it. */
fun browseHeading(
    isLocating: Boolean,
    locationName: String?,
    hasCoordinates: Boolean,
): String = when {
    isLocating -> "Finding houses near you"
    !locationName.isNullOrBlank() -> "Houses around $locationName"
    hasCoordinates -> "Houses near you"
    else -> "Trending near you"
}
