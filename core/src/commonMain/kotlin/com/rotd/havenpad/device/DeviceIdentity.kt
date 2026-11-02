package com.rotd.havenpad.device

import com.rotd.havenpad.storage.LocalSettings
import kotlin.random.Random

data class DeviceStamp(
    val id: String,
    val name: String,
    val platform: String,
)

object DeviceIdentity {
    private const val KEY = "havenpad_device_id"

    fun current(): DeviceStamp {
        val existing = LocalSettings.getString(KEY)?.trim().orEmpty()
        val id = existing.ifBlank {
            val created = buildString(32) {
                repeat(32) { append(Random.nextInt(16).toString(16)) }
            }
            LocalSettings.putString(KEY, created)
            created
        }
        return DeviceStamp(id = id, name = platformDeviceName(), platform = platformLabel())
    }
}

internal expect fun platformDeviceName(): String

internal expect fun platformLabel(): String
