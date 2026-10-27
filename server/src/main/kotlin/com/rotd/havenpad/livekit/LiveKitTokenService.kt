package com.rotd.havenpad.livekit

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.util.Date

/**
 * Mints LiveKit access tokens from env:
 * - LIVEKIT_API_KEY
 * - LIVEKIT_API_SECRET
 * - LIVEKIT_URL (wss://… or https://…)
 */
object LiveKitTokenService {

    data class Config(
        val apiKey: String,
        val apiSecret: String,
        val serverUrl: String,
    )

    fun configOrNull(): Config? {
        val key = System.getenv("LIVEKIT_API_KEY")?.trim().orEmpty()
        val secret = System.getenv("LIVEKIT_API_SECRET")?.trim().orEmpty()
        val url = System.getenv("LIVEKIT_URL")?.trim().orEmpty()
        if (key.isBlank() || secret.isBlank() || url.isBlank()) return null
        return Config(apiKey = key, apiSecret = secret, serverUrl = url)
    }

    fun requireConfig(): Config =
        configOrNull()
            ?: error(
                "LiveKit is not configured. Set LIVEKIT_API_KEY, LIVEKIT_API_SECRET, and LIVEKIT_URL."
            )

    fun createToken(
        identity: String,
        displayName: String,
        roomName: String,
        ttlSeconds: Long = 3600,
        canPublish: Boolean = true,
    ): String {
        val cfg = requireConfig()
        val now = Instant.now()
        val videoGrant = mapOf(
            "roomJoin" to true,
            "room" to roomName,
            "canPublish" to canPublish,
            "canSubscribe" to true,
            "canPublishData" to true,
        )
        return JWT.create()
            .withIssuer(cfg.apiKey)
            .withSubject(identity)
            .withClaim("name", displayName)
            .withClaim("video", videoGrant)
            .withNotBefore(Date.from(now.minusSeconds(10)))
            .withExpiresAt(Date.from(now.plusSeconds(ttlSeconds)))
            .sign(Algorithm.HMAC256(cfg.apiSecret))
    }

    fun serverUrl(): String = requireConfig().serverUrl
}
