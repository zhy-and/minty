package com.rapid.prominence.minty.runtime

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rapid.prominence.minty.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/** EnMusic's kp_config_v1 segment format; unsupported keepalive routes are ignored. */
object RuntimeRemoteConfig {
    const val KEY = "kp_config_v1"
    private const val TAG = "MintyRuntimeConfig"
    private val coldFetchStarted = AtomicBoolean(false)
    private val hotFetchInFlight = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val defaults = """
        {"version":1,
         "organic":{"kpa_boot_switch":"1","kpa_service_switch":"1","kpa_resume_switch":"1","kpa_alarm_switch":"0","kpa_job_switch":"0","kpa_fcm_switch":"0","kpa_home_switch":"0","kpa_delete_intent_switch":"0"},
         "paid":{"kpa_boot_switch":"1","kpa_service_switch":"1","kpa_resume_switch":"1","kpa_alarm_switch":"0","kpa_job_switch":"1","kpa_fcm_switch":"1","kpa_home_switch":"0","kpa_delete_intent_switch":"1"}}
    """.trimIndent()

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val frc = FirebaseRemoteConfig.getInstance()
        frc.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L).build())
        frc.setDefaultsAsync(mapOf(KEY to defaults))
        UserSegmentHelper.addOnResolvedListener { applyForCurrentSegment(context.applicationContext) }
    }

    fun refreshIfNeeded(context: Context, hotStart: Boolean = false) {
        initialize(context)
        if (hotStart) {
            if (!hotFetchInFlight.compareAndSet(false, true)) return
        } else if (!coldFetchStarted.compareAndSet(false, true)) return
        if (UserSegmentHelper.isResolved()) applyForCurrentSegment(context)
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnSuccessListener { if (UserSegmentHelper.isResolved()) applyForCurrentSegment(context) }
            .addOnFailureListener { Log.w(TAG, "FRC fetch failed", it) }
            .addOnCompleteListener { if (hotStart) hotFetchInFlight.set(false) }
    }

    fun applyForCurrentSegment(context: Context): Boolean {
        if (!UserSegmentHelper.isResolved()) return false
        val raw = FirebaseRemoteConfig.getInstance().getString(KEY).ifBlank { defaults }
        val segment = parseSegment(raw, UserSegmentHelper.currentSegmentKey())
            ?: parseSegment(defaults, "organic") ?: return false
        val beforeService = RuntimeConfig.isServiceEnabled(context)
        val beforeFcm = RuntimeConfig.isFcmEnabled(context)
        for (key in listOf(RuntimeConfig.SERVICE_SWITCH, RuntimeConfig.RESUME_SWITCH, RuntimeConfig.FCM_SWITCH)) {
            val value = segment.get(key)?.let { element ->
                if (!element.isJsonPrimitive) null else when (element.asString) {
                    "1", "true" -> "1"
                    "0", "false" -> "0"
                    else -> null
                }
            }
            if (value != null) RuntimeConfig.write(context, key, value)
        }
        val changed = beforeService != RuntimeConfig.isServiceEnabled(context) ||
            beforeFcm != RuntimeConfig.isFcmEnabled(context)
        if (beforeService && !RuntimeConfig.isServiceEnabled(context)) {
            context.stopService(Intent(context, NotificationToolbarService::class.java))
        }
        if (changed) MintyRuntime.bootstrap(context, MintyRuntime.Trigger.REMOTE_CONFIG)
        return changed
    }

    internal fun parseSegment(raw: String, key: String): JsonObject? = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        if (!root.has("organic") && !root.has("paid")) root else {
            val selected = if (key == "paid") root.getAsJsonObject("paid")
                ?: root.getAsJsonObject("organic") else root.getAsJsonObject("organic")
            selected
        }
    }.getOrNull()
}
