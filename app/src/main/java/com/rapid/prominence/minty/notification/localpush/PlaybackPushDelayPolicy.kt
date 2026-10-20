package com.rapid.prominence.minty.notification.localpush

object PlaybackPushDelayPolicy {
    const val DEFAULT_DELAY_SEC = 5

    fun resolveDelaySec(configuredDelaySec: Int): Int {
        return configuredDelaySec.takeIf { it >= 0 } ?: DEFAULT_DELAY_SEC
    }
}
