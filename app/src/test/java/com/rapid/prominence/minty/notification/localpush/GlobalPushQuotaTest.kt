package com.rapid.prominence.minty.notification.localpush

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalPushQuotaTest {

    @Test
    fun `uses first max before the first 24 hours end`() {
        val quota = GlobalPushQuota.select(
            firstMax = 2,
            dailyMax = 5,
            installAgeMs = 23L * 60 * 60 * 1000,
        )

        assertEquals(2, quota.max)
        assertEquals(GlobalPushQuota.Period.FIRST_24_HOURS, quota.period)
    }

    @Test
    fun `uses daily max at exactly 24 hours after installation`() {
        val quota = GlobalPushQuota.select(
            firstMax = 2,
            dailyMax = 5,
            installAgeMs = 24L * 60 * 60 * 1000,
        )

        assertEquals(5, quota.max)
        assertEquals(GlobalPushQuota.Period.DAILY, quota.period)
    }

    @Test
    fun `uses daily max during first 24 hours when legacy config omits first max`() {
        val quota = GlobalPushQuota.select(
            firstMax = null,
            dailyMax = 5,
            installAgeMs = 60_000,
        )

        assertEquals(5, quota.max)
        assertEquals(GlobalPushQuota.Period.FIRST_24_HOURS, quota.period)
    }
}
