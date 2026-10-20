package com.rapid.prominence.minty.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.rapid.prominence.minty.analytics.MintyPushAnalytics
import java.util.concurrent.atomic.AtomicBoolean

/** Only the EnMusic FGS/FCM routes agreed for Minty; no boot, interval or delete self-heal route. */
object MintyRuntime {
    private const val TAG = "MintyRuntime"
    private const val QUICK_JOB_ID = 20_849_604
    private const val RECOVERY_COOLDOWN_MS = 60_000L
    private val bootstrapping = AtomicBoolean(false)
    private val pendingRecovery = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val topicRetryLock = Any()
    private var pendingTopicRetry: Runnable? = null
    private var topicRetryDelayMs = RuntimeConfig.FCM_TOPIC_RETRY_INITIAL_MS
    @Volatile private var lastRecoveryAtMs = 0L

    enum class Trigger(val value: String) {
        MANUAL("手动触发"), APP_FOREGROUND("应用回到前台"),
        SERVICE_DESTROY("前台服务销毁自愈"), SERVICE_TASK_REMOVED("最近任务划掉自愈"),
        FCM_HIGH("推送高优先级唤醒"), REMOTE_CONFIG("远端配置更新补链"),
        QUICK_JOB("快速补偿任务"), PERMISSION_GRANTED("通知授权通过")
    }

    @JvmStatic fun bootstrap(context: Context, reason: Trigger = Trigger.MANUAL, hasFcmHighPriority: Boolean = false) {
        val app = context.applicationContext
        RuntimeRemoteConfig.initialize(app)
        if (!bootstrapping.compareAndSet(false, true)) {
            runCatching {
                MintyPushAnalytics.logKaOutcome(app, routeFor(reason), reason.value, "失败",
                    mapOf("failure_reason" to "已有调度正在执行"))
            }.onFailure { Log.w(TAG, "Concurrent outcome report failed", it) }
            return
        }
        try {
            val startedRoutes = mutableListOf<String>()
            val skippedRoutes = mutableListOf<String>()
            var failureReason = ""
            if (RuntimeConfig.isServiceEnabled(app) && notificationPermissionGranted(app)) {
                runCatching { NotificationToolbarService.start(app, hasBackgroundExemption = hasFcmHighPriority) }
                    .onSuccess { if (it) startedRoutes += "前台服务" else skippedRoutes += "前台服务后台受限" }
                    .onFailure { failureReason = "前台服务${it.javaClass.simpleName}" }
            } else {
                skippedRoutes += if (!notificationPermissionGranted(app)) "前台服务无通知权限" else "前台服务开关关闭"
            }
            if (RuntimeConfig.isFcmEnabled(app)) {
                runCatching { ensureFcmChannel(app, reason) }
                    .onSuccess { startedRoutes += "推送通道" }
                    .onFailure { if (failureReason.isBlank()) failureReason = "推送通道${it.javaClass.simpleName}" }
            } else {
                skippedRoutes += "推送通道开关关闭"
            }
            runCatching {
                MintyPushAnalytics.logKaOutcome(app, routeFor(reason), reason.value,
                    if (failureReason.isBlank()) "成功" else "失败",
                    mapOf(
                        "failure_reason" to failureReason,
                        "started_routes" to startedRoutes.joinToString(","),
                        "skipped_routes" to skippedRoutes.joinToString(","),
                    ))
            }.onFailure { Log.w(TAG, "Outcome report failed", it) }
        } finally {
            bootstrapping.set(false)
        }
    }

    private fun routeFor(reason: Trigger): String = when (reason) {
        Trigger.SERVICE_DESTROY, Trigger.SERVICE_TASK_REMOVED, Trigger.PERMISSION_GRANTED -> "前台服务"
        Trigger.FCM_HIGH -> "推送通道"
        Trigger.APP_FOREGROUND -> "前后台切换"
        Trigger.QUICK_JOB -> "任务调度"
        Trigger.REMOTE_CONFIG -> "中台配置补链"
        Trigger.MANUAL -> "手动触发"
    }

    @JvmStatic fun onAppForeground(context: Context) {
        RuntimeRemoteConfig.refreshIfNeeded(context, hotStart = true)
        if (RuntimeConfig.isResumeEnabled(context)) bootstrap(context, Trigger.APP_FOREGROUND)
    }

    @JvmStatic fun onNotificationPermissionGranted(context: Context) {
        bootstrap(context, Trigger.PERMISSION_GRANTED)
    }

