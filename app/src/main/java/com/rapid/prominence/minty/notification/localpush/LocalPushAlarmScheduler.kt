package com.rapid.prominence.minty.notification.localpush

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import java.util.Calendar
import java.util.Locale

/**
 * 定点本地推送闹钟触发。
 *
 * 调试：
 * adb shell am broadcast -a com.rapid.prominence.minty.action.LOCAL_PUSH_SCHEDULED \
 *   --es scene_key scheduled_push_1 --ei time_index 0 -p <package>
 */
class LocalPushAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_LOCAL_PUSH_SCHEDULED) {
            LogUtils.w(TAG, "忽略非定点推送 action=${intent?.action}")
            return
        }
        val sceneKey = intent.getStringExtra(EXTRA_SCENE_KEY).orEmpty()
        val timeIndex = intent.getIntExtra(EXTRA_TIME_INDEX, -1)
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL).orEmpty()
        LogUtils.i(TAG, "收到定点闹钟：scene=$sceneKey timeIndex=$timeIndex time=$timeLabel")

        val type = LocalPushConfig.eventTypeForSceneKey(sceneKey)
        if (type == null) {
            LogUtils.w(TAG, "未知 sceneKey=$sceneKey，无法映射 EventType，跳过展示")
        } else {
            val shown = LocalEventPushHelper.show(context, type)
            LogUtils.i(TAG, "定点推送展示结果：scene=$sceneKey type=${type.name} shown=$shown")
        }

        // 无论是否展示成功，都排下一次同刻闹钟（明天）
        LocalPushAlarmScheduler.scheduleNextForSlot(
            context = context.applicationContext,
            sceneKey = sceneKey,
            timeIndex = timeIndex,
            timeLabel = timeLabel,
        )
    }

    companion object {
        const val ACTION_LOCAL_PUSH_SCHEDULED =
            "com.rapid.prominence.minty.action.LOCAL_PUSH_SCHEDULED"
        const val EXTRA_SCENE_KEY = "scene_key"
        const val EXTRA_TIME_INDEX = "time_index"
        const val EXTRA_TIME_LABEL = "time_label"
        private const val TAG = "LocalPushAlarm"
    }
}

/**
 * 按 FRC/本地 JSON 的 scene.times 排本地闹钟；配置变更时 [restart] 全量重排。
 */
object LocalPushAlarmScheduler {
    private const val TAG = "LocalPushAlarmSched"
    private const val REQUEST_CODE_BASE = 4100

    fun start(context: Context) {
        rescheduleAll(context.applicationContext, reason = "start")
    }

    fun restart(context: Context) {
        rescheduleAll(context.applicationContext, reason = "restart")
    }

    fun scheduleNextForSlot(
        context: Context,
        sceneKey: String,
        timeIndex: Int,
        timeLabel: String,
    ) {
        if (sceneKey.isBlank() || timeIndex < 0 || timeLabel.isBlank()) {
            LogUtils.w(TAG, "补排下一次失败：参数无效 scene=$sceneKey index=$timeIndex time=$timeLabel")
            return
        }
        val appContext = context.applicationContext
        val scene = LocalPushConfig.currentConfig().scenes[sceneKey]
        if (scene == null || !scene.enabled || !LocalPushConfig.currentConfig().enabled) {
            LogUtils.i(TAG, "场景已关闭，不再补排：scene=$sceneKey")
            cancelSlot(appContext, sceneKey, timeIndex)
            return
        }
        val triggerAt = nextTriggerMillis(timeLabel, skipIfWithinMs = 60_000L)
        if (triggerAt == null) {
            LogUtils.w(TAG, "时刻解析失败，无法补排：scene=$sceneKey time=$timeLabel")
            return
        }
        scheduleExact(appContext, sceneKey, timeIndex, timeLabel, triggerAt)
    }

