package com.rotd.havenpad.auth

import com.rotd.havenpad.LoginResponse
import com.rotd.havenpad.SignInPrompt
import com.rotd.havenpad.SignedInDevice
import com.rotd.havenpad.User
import com.rotd.havenpad.admin.AccountEventLog
import com.rotd.havenpad.db.SignInChallengesTable
import com.rotd.havenpad.db.UserDevicesTable
import com.rotd.havenpad.push.PushService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object DeviceSignInService {
    private const val PENDING = "PENDING"
    private const val APPROVED = "APPROVED"
    private const val DENIED = "DENIED"
    private const val CONSUMED = "CONSUMED"
    private const val WINDOW_MS = 3 * 60 * 1000L

    fun begin(user: User, deviceId: String, deviceName: String, platform: String): LoginResponse {
        val id = deviceId.trim()
        val name = deviceName.trim().ifBlank { "Unknown device" }.take(120)
        val plat = platform.trim().ifBlank { "unknown" }.take(20)
        if (id.isBlank()) {
            return LoginResponse(token = AuthService.generateToken(user), user = user)
        }
        val outcome = transaction {
            val known = UserDevicesTable.selectAll().where { UserDevicesTable.id eq id }.singleOrNull()
            val alreadyTrusted = known != null &&
                known[UserDevicesTable.userId] == user.id &&
                known[UserDevicesTable.trusted]
            val otherTrusted = UserDevicesTable.selectAll()
                .where { (UserDevicesTable.userId eq user.id) and (UserDevicesTable.trusted eq true) }
                .count()
            if (alreadyTrusted || otherTrusted == 0L) {
                val created = upsertTrusted(id, user.id, name, plat)
                if (created) AccountEventLog.record(user.id, "Signed in on a device", name)
                Outcome.Ready
            } else {
                val challengeId = UUID.randomUUID().toString()
                SignInChallengesTable.insert {
                    it[SignInChallengesTable.id] = challengeId
                    it[SignInChallengesTable.userId] = user.id
                    it[SignInChallengesTable.deviceId] = id
                    it[SignInChallengesTable.deviceName] = name
                    it[SignInChallengesTable.platform] = plat
                    it[status] = PENDING
                    it[createdAt] = System.currentTimeMillis()
                }
                Outcome.Wait(challengeId, name)
            }
        }
        return when (outcome) {
            Outcome.Ready -> LoginResponse(token = AuthService.generateToken(user), user = user)
            is Outcome.Wait -> {
                AccountEventLog.record(user.id, "Sign-in needs approval", outcome.deviceName)
                PushService.notifySignIn(user.id, outcome.deviceName, outcome.challengeId)
                LoginResponse(pending = true, challengeId = outcome.challengeId, deviceName = outcome.deviceName)
            }
        }
    }

    fun remember(userId: String, deviceId: String, deviceName: String, platform: String) {
        val id = deviceId.trim()
        if (userId.isBlank() || id.isBlank()) return
        val name = deviceName.trim().ifBlank { "This device" }.take(120)
        transaction {
            val created = upsertTrusted(id, userId, name, platform.trim().ifBlank { "unknown" }.take(20))
            if (created) AccountEventLog.record(userId, "Signed in on a device", name)
        }
    }

    fun list(userId: String, currentDeviceId: String): List<SignedInDevice> = transaction {
        UserDevicesTable.selectAll()
            .where { (UserDevicesTable.userId eq userId) and (UserDevicesTable.trusted eq true) }
            .orderBy(UserDevicesTable.lastSeenAt to SortOrder.DESC)
            .map { row ->
                SignedInDevice(
                    id = row[UserDevicesTable.id],
                    name = row[UserDevicesTable.name],
                    platform = row[UserDevicesTable.platform],
                    lastSeenAt = row[UserDevicesTable.lastSeenAt],
                    thisDevice = row[UserDevicesTable.id] == currentDeviceId,
                )
            }
    }

    fun names(userId: String): List<String> = transaction {
        UserDevicesTable.selectAll()
            .where { (UserDevicesTable.userId eq userId) and (UserDevicesTable.trusted eq true) }
            .orderBy(UserDevicesTable.lastSeenAt to SortOrder.DESC)
            .map { it[UserDevicesTable.name] }
    }

    fun incoming(userId: String): SignInPrompt? = transaction {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        SignInChallengesTable.selectAll()
            .where { (SignInChallengesTable.userId eq userId) and (SignInChallengesTable.status eq PENDING) }
            .orderBy(SignInChallengesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.takeIf { it[SignInChallengesTable.createdAt] >= cutoff }
            ?.let { row ->
                SignInPrompt(
                    challengeId = row[SignInChallengesTable.id],
                    deviceName = row[SignInChallengesTable.deviceName],
                    createdAt = row[SignInChallengesTable.createdAt],
                )
            }
    }

    fun approve(userId: String, challengeId: String) {
        val name = transaction {
            val row = openChallenge(userId, challengeId) ?: error("That sign-in request has expired.")
            SignInChallengesTable.update({ SignInChallengesTable.id eq challengeId }) {
                it[status] = APPROVED
            }
            upsertTrusted(
                row[SignInChallengesTable.deviceId],
                userId,
                row[SignInChallengesTable.deviceName],
                row[SignInChallengesTable.platform],
            )
            row[SignInChallengesTable.deviceName]
        }
        AccountEventLog.record(userId, "Allowed a device", name)
    }

    fun deny(userId: String, challengeId: String) {
        val name = transaction {
            val row = openChallenge(userId, challengeId) ?: error("That sign-in request has expired.")
            SignInChallengesTable.update({ SignInChallengesTable.id eq challengeId }) {
                it[status] = DENIED
            }
            row[SignInChallengesTable.deviceName]
        }
        AccountEventLog.record(userId, "Blocked a device", name)
    }

    fun poll(challengeId: String): LoginResponse {
        val id = challengeId.trim()
        if (id.isBlank()) return LoginResponse(denied = true)
        return transaction {
            val row = SignInChallengesTable.selectAll()
                .where { SignInChallengesTable.id eq id }
                .singleOrNull()
                ?: return@transaction LoginResponse(denied = true)
            val name = row[SignInChallengesTable.deviceName]
            val created = row[SignInChallengesTable.createdAt]
            val expired = System.currentTimeMillis() - created > WINDOW_MS
            when (row[SignInChallengesTable.status]) {
                PENDING -> if (expired) {
                    SignInChallengesTable.update({ SignInChallengesTable.id eq id }) { it[status] = DENIED }
                    LoginResponse(denied = true, deviceName = name)
                } else {
                    LoginResponse(pending = true, challengeId = id, deviceName = name)
                }
                APPROVED -> {
                    val user = AuthService.getUserById(row[SignInChallengesTable.userId])
                        ?: return@transaction LoginResponse(denied = true, deviceName = name)
                    SignInChallengesTable.update({ SignInChallengesTable.id eq id }) { it[status] = CONSUMED }
                    LoginResponse(token = AuthService.generateToken(user), user = user, deviceName = name)
                }
                else -> LoginResponse(denied = true, deviceName = name)
            }
        }
    }

    private fun openChallenge(userId: String, challengeId: String) =
        SignInChallengesTable.selectAll()
            .where {
                (SignInChallengesTable.id eq challengeId) and
                    (SignInChallengesTable.userId eq userId) and
                    (SignInChallengesTable.status eq PENDING)
            }
            .singleOrNull()
            ?.takeIf { System.currentTimeMillis() - it[SignInChallengesTable.createdAt] <= WINDOW_MS }

    private fun upsertTrusted(deviceId: String, userId: String, name: String, platform: String): Boolean {
        val now = System.currentTimeMillis()
        val existing = UserDevicesTable.selectAll().where { UserDevicesTable.id eq deviceId }.singleOrNull()
        return if (existing == null) {
            UserDevicesTable.insert {
                it[id] = deviceId
                it[UserDevicesTable.userId] = userId
                it[UserDevicesTable.name] = name
                it[UserDevicesTable.platform] = platform
                it[trusted] = true
                it[lastSeenAt] = now
                it[createdAt] = now
            }
            true
        } else {
            val created = existing[UserDevicesTable.userId] != userId || !existing[UserDevicesTable.trusted]
            UserDevicesTable.update({ UserDevicesTable.id eq deviceId }) {
                it[UserDevicesTable.userId] = userId
                it[UserDevicesTable.name] = name
                it[UserDevicesTable.platform] = platform
                it[trusted] = true
                it[lastSeenAt] = now
            }
            created
        }
    }

    private sealed interface Outcome {
        data object Ready : Outcome
        data class Wait(val challengeId: String, val deviceName: String) : Outcome
    }
}
