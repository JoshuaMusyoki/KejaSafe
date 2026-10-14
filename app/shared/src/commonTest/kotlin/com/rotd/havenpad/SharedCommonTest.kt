package com.rotd.havenpad

import com.rotd.havenpad.web.canPostStory
import com.rotd.havenpad.web.formatMoney
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {
    @Test
    fun landingMoneyUsesKenyanScale() {
        assertEquals("KES 1.4K", formatMoney(1400.0))
        assertEquals("KES 0", formatMoney(0.0))
    }

    @Test
    fun onlyStudentsAndLandlordsCanPostAStory() {
        assertFalse(null.canPostStory())
        val student = User(
            id = "s",
            email = "s@havenpad.app",
            username = "s",
            fullName = "Student",
            role = UserRole.STUDENT,
        )
        assertTrue(student.canPostStory())
        assertFalse(student.copy(role = UserRole.ADMIN).canPostStory())
    }
}
