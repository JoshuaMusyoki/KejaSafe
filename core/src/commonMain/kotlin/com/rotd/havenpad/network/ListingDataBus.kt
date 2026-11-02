package com.rotd.havenpad.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight bus so home screens refresh when a listing is created/updated
 * without depending on navigation re-creating ViewModels.
 */
object ListingDataBus {
    private val _listingsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val listingsChanged: SharedFlow<Unit> = _listingsChanged.asSharedFlow()

    fun notifyListingsChanged() {
        _listingsChanged.tryEmit(Unit)
    }
}

object RentDataBus {
    private val _billsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val billsChanged: SharedFlow<Unit> = _billsChanged.asSharedFlow()

    fun notifyBillsChanged() {
        _billsChanged.tryEmit(Unit)
    }
}
