package com.rapid.prominence.minty.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmWakePolicyTest {
    @Test
    fun onlyActuallyDeliveredHighPriorityGrantsForegroundServiceExemption() {
        assertTrue(FcmWakePolicy.shouldWake(1, "", true))
        assertTrue(FcmWakePolicy.hasForegroundServiceExemption(1))
        assertFalse(FcmWakePolicy.hasForegroundServiceExemption(0))
    }

    @Test
    fun wakeTypeTriggersAttemptWithoutInventingHighPriorityExemption() {
        assertTrue(FcmWakePolicy.shouldWake(0, "wake", true))
        assertTrue(FcmWakePolicy.shouldWake(0, "kpa_wake", true))
        assertFalse(FcmWakePolicy.hasForegroundServiceExemption(0))
        assertFalse(FcmWakePolicy.shouldWake(0, "wake", false))
    }

    @Test
    fun ordinaryOrOriginalPriorityOnlyMessageDoesNotWake() {
        assertFalse(FcmWakePolicy.shouldWake(0, "", true))
    }
}
