package com.rotd.havenpad.db

import com.rotd.havenpad.domain.listing.*
import org.jetbrains.exposed.v1.core.Table

object PropertyRequestsTable : Table("property_requests") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val size = enumerationByName("size", 30, HouseSize::class)
    val houseType = enumerationByName("house_type", 30, HouseType::class).nullable()
    val listingType = enumerationByName("listing_type", 30, ListingType::class)
    val area = varchar("area", 200)
    val distance = enumerationByName("distance", 50, DistancePreference::class)
    val budgetFrom = long("budget_from")
    val budgetTo = long("budget_to")
    val additionalInfo = text("additional_info").nullable()
    val agentsCanCall = bool("agents_can_call").default(false)
    val paymentStatus = varchar("payment_status", 20).default("PENDING")
    val mpesaCheckoutRequestId = varchar("mpesa_checkout_request_id", 100).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
