package com.rotd.havenpad.trust

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrowsePlaceTest {
    @Test
    fun tightFixIncludesTheNeighborhood() {
        assertEquals(
            "Kilimani, Nairobi",
            browsePlaceName("Kilimani", "Nairobi", accuracyMeters = 40f),
        )
    }

    @Test
    fun coarseFixDropsTheNeighborhood() {
        assertEquals(
            "Nairobi",
            browsePlaceName("Kilimani", "Nairobi", accuracyMeters = 800f),
        )
    }

    @Test
    fun missingCityProducesNoLabel() {
        assertNull(browsePlaceName("Kilimani", null, accuracyMeters = null))
        assertNull(browsePlaceName(null, "  ", accuracyMeters = 50f))
    }
}