    @JvmStatic fun onFcmMessage(context: Context, message: RemoteMessage) {
        val data = message.data
        val type = data["type"].orEmpty()
        val high = FcmWakePolicy.hasForegroundServiceExemption(message.priority)
        val reportHigh = high || message.originalPriority == RemoteMessage.PRIORITY_HIGH ||
            data["google.delivered_priority"].equals("high", true)
        val enabled = RuntimeConfig.isFcmEnabled(context)
        if (reportHigh || FcmWakePolicy.isWakeType(type)) {
            Log.i(TAG, "FCM arrival: priority=${message.priority}, original=${message.originalPriority}, type=$type, enabled=$enabled")
            runCatching { MintyPushAnalytics.reportBackend(context, "10018", "fcm", "高优先级FCM到达", "received", mapOf(
                "fcm_priority" to message.priority.toString(),
                "fcm_original_priority" to message.originalPriority.toString(),
                "fcm_delivered_priority" to data["google.delivered_priority"].orEmpty(),
                "fcm_type" to type,
                "is_high_priority" to if (reportHigh) "1" else "0",
                "is_wake_message" to if (FcmWakePolicy.isWakeType(type)) "1" else "0",
                "fcm_wake_enabled" to if (enabled) "1" else "0",
            )) }.onFailure { Log.w(TAG, "FCM arrival report failed", it) }
        }
        if (FcmWakePolicy.shouldWake(message.priority, type, enabled)) {
            bootstrap(context, Trigger.FCM_HIGH, hasFcmHighPriority = high)
        }
    }

    internal fun notificationPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun onForegroundServiceLaunchRequested() { pendingRecovery.set(false) }

    internal fun onForegroundServiceDeferred(context: Context, detail: String) {
        pendingRecovery.set(true)
        Log.w(TAG, "FGS deferred: $detail")
        reportServiceStage(context, "start_foreground_deferred", detail)
        scheduleRecovery(context.applicationContext)
    }

    internal fun reportServiceStage(context: Context, stage: String, result: String) {
        Log.i(TAG, "ka_service: $stage / $result")
        runCatching { MintyPushAnalytics.logKaService(context, "前台服务启动", stage, result) }
            .onFailure { Log.w(TAG, "Service stage report failed", it) }
    }

    @SuppressLint("SpecifyJobSchedulerIdRange")
    private fun scheduleRecovery(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastRecoveryAtMs < RECOVERY_COOLDOWN_MS) return
        lastRecoveryAtMs = now
        runCatching {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val job = JobInfo.Builder(QUICK_JOB_ID, ComponentName(context, FgsRecoveryJobService::class.java))
                .setMinimumLatency(RuntimeConfig.QUICK_JOB_MIN_LATENCY_MS)
                .setOverrideDeadline(RuntimeConfig.QUICK_JOB_OVERRIDE_DEADLINE_MS)
                .build()
            scheduler.schedule(job)
        }.onFailure { Log.w(TAG, "Recovery job scheduling failed", it) }
    }

    internal fun runRecoveryJob(context: Context) {
        if (!pendingRecovery.get()) return
        if (!notificationPermissionGranted(context) || !RuntimeConfig.isServiceEnabled(context)) {
            pendingRecovery.set(false)
            reportServiceStage(context, "fgs_recovery_job", "permission_or_switch_off")
            return
        }
        val requested = NotificationToolbarService.start(context)
        reportServiceStage(context, "fgs_recovery_job", if (requested) "launch_requested" else "still_deferred")
    }

    private fun ensureFcmChannel(context: Context, reason: Trigger) {
        FirebaseMessaging.getInstance().token
            .addOnFailureListener { Log.w(TAG, "FCM token failed: ${reason.value}", it) }
        subscribeTopic(context.applicationContext, reason)
    }

    private fun subscribeTopic(context: Context, reason: Trigger) {
        FirebaseMessaging.getInstance().subscribeToTopic(RuntimeConfig.FCM_TOPIC)
            .addOnSuccessListener {
                synchronized(topicRetryLock) {
                    pendingTopicRetry?.let(handler::removeCallbacks)
                    pendingTopicRetry = null
                    topicRetryDelayMs = RuntimeConfig.FCM_TOPIC_RETRY_INITIAL_MS
                }
                Log.i(TAG, "FCM topic subscribed: ${RuntimeConfig.FCM_TOPIC}, reason=${reason.value}")
            }
            .addOnFailureListener {
                Log.w(TAG, "FCM topic subscribe failed: ${reason.value}", it)
                synchronized(topicRetryLock) {
                    if (pendingTopicRetry != null) return@addOnFailureListener
                    val delay = topicRetryDelayMs
                    topicRetryDelayMs = (delay * 2).coerceAtMost(RuntimeConfig.FCM_TOPIC_RETRY_MAX_MS)
                    val retry = Runnable {
                        synchronized(topicRetryLock) { pendingTopicRetry = null }
                        if (RuntimeConfig.isFcmEnabled(context)) subscribeTopic(context, reason)
                    }
                    pendingTopicRetry = retry
                    handler.postDelayed(retry, delay)
                }
            }
    }
}
