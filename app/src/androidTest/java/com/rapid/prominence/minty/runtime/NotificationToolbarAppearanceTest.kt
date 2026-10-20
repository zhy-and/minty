package com.rapid.prominence.minty.runtime

import androidx.test.platform.app.InstrumentationRegistry
import com.rapid.prominence.minty.R
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationToolbarAppearanceTest {
    @Test
    fun notificationUsesMintAccentInsteadOfTheSystemDefault() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationToolbarHelper.build(context).build()

        assertEquals(context.getColor(R.color.notif_toolbar_icon_bg), notification.color)
    }
}
