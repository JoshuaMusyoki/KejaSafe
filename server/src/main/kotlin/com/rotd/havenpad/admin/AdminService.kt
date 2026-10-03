package com.rotd.havenpad.admin

import com.rotd.havenpad.AdminActivity
import com.rotd.havenpad.AdminUserDetail
import com.rotd.havenpad.AdminUserSummary
import com.rotd.havenpad.CreateLandlordRequest
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.auth.AuthService
import com.rotd.havenpad.auth.DeviceSignInService
import com.rotd.havenpad.db.AccountEventsTable
import com.rotd.havenpad.db.DeviceTokensTable
import com.rotd.havenpad.db.KycTable
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.UsersTable
import com.rotd.havenpad.kyc.KycDocumentCheck
import com.rotd.havenpad.kyc.KycService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object AdminService {
    fun list(role: UserRole): List<AdminUserSummary> = transaction {
        require(role == UserRole.LANDLORD || role == UserRole.STUDENT) {
            "Choose landlords or students."
        }
        UsersTable.selectAll()
            .where { (UsersTable.role eq role) and (UsersTable.active eq true) }
            .map { row ->
                AdminUserSummary(
                    id = row[UsersTable.id],
                    fullName = row[UsersTable.fullName],
                    username = row[UsersTable.username],
                    email = row[UsersTable.email],
                    role = row[UsersTable.role],
                    kycStatus = row[UsersTable.kycStatus],
                    active = row[UsersTable.active],
                    createdAt = row[UsersTable.createdAt],
                    phone = row[UsersTable.phone],
                    businessName = row[UsersTable.businessName],
                )
            }
            .sortedBy { it.fullName.lowercase() }
    }

    fun detail(userId: String): AdminUserDetail {
        val built = detailRow(userId)
        val idBytes = KycService.readFile(userId, "id")?.first
        val proofBytes = KycService.readFile(userId, "address")?.first
        val check = if (built.hasIdCopy || built.hasAddressProof) {
            KycDocumentCheck.assess(
                fullName = built.user.fullName,
                role = built.user.role,
                documentType = built.documentType,
                documentNumber = built.documentNumber,
                address = built.residentialAddress,
                idBytes = idBytes,
                proofBytes = proofBytes,
            )
        } else {
            null
        }
        return built.copy(documentCheck = check)
    }

    private fun detailRow(userId: String): AdminUserDetail = transaction {
        val row = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?: error("Account was not found.")
        val kyc = KycTable.selectAll()
            .where { KycTable.userId eq userId }
            .orderBy(KycTable.submittedAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
        val summary = AdminUserSummary(
            id = row[UsersTable.id],
            fullName = row[UsersTable.fullName],
            username = row[UsersTable.username],
            email = row[UsersTable.email],
            role = row[UsersTable.role],
            kycStatus = row[UsersTable.kycStatus],
            active = row[UsersTable.active],
            createdAt = row[UsersTable.createdAt],
            phone = row[UsersTable.phone],
            businessName = row[UsersTable.businessName],
        )
        AdminUserDetail(
            user = summary,
            residentialAddress = kyc?.get(KycTable.residentialAddress).orEmpty(),
            documentType = kyc?.get(KycTable.documentType).orEmpty(),
            documentNumber = kyc?.get(KycTable.documentNumber).orEmpty(),
            hasIdCopy = kyc?.get(KycTable.documentFrontImageUrl)?.isNotBlank() == true,
            hasAddressProof = kyc?.get(KycTable.addressProofImageUrl)?.isNotBlank() == true,
            reviewNote = kyc?.get(KycTable.reviewNote),
            signedInDevices = DeviceSignInService.names(userId),
            activities = activities(
                userId = userId,
                createdAt = row[UsersTable.createdAt],
                role = row[UsersTable.role],
                kycSubmittedAt = kyc?.get(KycTable.submittedAt),
                documentType = kyc?.get(KycTable.documentType).orEmpty(),
            ),
        )
    }

    fun addLandlord(request: CreateLandlordRequest): AdminUserSummary {
        val email = request.email.trim()
        require(email.contains("@")) { "Enter a valid email." }
        require(request.password.length >= 8) { "Password must be at least 8 characters." }
        val user = AuthService.createLandlord(request)
            ?: throw IllegalArgumentException("That email or username is already registered.")
        return list(UserRole.LANDLORD).first { it.id == user.id }
    }

    fun remove(actorId: String, userId: String) {
        require(actorId != userId) { "You cannot remove your own admin account." }
        val target = transaction {
            UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
        } ?: error("Account was not found.")
        require(target[UsersTable.role] != UserRole.ADMIN) { "Admin accounts stay on the platform." }
        AuthService.deactivateUser(userId)
    }

    fun review(userId: String, status: KycStatus, note: String?) {
        val role = transaction {
            UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.get(UsersTable.role)
        } ?: error("Account was not found.")
        require(role == UserRole.LANDLORD || role == UserRole.STUDENT) {
            "Verification is for student and landlord accounts."
        }
        KycService.review(userId, status, note)
    }

    private fun activities(
        userId: String,
        createdAt: Long,
        role: UserRole,
        kycSubmittedAt: Long?,
        documentType: String,
    ): List<AdminActivity> {
        val items = mutableListOf<AdminActivity>()
        if (createdAt > 0) {
            items += AdminActivity(
                at = createdAt,
                title = "Account created",
                detail = role.name.lowercase().replaceFirstChar { it.titlecase() },
            )
        }
        AccountEventsTable.selectAll()
            .where { AccountEventsTable.userId eq userId }
            .orderBy(AccountEventsTable.createdAt to SortOrder.DESC)
            .limit(20)
            .forEach { row ->
                items += AdminActivity(
                    at = row[AccountEventsTable.createdAt],
                    title = row[AccountEventsTable.title],
                    detail = row[AccountEventsTable.detail],
                )
            }
        val loggedDevice = items.any { it.title.contains("device", ignoreCase = true) }
        if (!loggedDevice) {
            DeviceTokensTable.selectAll()
                .where { DeviceTokensTable.userId eq userId }
                .orderBy(DeviceTokensTable.updatedAt to SortOrder.DESC)
                .limit(5)
                .forEach { row ->
                    val platform = row[DeviceTokensTable.platform].replaceFirstChar { it.titlecase() }
                    items += AdminActivity(
                        at = row[DeviceTokensTable.updatedAt],
                        title = "Device on this account",
                        detail = platform,
                    )
                }
        }
        if (kycSubmittedAt != null && kycSubmittedAt > 0) {
            val studentId = documentType == "Student ID"
            items += AdminActivity(
                kycSubmittedAt,
                if (studentId) "Submitted a student ID" else "Submitted identity documents",
                if (studentId) "University student ID" else "ID and residential address",
            )
        }
        ListingsTable.selectAll()
            .where { ListingsTable.landlordId eq userId }
            .orderBy(ListingsTable.createdAt to SortOrder.DESC)
            .limit(12)
            .forEach { row ->
                items += AdminActivity(
                    at = row[ListingsTable.createdAt],
                    title = "Published a listing",
                    detail = row[ListingsTable.title],
                )
            }
        ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.guestUserId eq userId) or (ListingBookingsTable.landlordId eq userId)
            }
            .orderBy(ListingBookingsTable.createdAt to SortOrder.DESC)
            .limit(12)
            .forEach { row ->
                val listingTitle = ListingsTable.selectAll()
                    .where { ListingsTable.id eq row[ListingBookingsTable.listingId] }
                    .singleOrNull()
                    ?.get(ListingsTable.title)
                    .orEmpty()
                val asGuest = row[ListingBookingsTable.guestUserId] == userId
                items += AdminActivity(
                    at = row[ListingBookingsTable.createdAt],
                    title = if (asGuest) "Requested a stay" else "Received a booking",
                    detail = listOf(listingTitle, row[ListingBookingsTable.status].name)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                )
            }
        return items.sortedByDescending { it.at }.take(25)
    }
}
