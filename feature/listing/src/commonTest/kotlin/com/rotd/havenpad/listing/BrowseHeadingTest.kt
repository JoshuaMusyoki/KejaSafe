package com.rotd.havenpad.listing

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowseHeadingTest {
    @Test
    fun headingFollowsTheFixTheStudentActuallyHas() {
        assertEquals("Finding houses near you", browseHeading(isLocating = true, locationName = null, hasCoordinates = false))
        assertEquals("Houses around Kilimani", browseHeading(isLocating = false, locationName = "Kilimani", hasCoordinates = true))
        assertEquals("Houses near you", browseHeading(isLocating = false, locationName = null, hasCoordinates = true))
        assertEquals("Trending near you", browseHeading(isLocating = false, locationName = null, hasCoordinates = false))
    }
}
