package com.rotd.kejasafe.data

import com.rotd.kejasafe.data.room.RoomListingLocalStore

actual fun createListingLocalStore(): ListingLocalStore = RoomListingLocalStore()
