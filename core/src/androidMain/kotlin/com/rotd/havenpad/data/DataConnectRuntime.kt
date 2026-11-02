package com.rotd.havenpad.data

import com.rotd.havenpad.dataconnect.KejasafeConnector
import com.rotd.havenpad.dataconnect.instance

/**
 * Shared access to [KejasafeConnector] with optional Data Connect emulator binding.
 *
 * Kept outside the generated `dataconnect` package so `firebase dataconnect:sdk:generate`
 * does not delete it.
 *
 * [cloudEnabled] defaults to `false` until the havenpad Data Connect service is deployed.
 */
object DataConnectRuntime {
    /** When `true`, bind to the local Data Connect emulator on first [connector] access. */
    @JvmField
    var useEmulator: Boolean = false

    /**
     * When `false`, profile code uses the local cache only (no SQL Connect RPCs).
     * Enable after `firebase deploy --only dataconnect`.
     */
    @JvmField
    @Volatile
    var cloudEnabled: Boolean = false

    @Volatile
    private var configured = false

    fun connector(): KejasafeConnector {
        val c = KejasafeConnector.instance
        if (!configured) {
            synchronized(this) {
                if (!configured) {
                    if (useEmulator) {
                        runCatching { c.dataConnect.useEmulator() }
                    }
                    configured = true
                }
            }
        }
        return c
    }
}
