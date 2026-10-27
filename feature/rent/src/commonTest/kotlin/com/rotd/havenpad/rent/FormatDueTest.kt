package com.rotd.havenpad.rent

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDueTest {
    @Test
    fun epochIsTheFirstDayOf1970() {
        assertEquals("1970-01-01", formatDue(0L))
    }

    @Test
    fun aLaterUtcMidnightKeepsTheCalendarDay() {
        val day = 20_000L * 86_400_000L
        assertEquals("2024-10-04", formatDue(day))
    }
}
