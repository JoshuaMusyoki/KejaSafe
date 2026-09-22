package com.rotd.kejasafe.media

import com.rotd.kejasafe.BillStatus
import com.rotd.kejasafe.RentBill

/**
 * Invoice PDF for rent bills — pure Kotlin so Android/iOS/desktop can preview & save
 * without waiting on the server endpoint.
 */
object InvoicePdfGenerator {
    fun render(
        bill: RentBill,
        tenantName: String = bill.tenantName,
        propertyAddress: String = "",
        issuedAtMillis: Long = bill.dueDate - 7L * 86_400_000L
    ): ByteArray {
        val invoiceNo = invoiceNumber(bill)
        val period = "${monthLabel(bill.month)} ${bill.year}"
        val propertyLabel = bill.propertyTitle.ifBlank { bill.propertyId }
        val statusLabel = when (bill.status) {
            BillStatus.PAID -> "PAID"
            BillStatus.PENDING_PAYMENT -> "PENDING"
            BillStatus.PARTIALLY_PAID -> "PARTIAL"
            BillStatus.OVERDUE -> "OVERDUE"
            BillStatus.FAILED -> "FAILED"
            BillStatus.CANCELLED -> "CANCELLED"
            BillStatus.UNPAID -> "UNPAID"
        }
        val issued = formatDate(issuedAtMillis.coerceAtLeast(0L))
        val due = formatDate(bill.dueDate)
        val who = tenantName.ifBlank { "Tenant" }

        val ops = StringBuilder()
        ops.append("0.04 0.42 0.33 rg\n0 720 612 72 re f\n")
        ops.append("1 1 1 rg\n")
        ops.append(textAt(40.0, 752.0, "/F2", 22, "KejaSafe"))
        ops.append(textAt(40.0, 734.0, "/F1", 10, "Secure rent and housing platform"))
        ops.append(textAt(420.0, 752.0, "/F2", 20, "INVOICE"))
        ops.append(textAt(420.0, 734.0, "/F1", 10, invoiceNo))
        ops.append("0.83 0.65 0.15 rg\n0 716 612 4 re f\n")
        ops.append("0.12 0.14 0.16 rg\n")

        var y = 690.0
        ops.append(textAt(40.0, y, "/F1", 9, "Issue date"))
        ops.append(textAt(140.0, y, "/F1", 9, "Due date"))
        ops.append(textAt(260.0, y, "/F1", 9, "Billing period"))
        ops.append(textAt(420.0, y, "/F1", 9, "Status"))
        y -= 14
        ops.append(textAt(40.0, y, "/F2", 11, issued))
        ops.append(textAt(140.0, y, "/F2", 11, due))
        ops.append(textAt(260.0, y, "/F2", 11, period))
        val statusColor = when (bill.status) {
            BillStatus.PAID -> "0.04 0.42 0.33"
            BillStatus.PENDING_PAYMENT -> "0.20 0.45 0.70"
            BillStatus.PARTIALLY_PAID -> "0.55 0.40 0.10"
            BillStatus.OVERDUE, BillStatus.FAILED, BillStatus.CANCELLED -> "0.75 0.22 0.17"
            BillStatus.UNPAID -> "0.72 0.48 0.08"
        }
        ops.append("$statusColor rg\n420 ${fmt(y - 4)} 80 16 re f\n")
        ops.append("1 1 1 rg\n")
        ops.append(textAt(432.0, y, "/F2", 10, statusLabel))
        ops.append("0.12 0.14 0.16 rg\n")

        y = 640.0
        ops.append("0.93 0.95 0.94 rg\n")
        ops.append("40 ${fmt(y - 78)} 250 90 re f\n")
        ops.append("322 ${fmt(y - 78)} 250 90 re f\n")
        ops.append("0.12 0.14 0.16 rg\n")
        ops.append(textAt(52.0, y, "/F2", 10, "BILL TO"))
        ops.append(textAt(52.0, y - 18, "/F2", 12, who.take(32)))
        ops.append(textAt(52.0, y - 34, "/F1", 10, "Tenant on KejaSafe"))
        ops.append(textAt(334.0, y, "/F2", 10, "PROPERTY"))
        ops.append(textAt(334.0, y - 18, "/F2", 12, propertyLabel.take(34)))
        if (propertyAddress.isNotBlank()) {
            wrapLines(propertyAddress, 36).take(2).forEachIndexed { i, line ->
                ops.append(textAt(334.0, y - 34 - i * 14.0, "/F1", 10, line))
            }
        } else {
            ops.append(textAt(334.0, y - 34, "/F1", 10, "Property ID: ${bill.propertyId.take(12)}"))
        }

        y = 520.0
        ops.append(textAt(40.0, y, "/F2", 12, "Charges"))
        y -= 18
        ops.append("0.04 0.42 0.33 rg\n40 ${fmt(y - 6)} 532 22 re f\n")
        ops.append("1 1 1 rg\n")
        ops.append(textAt(52.0, y, "/F2", 10, "#"))
        ops.append(textAt(80.0, y, "/F2", 10, "Description"))
        ops.append(textAt(460.0, y, "/F2", 10, "Amount (KES)"))
        ops.append("0.12 0.14 0.16 rg\n")
        y -= 28

        bill.items.forEachIndexed { index, item ->
            if (y < 160) return@forEachIndexed
            if (index % 2 == 0) {
                ops.append("0.96 0.97 0.96 rg\n40 ${fmt(y - 6)} 532 22 re f\n")
                ops.append("0.12 0.14 0.16 rg\n")
            }
            val amountLabel = if (item.amount <= 0.0 && item.name.contains("token", ignoreCase = true)) {
                "TOKEN"
            } else {
                formatAmount(item.amount)
            }
            ops.append(textAt(52.0, y, "/F1", 10, "${index + 1}"))
            ops.append(textAt(80.0, y, "/F1", 10, item.name.take(40)))
            ops.append(textAt(460.0, y, "/F1", 10, amountLabel))
            ops.append("0.85 0.88 0.86 RG\n0.5 w\n")
            ops.append("40 ${fmt(y - 8)} m 572 ${fmt(y - 8)} l S\n")
            ops.append("0.12 0.14 0.16 rg\n")
            y -= 26
        }

        if (bill.depositIncluded) {
            y -= 4
            ops.append(textAt(52.0, y, "/F1", 9, "* Includes one-time security deposit"))
            y -= 16
        }
        bill.electricityNote?.takeIf { it.isNotBlank() }?.let { note ->
            ops.append(textAt(52.0, y, "/F1", 9, "Note: ${asciiSafe(note).take(70)}"))
            y -= 16
        }

        val totalsY = (y - 10).coerceIn(130.0, 200.0)
        ops.append("0.04 0.42 0.33 rg\n340 ${fmt(totalsY - 50)} 232 62 re f\n")
        ops.append("1 1 1 rg\n")
        ops.append(textAt(356.0, totalsY - 8, "/F1", 10, "TOTAL DUE"))
        ops.append(textAt(356.0, totalsY - 32, "/F2", 18, "KES ${formatAmount(bill.totalAmount)}"))

        ops.append("0.12 0.14 0.16 rg\n")
        ops.append(textAt(40.0, 110.0, "/F2", 11, "Payment"))
        val receipt = bill.mpesaReceiptCode?.takeIf { it.isNotBlank() }
        val payerName = bill.mpesaPayerName?.takeIf { it.isNotBlank() }
        if (bill.status == BillStatus.PAID && receipt != null) {
            val paidByText = if (payerName != null) " (paid by $payerName)" else ""
            ops.append(textAt(40.0, 94.0, "/F1", 10, "Paid via M-Pesa$paidByText. Confirmation code: $receipt"))
            ops.append(textAt(40.0, 80.0, "/F1", 10, "Keep this invoice for your records."))
        } else {
            ops.append(textAt(40.0, 94.0, "/F1", 10, "Pay securely in the KejaSafe app via M-Pesa."))
            ops.append(textAt(40.0, 80.0, "/F1", 10, "Funds for deposits may be held in escrow until move-in."))
        }
        ops.append("0.83 0.65 0.15 rg\n40 58 532 2 re f\n")
        ops.append("0.45 0.48 0.50 rg\n")
        ops.append(textAt(40.0, 40.0, "/F1", 8, "Thank you for choosing KejaSafe."))
        ops.append(textAt(40.0, 28.0, "/F1", 8, "Invoice $invoiceNo | Ref ${bill.id.take(12)}"))

        return assemblePdf(ops.toString().encodeToByteArray())
    }

