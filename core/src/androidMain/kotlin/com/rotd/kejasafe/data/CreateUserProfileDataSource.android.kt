package com.rotd.kejasafe.data

/**
 * Active profile store while Cloud SQL / Firebase Data Connect is paused.
 *
 * H2 + Ktor remain the server of record for listings/rent (`:server`).
 * Profiles for Firebase Auth users live in this local cache on device.
 *
 * To re-enable Cloud SQL later:
 * ```
 * // actual fun createUserProfileDataSource(): UserProfileDataSource =
 * //     SqlConnectUserProfileDataSource()
 * ```
 * and set `DataConnectRuntime.cloudEnabled = true` after deploying Data Connect.
 */
actual fun createUserProfileDataSource(): UserProfileDataSource =
    SessionCachedUserProfileDataSource()

// Cloud SQL / Data Connect (paused — requires paid Cloud SQL instance):
// actual fun createUserProfileDataSource(): UserProfileDataSource =
//     SqlConnectUserProfileDataSource()
