package com.rotd.havenpad.community

import com.rotd.havenpad.BillStatus
import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.ListingStatus
import com.rotd.havenpad.PlatformStats
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.db.BillsTable
import com.rotd.havenpad.db.CommunityStoriesTable
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.round

/**
 * Counters for the public landing page. Everything is derived from live rows so the
 * marketing site never shows numbers the platform cannot back up.
 */
object PlatformStatsService {
    fun getStats(): PlatformStats = transaction {
        val listings = ListingsTable.selectAll().map {
            it[ListingsTable.status] to it[ListingsTable.university]
        }
        val verifiedListings = listings.count { (status, _) -> status == ListingStatus.VERIFIED }
        val universities = listings
            .mapNotNull { (_, university) -> university?.trim()?.lowercase()?.ifBlank { null } }
            .distinct()
            .size

        val studentsHoused = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.status eq BookingStatus.CONFIRMED }
            .mapNotNull { it[ListingBookingsTable.guestUserId] }
            .distinct()
            .size

        val landlords = UsersTable.selectAll()
            .where { UsersTable.role eq UserRole.LANDLORD }
            .count()
            .toInt()

        val protectedInEscrow = BillsTable.selectAll()
            .where { BillsTable.status eq BillStatus.PAID }
            .sumOf { it[BillsTable.totalAmount] }

        val ratings = CommunityStoriesTable.selectAll().map { it[CommunityStoriesTable.rating] }

        PlatformStats(
            studentsHoused = studentsHoused,
            verifiedListings = verifiedListings,
            universitiesCovered = universities,
            landlords = landlords,
            protectedInEscrow = protectedInEscrow,
            stories = ratings.size,
            averageRating = if (ratings.isEmpty()) 0.0 else round(ratings.average() * 10) / 10
        )
    }
}
