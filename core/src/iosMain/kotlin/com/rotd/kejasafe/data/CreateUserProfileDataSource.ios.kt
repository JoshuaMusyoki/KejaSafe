package com.rotd.kejasafe.data

actual fun createUserProfileDataSource(): UserProfileDataSource =
    SessionCachedUserProfileDataSource()
