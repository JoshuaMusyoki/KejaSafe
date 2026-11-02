package com.rotd.havenpad.web

import kotlin.math.abs
import kotlin.math.round

/** Number formatting for the landing page — kotlin common has no locale formatter. */
internal fun formatCount(value: Int): String = groupDigits(value.toLong())

internal fun formatMoney(value: Double, currency: String = "KES"): String {
    val amount = abs(value)
    return when {
        amount >= 1_000_000 -> "$currency ${oneDecimal(value / 1_000_000)}M"
        amount >= 1_000 -> "$currency ${oneDecimal(value / 1_000)}K"
        else -> "$currency ${groupDigits(round(value).toLong())}"
    }
}

internal fun formatRating(value: Double): String = oneDecimal(value)

private fun oneDecimal(value: Double): String {
    val scaled = round(value * 10).toLong()
    val whole = scaled / 10
    val decimal = abs(scaled % 10)
    return if (decimal == 0L) groupDigits(whole) else "${groupDigits(whole)}.$decimal"
}

private fun groupDigits(value: Long): String {
    val sign = if (value < 0) "-" else ""
    val digits = abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return sign + grouped
}
