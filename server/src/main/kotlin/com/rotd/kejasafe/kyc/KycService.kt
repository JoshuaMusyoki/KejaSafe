package com.rotd.kejasafe.kyc

import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.KycSubmission
import com.rotd.kejasafe.db.KycTable
import com.rotd.kejasafe.db.UsersTable
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
