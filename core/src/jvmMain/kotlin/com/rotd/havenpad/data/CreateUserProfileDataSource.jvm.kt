package com.rotd.havenpad.data

actual fun createUserProfileDataSource(): UserProfileDataSource =
    SessionCachedUserProfileDataSource()
