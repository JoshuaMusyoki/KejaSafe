package com.rotd.havenpad.call

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatElapsedTest {
    @Test
    fun elapsedTimeIsHoursMinutesSeconds() {
        assertEquals("00:00:00", formatElapsed(0))
        assertEquals("00:01:05", formatElapsed(65))
        assertEquals("01:01:01", formatElapsed(3661))
    }
}
