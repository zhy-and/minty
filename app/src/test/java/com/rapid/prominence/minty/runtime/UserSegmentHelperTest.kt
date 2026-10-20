package com.rapid.prominence.minty.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class UserSegmentHelperTest {
    @Test fun missingAndOrganicReferrersStayOrganic() {
        assertEquals("organic", UserSegmentHelper.segmentForRefer(""))
        assertEquals("organic", UserSegmentHelper.segmentForRefer("utm_source=organic"))
    }

    @Test fun paidReferrerSelectsPaid() {
        assertEquals("paid", UserSegmentHelper.segmentForRefer("utm_source=facebook"))
    }
}
