package com.rapid.prominence.minty.runtime

import android.app.ActivityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundStartGateTest {
    @Test fun backgroundAndroid12NeedsActualExemption() {
        val cached = ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
        assertFalse(ForegroundStartGate.allowsStart(31, false, cached, false))
        assertTrue(ForegroundStartGate.allowsStart(31, false, cached, true))
    }

    @Test fun visibleProcessOrPreAndroid12CanStart() {
        val visible = ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        assertTrue(ForegroundStartGate.allowsStart(31, false, visible, false))
        assertTrue(ForegroundStartGate.allowsStart(30, false, ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED, false))
    }
}
