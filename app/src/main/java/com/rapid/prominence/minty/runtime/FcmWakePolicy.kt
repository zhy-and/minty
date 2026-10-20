package com.rapid.prominence.minty.runtime

/** FCM's actual delivery priority, not originalPriority or a data field, controls the FGS exemption. */
object FcmWakePolicy {
    const val HIGH_PRIORITY = 1

    fun isWakeType(type: String?): Boolean =
        type.equals("wake", true) || type.equals("kpa_wake", true)

    fun hasForegroundServiceExemption(deliveredPriority: Int): Boolean =
        deliveredPriority == HIGH_PRIORITY

    fun shouldWake(deliveredPriority: Int, type: String?, enabled: Boolean): Boolean =
        enabled && (hasForegroundServiceExemption(deliveredPriority) || isWakeType(type))
}
