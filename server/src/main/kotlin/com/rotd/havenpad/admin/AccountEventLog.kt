package com.rotd.havenpad.admin

import com.rotd.havenpad.db.AccountEventsTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object AccountEventLog {
    fun record(userId: String, title: String, detail: String) {
        if (userId.isBlank()) return
        transaction {
            AccountEventsTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[AccountEventsTable.userId] = userId
                it[AccountEventsTable.title] = title.take(80)
                it[AccountEventsTable.detail] = detail.take(200)
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }
}