    private fun assemblePdf(streamBytes: ByteArray): ByteArray {
        val objects = ArrayList<ByteArray>(6)
        fun add(s: String) { objects += s.encodeToByteArray() }

        add("<< /Type /Catalog /Pages 2 0 R >>")
        add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        add(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>"
        )

        val content = buildList {
            addAll("<< /Length ${streamBytes.size} >>\nstream\n".encodeToByteArray().toList())
            addAll(streamBytes.toList())
            addAll("\nendstream".encodeToByteArray().toList())
        }.toByteArray()
        objects += content
        add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>")

        val out = ArrayList<Byte>()
        fun w(bytes: ByteArray) { out.addAll(bytes.toList()) }
        fun w(s: String) { w(s.encodeToByteArray()) }

        w("%PDF-1.4\n")
        w(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))
        val offsets = IntArray(objects.size + 1)
        for (i in objects.indices) {
            offsets[i + 1] = out.size
            w("${i + 1} 0 obj\n")
            w(objects[i])
            w("\nendobj\n")
        }
        val xrefPos = out.size
        w("xref\n0 ${objects.size + 1}\n")
        w("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            w(offsets[i].toString().padStart(10, '0') + " 00000 n \n")
        }
        w("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        w("startxref\n$xrefPos\n%%EOF\n")
        return out.toByteArray()
    }

