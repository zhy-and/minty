package com.rapid.prominence.minty.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeRemoteConfigTest {
    private val config = """{"organic":{"kpa_fcm_switch":"0"},"paid":{"kpa_fcm_switch":"1"}}"""

    @Test fun paidUsesPaidSwitches() {
        assertEquals("1", RuntimeRemoteConfig.parseSegment(config, "paid")?.get("kpa_fcm_switch")?.asString)
    }

    @Test fun organicNeverFallsBackToPaid() {
        assertNull(RuntimeRemoteConfig.parseSegment("""{"paid":{"kpa_fcm_switch":"1"}}""", "organic"))
    }

    @Test fun oldSingleSegmentConfigRemainsReadable() {
        assertEquals("0", RuntimeRemoteConfig.parseSegment("""{"kpa_fcm_switch":"0"}""", "paid")?.get("kpa_fcm_switch")?.asString)
    }
}
