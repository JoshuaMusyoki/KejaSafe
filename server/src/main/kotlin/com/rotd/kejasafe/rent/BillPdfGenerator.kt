package com.rotd.kejasafe.rent

import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.media.InvoicePdfGenerator

/**
 * Server-side wrapper around the shared invoice PDF generator.
 */
object BillPdfGenerator {
    fun render(
        bill: RentBill,
        tenantName: String,
        propertyAddress: String = "",
        issuedAtMillis: Long = System.currentTimeMillis()
    ): ByteArray = InvoicePdfGenerator.render(
        bill = bill,
        tenantName = tenantName,
        propertyAddress = propertyAddress,
        issuedAtMillis = issuedAtMillis
    )
}