    private fun rescheduleAll(context: Context, reason: String) {
        val config = LocalPushConfig.currentConfig()
        LogUtils.i(
            TAG,
            "开始重排定点闹钟：reason=$reason enabled=${config.enabled} " +
                "scenes=${config.scenes.filter { it.value.times.isNotEmpty() }.keys}",
        )
        // 先取消已知定点场景的旧闹钟（按最大 8 个时刻槽位兜底）
        listOf("scheduled_push_1", "scheduled_push_2", "scheduled_push_3").forEach { key ->
            repeat(8) { idx -> cancelSlot(context, key, idx) }
        }

        if (!config.enabled) {
            LogUtils.i(TAG, "总开关关闭，已取消定点闹钟 reason=$reason")
            return
        }

        config.scenes.forEach { (sceneKey, scene) ->
            if (!scene.enabled || scene.times.isEmpty()) return@forEach
            scene.times.forEachIndexed { index, raw ->
                val timeLabel = raw.trim()
                val triggerAt = nextTriggerMillis(timeLabel, skipIfWithinMs = 5_000L)
                if (triggerAt == null) {
                    LogUtils.w(TAG, "跳过非法时刻：scene=$sceneKey index=$index raw=$raw")
                    return@forEachIndexed
                }
                scheduleExact(context, sceneKey, index, timeLabel, triggerAt)
            }
        }
    }

    private fun scheduleExact(
        context: Context,
        sceneKey: String,
        timeIndex: Int,
        timeLabel: String,
        triggerAtMillis: Long,
    ) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (alarm == null) {
            LogUtils.e(TAG, "AlarmManager 不可用，无法排程 scene=$sceneKey")
            return
        }
        val pending = pendingIntent(context, sceneKey, timeIndex, timeLabel)
        runCatching {
            alarm.cancel(pending)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarm.canScheduleExactAlarms()
                    if (canExact) {
                        alarm.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pending,
                        )
                        LogUtils.i(
                            TAG,
                            "已排精确闹钟：scene=$sceneKey time=$timeLabel index=$timeIndex " +
                                "triggerAt=$triggerAtMillis mode=exactAllowWhileIdle",
                        )
                    } else {
                        alarm.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pending,
                        )
                        LogUtils.w(
                            TAG,
                            "无精确闹钟权限，降级 inexact：scene=$sceneKey time=$timeLabel " +
                                "triggerAt=$triggerAtMillis mode=allowWhileIdle",
                        )
                    }
                }
                else -> {
                    @Suppress("DEPRECATION")
                    alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                    LogUtils.i(
                        TAG,
                        "已排精确闹钟(legacy)：scene=$sceneKey time=$timeLabel index=$timeIndex " +
                            "triggerAt=$triggerAtMillis",
                    )
                }
            }
        }.onFailure {
            LogUtils.e(
                TAG,
                "排程失败：scene=$sceneKey time=$timeLabel ${it.javaClass.simpleName} ${it.message.orEmpty()}",
            )
        }
    }

    private fun cancelSlot(context: Context, sceneKey: String, timeIndex: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, sceneKey, timeIndex, timeLabel = "")
        runCatching { alarm.cancel(pending) }
    }

    private fun pendingIntent(
        context: Context,
        sceneKey: String,
        timeIndex: Int,
        timeLabel: String,
    ): PendingIntent {
        val intent = Intent(context, LocalPushAlarmReceiver::class.java).apply {
            action = LocalPushAlarmReceiver.ACTION_LOCAL_PUSH_SCHEDULED
            setPackage(context.packageName)
            putExtra(LocalPushAlarmReceiver.EXTRA_SCENE_KEY, sceneKey)
            putExtra(LocalPushAlarmReceiver.EXTRA_TIME_INDEX, timeIndex)
            putExtra(LocalPushAlarmReceiver.EXTRA_TIME_LABEL, timeLabel)
        }
        val sceneOrdinal = when (sceneKey) {
            "scheduled_push_1" -> 0
            "scheduled_push_2" -> 1
            "scheduled_push_3" -> 2
            else -> (kotlin.math.abs(sceneKey.hashCode()) % 50) + 3
        }
        val requestCode = REQUEST_CODE_BASE + sceneOrdinal * 16 + (timeIndex and 0xF)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * @param skipIfWithinMs 若算出的触发点距现在过近，则改推到明天同刻（避免刚排完立刻触发）
     */
    private fun nextTriggerMillis(timeLabel: String, skipIfWithinMs: Long): Long? {
        val parts = timeLabel.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delta = target.timeInMillis - now.timeInMillis
        if (delta <= skipIfWithinMs) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        LogUtils.i(
            TAG,
            "计算下次触发：time=${String.format(Locale.US, "%02d:%02d", hour, minute)} " +
                "triggerAt=${target.timeInMillis} inMs=${target.timeInMillis - now.timeInMillis}",
        )
        return target.timeInMillis
    }
}
