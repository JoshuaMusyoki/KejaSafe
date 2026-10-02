package com.rotd.havenpad.rent

import com.rotd.havenpad.RentBill
import com.rotd.havenpad.media.InvoicePdfGenerator

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
