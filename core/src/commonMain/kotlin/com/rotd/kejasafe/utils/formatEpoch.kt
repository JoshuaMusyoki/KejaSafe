package com.rotd.kejasafe.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formats a millisecond epoch timestamp into a user-friendly audit log string.
 * Example output: "Sep 19, 2026 • 10:33 AM"
 */
fun formatEpoch(millis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    if (millis <= 0) return "—"

    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
    val ldt = instant.toLocalDateTime(timeZone)

    val monthName = ldt.month.name.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .take(3)

    val hour12 = when (val h = ldt.hour % 12) {
        0 -> 12
        else -> h
    }
    val amPm = if (ldt.hour >= 12) "PM" else "AM"
    val minuteStr = ldt.minute.toString().padStart(2, '0')

    return "$monthName ${ldt.day}, ${ldt.year} • $hour12:$minuteStr $amPm"
}