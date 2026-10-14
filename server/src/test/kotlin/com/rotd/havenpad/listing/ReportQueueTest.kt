package com.rotd.havenpad.listing

import com.rotd.havenpad.ReviewReportStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportQueueTest {
    @Test
    fun anEmptyReportDoesNotEnterTheAdminQueue() {
        assertFalse(isActionableReport(ReviewReportStatus.NONE, ""))
        assertFalse(isActionableReport(ReviewReportStatus.DISMISSED, "Wrong house"))
    }

    @Test
    fun aWrittenReportStaysVisibleUntilItIsDismissed() {
        assertTrue(isActionableReport(ReviewReportStatus.OPEN, "The student never stayed here"))
        assertTrue(isActionableReport(ReviewReportStatus.FOLLOWED_UP, "Asked the student"))
        assertTrue(isActionableReport(ReviewReportStatus.NONE, "Sent before the status was saved"))
    }
}
