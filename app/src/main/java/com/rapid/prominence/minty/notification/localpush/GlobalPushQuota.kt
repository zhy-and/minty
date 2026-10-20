package com.rapid.prominence.minty.notification.localpush

/** Selects the global frequency-cap window from the elapsed time since installation. */
object GlobalPushQuota {
    const val FIRST_24_HOURS_MS = 24L * 60 * 60 * 1000

    enum class Period {
        FIRST_24_HOURS,
        DAILY,
    }

    data class Selection(
        val period: Period,
        val max: Int,
    )

    /** Legacy configs without first_max retain daily_max as their first-day cap. */
    fun select(firstMax: Int?, dailyMax: Int, installAgeMs: Long): Selection {
        return if (installAgeMs < FIRST_24_HOURS_MS) {
            Selection(Period.FIRST_24_HOURS, firstMax ?: dailyMax)
        } else {
            Selection(Period.DAILY, dailyMax)
        }
    }
}
