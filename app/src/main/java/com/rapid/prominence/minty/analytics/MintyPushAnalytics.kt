package com.rapid.prominence.minty.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFER_INFO
import com.rapid.prominence.minty.runtime.UserSegmentHelper
import com.tencent.mmkv.MMKV
import media.library.flys.tools.MintyStuTools.uniqueId
import media.library.manager.MintyApi
import media.library.manager.MintyCommonInter
import media.library.manager.MintyWorkQueue
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/** The common analytics outlet for the copied EnMusic push and runtime paths. */
object MintyPushAnalytics {
    private const val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss"
    private const val SAMPLE_BUCKET_KEY = "ka_firebase_sample_bucket"
    private const val DEDUPE_PREFIX = "ka_firebase_dedupe_"
    private const val LAST_SUCCESS_KEY = "ka_firebase_last_success_at"
    private const val RC_SAMPLE_RATE = "ka_firebase_sample_rate"
    private const val RC_DEDUPE_SEC = "ka_firebase_dedupe_sec"
    private val defaultsSet = AtomicBoolean(false)

    fun reportBackend(
        context: Context,
        eventType: String,
        route: String,
        reason: String,
        outcome: String,
        detail: Map<String, String> = emptyMap(),
    ) {
        val app = context.applicationContext
        val args = linkedMapOf(
            "trigger_route" to route,
            "trigger_reason" to reason,
            "outcome" to outcome,
            "failure_reason" to detail["failure_reason"].orEmpty(),
            "user_segment" to UserSegmentHelper.currentSegmentKey(),
            "notification_permission" to if (NotificationManagerCompat.from(app).areNotificationsEnabled()) "granted" else "denied",
            "started_routes" to detail["started_routes"].orEmpty(),
            "skipped_routes" to detail["skipped_routes"].orEmpty(),
            "install_time" to runCatching {
                val installed = app.packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
                SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(installed))
            }.getOrDefault(""),
            "event_time" to SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date()),
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "brand" to Build.BRAND.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
        ).apply { putAll(detail) }
        val payload = hashMapOf(
            "mji" to AppUtils.getAppName(),
            "uqn" to "Android",
            "csfq" to Build.BRAND.orEmpty(),
            "ntmy" to Build.MODEL.orEmpty(),
            "mrs" to eventType,
            "nkwol" to System.currentTimeMillis().toString(),
            "vlfhdy" to MMKV.defaultMMKV().getString(MINTY_APP_APP_REFER_INFO, "").orEmpty(),
            "sna" to Gson().toJson(args),
            "sst" to MintyApi.commonUniIds(),
            "wcww" to runCatching { uniqueId(app) }.getOrDefault(""),
            "airftw" to Build.VERSION.RELEASE.orEmpty(),
            "dgjva" to AppUtils.getAppVersionName(),
            "wuzma" to MMKV.defaultMMKV().getString("ad_id", "").orEmpty(),
        )
        runCatching { ActivityUtils.getTopActivity()?.localClassName.orEmpty() }
            .getOrNull()?.takeIf { it.isNotBlank() }?.let { payload["zewew"] = it }
        runCatching {
            MintyWorkQueue.get().upValues(payload, MintyApi.value_info,
                object : MintyCommonInter.BasicRequestListener {
                    override fun loadFailed(message: String) {
                        Timber.w("Push event %s failed: %s", eventType, message)
                    }
                    override fun loadSuccessConfig(configInfo: String) {
                        Timber.d("Push event %s response: %s", eventType, configInfo)
                    }
                })
        }.onFailure { Timber.w(it, "Push event %s could not be sent", eventType) }
    }

    fun logLpFunnel(
        context: Context,
        step: String,
        scene: String,
        bucket: String = "",
        skipReason: String = "",
        notifyId: String = "",
        router: String = "",
    ) = logEvent(context, "lp_funnel", mapOf(
        "step" to step, "scene" to scene, "bucket" to bucket,
        "skip_reason" to skipReason, "notify_id" to notifyId, "router" to router,
    ))

    fun logLpFunnel(context: Context, values: Map<String, String>) =
        logEvent(context, "lp_funnel", values)

    fun logKaOutcome(
        context: Context, route: String, reason: String, outcome: String,
        detail: Map<String, String> = emptyMap(),
    ) {
        val success = outcome == "success" || outcome.contains("成功")
        val sampleGate = success && reason in setOf(
            "手动触发", "应用回到前台", "远端配置更新补链", "快速补偿任务",
        )
        val sampled = sampleGate && !com.rapid.prominence.minty.BuildConfig.DEBUG
        if (!sampled || sampleBucket(context) < sampleRate()) {
            logEvent(context, "ka_outcome", mapOf(
                "trigger_route" to route, "trigger_reason" to reason,
                "outcome" to outcome, "sampled" to if (sampled) "yes" else "no",
                "fail_reason" to detail["failure_reason"].orEmpty(),
                "started_routes" to detail["started_routes"].orEmpty(),
                "skipped_routes" to detail["skipped_routes"].orEmpty(),
            ))
            if (success) logAliveGap(context, route, reason)
        }
        reportBackend(context, "10008", route, reason, outcome, detail)
    }

    fun logKaService(context: Context, reason: String, stage: String, outcome: String) =
        logEvent(context, "ka_service", mapOf("trigger_reason" to reason, "stage" to stage, "outcome" to outcome))

    fun logPermissionPath(context: Context, event: String) = logEvent(context, event, emptyMap())

    fun logEvent(context: Context, event: String, values: Map<String, String>) {
        runCatching {
            if (!passDedupe(context, event, values)) return
            val analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            ensureFrcDefaults()
            analytics.setUserProperty("notif_permission", if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "granted" else "denied")
            analytics.setUserProperty("oem_brand", Build.BRAND.orEmpty().take(36))
            analytics.setUserProperty("user_segment", UserSegmentHelper.currentSegmentKey())
            analytics.setUserProperty("ka_sample_bucket", sampleBucket(context).toString())
            val bundle = Bundle()
            bundle.putString("sdk_int", Build.VERSION.SDK_INT.toString())
            bundle.putString("app_ver", AppUtils.getAppVersionName().orEmpty().take(100))
            bundle.putString("user_segment", UserSegmentHelper.currentSegmentKey())
            values.forEach { (key, value) -> bundle.putString(key, value.take(100)) }
            analytics.logEvent(event, bundle)
        }.onFailure { Timber.w(it, "Firebase event %s failed", event) }
    }

    private fun logAliveGap(context: Context, route: String, reason: String) {
        val mmkv = MMKV.defaultMMKV()
        val now = System.currentTimeMillis()
        val last = mmkv.getLong(LAST_SUCCESS_KEY, 0L)
        mmkv.putLong(LAST_SUCCESS_KEY, now)
        if (last <= 0L) return
        val seconds = ((now - last) / 1000L).coerceAtLeast(0L)
        val bucket = when {
            seconds < 300 -> "under_5_min"
            seconds < 3600 -> "5_min_to_1_hour"
            seconds < 21_600 -> "1_to_6_hours"
            seconds < 86_400 -> "6_to_24_hours"
            else -> "over_24_hours"
        }
        logEvent(context, "ka_alive_gap", mapOf(
            "gap_sec" to seconds.toString(), "gap_bucket" to bucket,
            "trigger_route" to route, "trigger_reason" to reason,
        ))
    }

    private fun ensureFrcDefaults() {
        if (defaultsSet.compareAndSet(false, true)) {
            FirebaseRemoteConfig.getInstance().setDefaultsAsync(
                mapOf(RC_SAMPLE_RATE to 10L, RC_DEDUPE_SEC to 60L),
            )
        }
    }

    private fun sampleBucket(context: Context): Int {
        val mmkv = MMKV.defaultMMKV()
        if (!mmkv.containsKey(SAMPLE_BUCKET_KEY)) {
            mmkv.putInt(SAMPLE_BUCKET_KEY, ThreadLocalRandom.current().nextInt(0, 100))
        }
        return mmkv.getInt(SAMPLE_BUCKET_KEY, 0)
    }

    private fun sampleRate(): Long = FirebaseRemoteConfig.getInstance()
        .getLong(RC_SAMPLE_RATE).takeIf { it > 0L }?.coerceIn(1L, 100L) ?: 10L

    private fun passDedupe(context: Context, event: String, values: Map<String, String>): Boolean {
        if (event.startsWith("path_notif_perm_")) return true
        val key = DEDUPE_PREFIX + (event + values.toSortedMap().toString()).hashCode()
        val mmkv = MMKV.defaultMMKV()
        val now = System.currentTimeMillis()
        val last = mmkv.getLong(key, 0L)
        val seconds = FirebaseRemoteConfig.getInstance().getLong(RC_DEDUPE_SEC).takeIf { it > 0L } ?: 60L
        if (last > 0L && now - last < seconds * 1000L) return false
        mmkv.putLong(key, now)
        return true
    }
}