    private fun textAt(x: Double, y: Double, font: String, size: Int, text: String): String =
        "BT\n$font $size Tf\n${fmt(x)} ${fmt(y)} Td\n(${escapePdf(text)}) Tj\nET\n"

    private fun fmt(n: Double): String {
        val whole = n.toLong()
        return if (n == whole.toDouble()) whole.toString() else {
            val scaled = ((n * 100.0) + if (n >= 0) 0.5 else -0.5).toLong()
            val abs = kotlin.math.abs(scaled)
            val sign = if (scaled < 0) "-" else ""
            "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
        }
    }

    private fun invoiceNumber(bill: RentBill): String {
        val shortId = bill.id.filter { it.isLetterOrDigit() }.takeLast(6).uppercase()
            .ifBlank { bill.id.takeLast(6).uppercase() }
        return "INV-${bill.year}${bill.month.toString().padStart(2, '0')}-$shortId"
    }

    private fun wrapLines(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun asciiSafe(text: String): String =
        text.map { c -> if (c.code in 32..126) c else ' ' }.joinToString("")

    private fun escapePdf(text: String): String =
        asciiSafe(text).map { c ->
            when (c) {
                '\\', '(', ')' -> "\\$c"
                else -> c.toString()
            }
        }.joinToString("")

    private fun formatAmount(value: Double): String {
        val whole = value.toLong()
        if (value == whole.toDouble()) {
            return groupThousands(whole)
        }
        val cents = ((value * 100.0) + if (value >= 0) 0.5 else -0.5).toLong()
        val abs = kotlin.math.abs(cents)
        return "${groupThousands(abs / 100)}.${(abs % 100).toString().padStart(2, '0')}"
    }

    private fun groupThousands(n: Long): String {
        val abs = kotlin.math.abs(n).toString()
        val grouped = abs.reversed().chunked(3).joinToString(",").reversed()
        return if (n < 0) "-$grouped" else grouped
    }

    private fun monthLabel(month: Int): String {
        val names = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return names.getOrElse(month - 1) { month.toString() }
    }

    private fun formatDate(millis: Long): String {
        val day = millis / 86_400_000L
        var days = day.toInt()
        var year = 1970
        while (true) {
            val diy = if (isLeap(year)) 366 else 365
            if (days < diy) break
            days -= diy
            year++
        }
        val lengths = intArrayOf(31, if (isLeap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 1
        for (len in lengths) {
            if (days < len) break
            days -= len
            month++
        }
        val d = days + 1
        return "${d.toString().padStart(2, '0')} ${monthLabel(month).take(3)} $year"
    }

    private fun isLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
