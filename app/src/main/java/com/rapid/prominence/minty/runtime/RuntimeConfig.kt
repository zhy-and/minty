package com.rapid.prominence.minty.runtime

import android.content.Context
import com.tencent.mmkv.MMKV

/** The three EnMusic runtime switches retained for Minty's agreed keepalive scope. */
object RuntimeConfig {
    const val FCM_TOPIC = "minty_wake"
    const val QUICK_JOB_MIN_LATENCY_MS = 3_999L
    const val QUICK_JOB_OVERRIDE_DEADLINE_MS = 5_999L
    const val FCM_TOPIC_RETRY_INITIAL_MS = 30_000L
    const val FCM_TOPIC_RETRY_MAX_MS = 86_400_000L
    const val SERVICE_SWITCH = "kpa_service_switch"
    const val RESUME_SWITCH = "kpa_resume_switch"
    const val FCM_SWITCH = "kpa_fcm_switch"

    fun isServiceEnabled(context: Context): Boolean = read(context, SERVICE_SWITCH, true)
    fun isResumeEnabled(context: Context): Boolean = read(context, RESUME_SWITCH, true)
    // Match EnMusic's stored-switch default: paid turns on only after its FRC segment is applied.
    fun isFcmEnabled(context: Context): Boolean = read(context, FCM_SWITCH, false)

    private fun read(context: Context, key: String, default: Boolean): Boolean {
        ensureMmkv(context)
        return MMKV.defaultMMKV().getString(key, if (default) "1" else "0") == "1"
    }

    internal fun write(context: Context, key: String, value: String) {
        ensureMmkv(context)
        MMKV.defaultMMKV().putString(key, value)
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure { MMKV.initialize(context.applicationContext) }
    }
}
