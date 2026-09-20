package com.rotd.kejasafe.rent

import com.rotd.kejasafe.BillItem
import com.rotd.kejasafe.BillStatus
import com.rotd.kejasafe.BookingStatus
import com.rotd.kejasafe.ElectricityBilling
import com.rotd.kejasafe.ExtraCharge
import com.rotd.kejasafe.GenerateBillRequest
import com.rotd.kejasafe.ListingCharges
import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.db.BillItemsTable
import com.rotd.kejasafe.db.BillsTable
import com.rotd.kejasafe.db.ListingBookingsTable
import com.rotd.kejasafe.db.ListingsTable
import com.rotd.kejasafe.db.UsersTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import com.rotd.kejasafe.UpdateBillRequest
import com.rotd.kejasafe.payment.MpesaService
import java.time.LocalDate
import java.util.UUID

object RentService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun generateBill(
        userId: String,
        propertyId: String,
        month: Int,
        year: Int,
        baseRent: Double,
        utilities: List<BillItem>,
        propertyTitle: String = "",
        tenantName: String = "",
        bookingId: String? = null,
        depositIncluded: Boolean = false,
        electricityNote: String? = null
    ): RentBill = transaction {
        val id = UUID.randomUUID().toString()
        val payable = utilities.filter { it.amount > 0 || it.name.contains("token", ignoreCase = true) }
        val total = baseRent + payable.filter { it.amount > 0 }.sumOf { it.amount }
        val due = System.currentTimeMillis() + 604800000 // 7 days
        val resolvedTenant = tenantName.ifBlank {
            UsersTable.selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.get(UsersTable.fullName)
                .orEmpty()
        }

        BillsTable.insert {
            it[BillsTable.id] = id
            it[BillsTable.userId] = userId
            it[BillsTable.propertyId] = propertyId
            it[BillsTable.propertyTitle] = propertyTitle
            it[BillsTable.tenantName] = resolvedTenant
            it[BillsTable.bookingId] = bookingId
            it[BillsTable.month] = month
            it[BillsTable.year] = year
            it[BillsTable.totalAmount] = total
            it[BillsTable.dueDate] = due
            it[BillsTable.status] = BillStatus.UNPAID
            it[BillsTable.depositIncluded] = depositIncluded
            it[BillsTable.electricityNote] = electricityNote
        }

        BillItemsTable.insert {
            it[billId] = id
            it[name] = "Base rent"
            it[amount] = baseRent
        }

        payable.forEach { item ->
            BillItemsTable.insert {
                it[billId] = id
                it[name] = item.name
                it[amount] = item.amount
            }
        }

        RentBill(
            id = id,
            userId = userId,
            propertyId = propertyId,
            propertyTitle = propertyTitle,
            tenantName = resolvedTenant,
            bookingId = bookingId,
            month = month,
            year = year,
            items = listOf(BillItem("Base rent", baseRent)) + payable,
            totalAmount = total,
            dueDate = due,
            depositIncluded = depositIncluded,
            electricityNote = electricityNote
        )
    }

    /**
     * Landlord issues a rent bill for a confirmed booking using listing charges.
     */
    fun generateBillFromBooking(landlordId: String, request: GenerateBillRequest): RentBill = transaction {
        val booking = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq request.bookingId }
            .singleOrNull()
            ?: error("Booking not found")
        if (booking[ListingBookingsTable.landlordId] != landlordId) {
            error("Not your listing booking")
        }
        if (booking[ListingBookingsTable.status] != BookingStatus.CONFIRMED) {
            error("Approve the booking before generating a bill")
        }
        val guestId = booking[ListingBookingsTable.guestUserId]
            ?: error("Booking has no tenant — cannot bill")
        val listingId = booking[ListingBookingsTable.listingId]
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")
        val rent = listing[ListingsTable.pricePerMonth]
            ?: error("Set a monthly price on the listing before generating a bill")
        if (rent <= 0) error("Listing price must be greater than zero")

        val today = LocalDate.now()
        val month = request.month ?: today.monthValue
        val year = request.year ?: today.year

        val existing = BillsTable.selectAll()
            .where {
                (BillsTable.bookingId eq request.bookingId) and
                    (BillsTable.month eq month) and
                    (BillsTable.year eq year)
            }
            .count()
        if (existing > 0) {
            error("A bill for this booking period already exists")
        }

        val charges = chargesFromListing(listing)
        val priorBillsForBooking = BillsTable.selectAll()
            .where { BillsTable.bookingId eq request.bookingId }
            .count()
        val includeDeposit = request.includeDeposit
            ?: (priorBillsForBooking == 0L && charges.deposit > 0)

        val utilities = if (request.utilities.isNotEmpty()) {
            request.utilities
        } else {
            buildList {
                addAll(charges.utilityLineItems().filter {
                    // Keep token note as informational 0-amount line only when token mode.
                    it.amount > 0 || it.name.contains("token", ignoreCase = true)
                })
                if (includeDeposit && charges.deposit > 0) {
                    add(BillItem("Security deposit", charges.deposit))
                }
            }
        }

        val electricityNote = when (charges.electricityBilling) {
            ElectricityBilling.TOKEN -> "Electricity is prepaid token — tenant tops up separately"
            ElectricityBilling.MONTHLY -> null
        }

        generateBill(
            userId = guestId,
            propertyId = listingId,
            month = month,
            year = year,
            baseRent = rent,
            utilities = utilities,
            propertyTitle = listing[ListingsTable.title],
            tenantName = booking[ListingBookingsTable.guestName],
            bookingId = request.bookingId,
            depositIncluded = includeDeposit && charges.deposit > 0,
            electricityNote = electricityNote
        )
    }

    fun getBillsForUser(userId: String): List<RentBill> = transaction {
        val ownListings = ListingsTable.selectAll()
            .where { ListingsTable.landlordId eq userId }
            .map { it[ListingsTable.id] }
            .toSet()

        BillsTable.selectAll()
            .mapNotNull { row ->
                val isTenant = row[BillsTable.userId] == userId
                val isLandlord = row[BillsTable.propertyId] in ownListings
                if (!isTenant && !isLandlord) return@mapNotNull null
                demoteUnverifiedPaid(row)
                toBill(
                    BillsTable.selectAll().where { BillsTable.id eq row[BillsTable.id] }.single()
                )
            }
            .sortedByDescending { it.dueDate }
    }

    fun getBillForUser(billId: String, userId: String): RentBill? = transaction {
        val row = BillsTable.selectAll()
            .where { BillsTable.id eq billId }
            .singleOrNull()
            ?: return@transaction null
        val isTenant = row[BillsTable.userId] == userId
        val isLandlord = ListingsTable.selectAll()
            .where { ListingsTable.id eq row[BillsTable.propertyId] }
            .singleOrNull()
            ?.get(ListingsTable.landlordId) == userId
        if (!isTenant && !isLandlord) return@transaction null
        demoteUnverifiedPaid(row)
        toBill(
            BillsTable.selectAll().where { BillsTable.id eq billId }.single()
        )
    }

    /**
     * Starts M-Pesa STK Push for a rent bill. Status becomes PENDING_PAYMENT until callback.
     */
    suspend fun initiateBillPayment(
        billId: String,
        payerUserId: String,
        phoneNumber: String
    ): Map<String, String> {
        val phone = phoneNumber.trim()
        if (phone.isBlank()) error("M-Pesa phone number is required")

        val (amountKes, reference) = transaction {
            val row = BillsTable.selectAll()
                .where { BillsTable.id eq billId }
                .singleOrNull()
                ?: error("Bill not found")
            if (row[BillsTable.userId] != payerUserId) error("Not your bill")

            demoteUnverifiedPaid(row)
            val fresh = BillsTable.selectAll().where { BillsTable.id eq billId }.single()
            val status = fresh[BillsTable.status]
            val receipt = fresh[BillsTable.mpesaReceiptCode]
            if (status == BillStatus.PAID && !receipt.isNullOrBlank()) {
                error("Bill already paid")
            }

            val kes = fresh[BillsTable.totalAmount].toLong().coerceAtLeast(1L)
            BillsTable.update({ BillsTable.id eq billId }) {
                it[BillsTable.status] = BillStatus.PENDING_PAYMENT
                it[mpesaReceiptCode] = null
            }
            kes to billId.take(12)
        }

        val stk = try {
            MpesaService.initiateStkPush(
                phoneNumber = phone,
                amountKes = amountKes,
                reference = reference,
                description = "Rent bill"
            )
        } catch (e: Exception) {
            transaction {
                BillsTable.update({ BillsTable.id eq billId }) {
                    it[status] = BillStatus.UNPAID
                }
            }
            throw e
        }

        if (!stk.accepted) {
            transaction {
                BillsTable.update({ BillsTable.id eq billId }) {
                    it[status] = BillStatus.UNPAID
                }
            }
            error(stk.failureMessage)
        }

        transaction {
            BillsTable.update({ BillsTable.id eq billId }) {
                it[mpesaCheckoutRequestId] = stk.CheckoutRequestID
            }
        }

        return mapOf(
            "billId" to billId,
            "checkoutRequestId" to (stk.CheckoutRequestID ?: ""),
            "message" to (stk.CustomerMessage
                ?: "STK Push sent to $phone. Enter your M-Pesa PIN on your phone.")
        )
    }

    fun handleMpesaCallback(
        checkoutRequestId: String,
        paid: Boolean,
        resultDesc: String,
        receiptCode: String? = null
    ) = transaction {
        val row = BillsTable.selectAll()
            .where { BillsTable.mpesaCheckoutRequestId eq checkoutRequestId }
            .singleOrNull() ?: return@transaction
        if (row[BillsTable.status] == BillStatus.PAID &&
            !row[BillsTable.mpesaReceiptCode].isNullOrBlank()
        ) {
            return@transaction
        }
        if (paid) {
            val code = receiptCode?.takeIf { it.isNotBlank() } ?: "MPESA-OK"
            BillsTable.update({ BillsTable.id eq row[BillsTable.id] }) {
                it[status] = BillStatus.PAID
                it[paymentRequested] = false
                it[mpesaReceiptCode] = code
            }
            println("Bill ${row[BillsTable.id]} marked PAID via M-Pesa ($code / $resultDesc)")
        } else {
            BillsTable.update({ BillsTable.id eq row[BillsTable.id] }) {
                it[status] = BillStatus.UNPAID
                it[mpesaReceiptCode] = null
            }
            println("Bill ${row[BillsTable.id]} M-Pesa failed: $resultDesc")
        }
    }

    /**
     * Earlier builds marked bills PAID without a real M-Pesa receipt — roll those back.
     */
    private fun demoteUnverifiedPaid(row: org.jetbrains.exposed.v1.core.ResultRow) {
        if (row[BillsTable.status] == BillStatus.PAID &&
            row[BillsTable.mpesaReceiptCode].isNullOrBlank()
        ) {
            BillsTable.update({ BillsTable.id eq row[BillsTable.id] }) {
                it[status] = BillStatus.UNPAID
                it[mpesaCheckoutRequestId] = null
            }
        }
    }

    /** Landlord nudges tenant to pay an open bill. */
    fun requestPayment(billId: String, landlordId: String): RentBill = transaction {
        val row = BillsTable.selectAll()
            .where { BillsTable.id eq billId }
            .singleOrNull()
            ?: error("Bill not found")
        val listingId = row[BillsTable.propertyId]
        val owned = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?.get(ListingsTable.landlordId) == landlordId
        if (!owned) error("Not your listing bill")
        if (row[BillsTable.status] == BillStatus.PAID) error("Bill already paid")

        BillsTable.update({ BillsTable.id eq billId }) {
            it[paymentRequested] = true
        }
        toBill(
            BillsTable.selectAll().where { BillsTable.id eq billId }.single()
        )
    }

    /**
     * Landlord updates line items on an unpaid bill (water/electricity vary monthly).
     */
    fun updateBill(billId: String, landlordId: String, request: UpdateBillRequest): RentBill = transaction {
        val row = BillsTable.selectAll()
            .where { BillsTable.id eq billId }
            .singleOrNull()
            ?: error("Bill not found")
        val listingId = row[BillsTable.propertyId]
        val owned = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?.get(ListingsTable.landlordId) == landlordId
        if (!owned) error("Not your listing bill")
        if (row[BillsTable.status] == BillStatus.PAID) error("Paid bills cannot be edited")

        val cleaned = request.items
            .map { BillItem(name = it.name.trim(), amount = it.amount) }
            .filter { it.name.isNotBlank() }
        if (cleaned.isEmpty()) error("Add at least one bill line")
        if (cleaned.none { it.name.contains("rent", ignoreCase = true) }) {
            error("Keep a Base rent line on the bill")
        }

        val total = cleaned.filter { it.amount > 0 }.sumOf { it.amount }
        val depositIncluded = cleaned.any { it.name.contains("deposit", ignoreCase = true) && it.amount > 0 }
        val electricityNote = request.electricityNote?.trim()?.ifBlank { null }
            ?: row[BillsTable.electricityNote]

        BillItemsTable.deleteWhere { BillItemsTable.billId eq billId }
        cleaned.forEach { item ->
            BillItemsTable.insert {
                it[BillItemsTable.billId] = billId
                it[name] = item.name
                it[amount] = item.amount
            }
        }
        BillsTable.update({ BillsTable.id eq billId }) {
            it[totalAmount] = total
            it[BillsTable.depositIncluded] = depositIncluded
            it[BillsTable.electricityNote] = electricityNote
            // Editing amounts invalidates a prior payment nudge until landlord requests again.
            it[paymentRequested] = false
        }

        toBill(BillsTable.selectAll().where { BillsTable.id eq billId }.single())
    }

    fun buildBillPdf(billId: String, requesterUserId: String): ByteArray {
        val bill = getBillForUser(billId, requesterUserId)
            ?: error("Bill not found")
        val tenantName = bill.tenantName.ifBlank {
            transaction {
                UsersTable.selectAll()
                    .where { UsersTable.id eq bill.userId }
                    .singleOrNull()
                    ?.get(UsersTable.fullName)
                    ?: "Tenant"
            }
        }
        val propertyAddress = transaction {
            ListingsTable.selectAll()
                .where { ListingsTable.id eq bill.propertyId }
                .singleOrNull()
                ?.get(ListingsTable.address)
                .orEmpty()
        }
        return BillPdfGenerator.render(
            bill = bill,
            tenantName = tenantName,
            propertyAddress = propertyAddress
        )
    }

    private fun chargesFromListing(row: org.jetbrains.exposed.v1.core.ResultRow): ListingCharges {
        val extras = runCatching {
            json.decodeFromString<List<ExtraCharge>>(row[ListingsTable.extraChargesJson])
        }.getOrElse { emptyList() }
        return ListingCharges(
            garbageCollection = row[ListingsTable.garbageCollection],
            water = row[ListingsTable.water],
            electricityBilling = row[ListingsTable.electricityBilling],
            electricityMonthly = row[ListingsTable.electricityMonthly],
            extraCharges = extras,
            deposit = row[ListingsTable.deposit]
        )
    }

    private fun toBill(row: org.jetbrains.exposed.v1.core.ResultRow): RentBill {
        val billId = row[BillsTable.id]
        val items = BillItemsTable.selectAll().where { BillItemsTable.billId eq billId }.map { itemRow ->
            BillItem(itemRow[BillItemsTable.name], itemRow[BillItemsTable.amount])
        }
        val storedTenant = row[BillsTable.tenantName]
        val tenant = storedTenant.ifBlank {
            UsersTable.selectAll()
                .where { UsersTable.id eq row[BillsTable.userId] }
                .singleOrNull()
                ?.get(UsersTable.fullName)
                .orEmpty()
        }
        return RentBill(
            id = billId,
            userId = row[BillsTable.userId],
            propertyId = row[BillsTable.propertyId],
            propertyTitle = row[BillsTable.propertyTitle],
            tenantName = tenant,
            bookingId = row[BillsTable.bookingId],
            month = row[BillsTable.month],
            year = row[BillsTable.year],
            items = items,
            totalAmount = row[BillsTable.totalAmount],
            dueDate = row[BillsTable.dueDate],
            status = row[BillsTable.status],
            depositIncluded = row[BillsTable.depositIncluded],
            electricityNote = row[BillsTable.electricityNote],
            paymentRequested = row[BillsTable.paymentRequested],
            mpesaReceiptCode = row[BillsTable.mpesaReceiptCode]
        )
    }
}
