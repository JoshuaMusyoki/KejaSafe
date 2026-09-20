package com.rotd.kejasafe

import com.rotd.kejasafe.rent.BillPdfGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class BillPdfGeneratorTest {
    @Test
    fun rendersInvoiceWithoutThrowing() {
        val bill = RentBill(
            id = "bill-test-123456",
            userId = "u1",
            propertyId = "p1",
            propertyTitle = "Test House",
            tenantName = "Jane Doe",
            month = 9,
            year = 2026,
            items = listOf(
                BillItem("Base rent", 9000.0),
                BillItem("Garbage collection", 100.0),
                BillItem("Water", 400.0),
                BillItem("Electricity (prepaid token)", 0.0),
                BillItem("Security deposit", 9000.0),
            ),
            totalAmount = 18500.0,
            dueDate = System.currentTimeMillis() + 7L * 86_400_000L,
            status = BillStatus.UNPAID,
            depositIncluded = true,
            electricityNote = "Electricity is prepaid token — tenant tops up separately"
        )
        val bytes = BillPdfGenerator.render(
            bill = bill,
            tenantName = "Jane Doe",
            propertyAddress = "Rungiri, Kabete"
        )
        assertTrue(bytes.size > 200, "PDF too small: ${bytes.size}")
        assertTrue(bytes[0] == '%'.code.toByte())
        assertTrue(bytes[1] == 'P'.code.toByte())
        val text = bytes.decodeToString()
        assertTrue(text.contains("%%EOF"))
        assertTrue(text.contains("INVOICE"))
    }
}
