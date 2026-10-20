package com.rapid.prominence.minty.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamCheckTest {
    @Test fun `a final client's rejected address is not playable`() {
        assertFalse(StreamCheck.accept(403, isLast = true))
        assertFalse(StreamCheck.accept(403, isLast = false))
    }

    @Test fun `a successful address is playable and an unreachable final check is tentative`() {
        assertTrue(StreamCheck.accept(200, isLast = false))
        assertTrue(StreamCheck.accept(null, isLast = true))
        assertFalse(StreamCheck.accept(null, isLast = false))
    }

    @Test fun `adopts a server visitor only when current one is invalid`() {
        assertEquals("CgtFresh", StreamCheck.visitorDataToAdopt("null", "CgtFresh"))
        assertEquals(null, StreamCheck.visitorDataToAdopt("CgtCurrent", "CgtFresh"))
        assertEquals(null, StreamCheck.visitorDataToAdopt(null, "undefined"))
    }
}
