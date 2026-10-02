package com.rotd.havenpad.kyc

import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.KycSubmission
import com.rotd.havenpad.db.KycTable
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object KycService {
    fun submitKyc(submission: KycSubmission) = transaction {
        KycTable.insert {
            it[userId] = submission.userId
            it[documentType] = submission.documentType
            it[documentNumber] = submission.documentNumber
            it[documentFrontImageUrl] = submission.documentFrontImageUrl
            it[documentBackImageUrl] = submission.documentBackImageUrl
            it[selfieImageUrl] = submission.selfieImageUrl
            it[submittedAt] = System.currentTimeMillis()
        }

        UsersTable.update({ UsersTable.id eq submission.userId }) {
            it[kycStatus] = KycStatus.PENDING
        }
    }

    fun getKycStatus(userId: String): KycStatus = transaction {
        UsersTable.selectAll().where { UsersTable.id eq userId }
            .map { it.get(UsersTable.kycStatus) }
            .singleOrNull() ?: KycStatus.NOT_SUBMITTED
    }
}
