package com.rotd.havenpad.data

import com.rotd.havenpad.data.room.RoomListingLocalStore

actual fun createListingLocalStore(): ListingLocalStore = RoomListingLocalStore()
