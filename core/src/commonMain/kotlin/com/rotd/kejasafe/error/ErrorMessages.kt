package com.rotd.kejasafe.error

/**
 * Central user-facing copy for network / server failures.
 * Keep strings short and actionable — ViewModels should prefer these over raw exceptions.
 */
object ErrorMessages {
    const val NO_INTERNET =
        "No internet connection. Check your network and try again."
    const val CONNECTION_TIMEOUT =
        "The request timed out. Check your connection and try again."
    const val SERVER_UNAVAILABLE =
        "We can’t reach KejaSafe right now. Please try again shortly."
    const val SERVER_ERROR =
        "Something went wrong on our side. Please try again."
    const val UNAUTHORIZED =
        "Your session expired. Please sign in again."
    const val FORBIDDEN =
        "You don’t have permission to do that."
    const val NOT_FOUND =
        "We couldn’t find what you were looking for."
    const val CONFLICT =
        "This action conflicts with the current data. Refresh and try again."
    const val VALIDATION =
        "Some details look invalid. Check your input and try again."
    const val NOT_SIGNED_IN =
        "You’re not signed in. Please log in to continue."
    const val UNKNOWN =
        "Something went wrong. Please try again."
    const val OFFLINE_CACHED =
        "You’re offline. Showing saved data — pull to refresh when you’re back online."
    const val OFFLINE_NO_CACHE =
        "You’re offline and there’s no saved data for this screen yet."
    const val SYNC_FAILED_CACHED =
        "Couldn’t refresh. Showing the last saved data."
    const val SAVED_OFFLINE =
        "Saved on this device. Changes will sync when you’re back online."
    const val PHOTOS_NEED_CONNECTION =
        "New photos need a connection. Remove them to save the rest offline, or try again online."
}
