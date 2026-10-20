package com.rapid.prominence.minty.notification.localpush

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Integrations owned by the host app; all push rules remain inside this package. */
object LocalPushUi {
    @Volatile var widgetPinnedChecker: ((Context) -> Boolean)? = null
    fun isNotificationGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasPinnedWidget(context: Context): Boolean = widgetPinnedChecker?.invoke(context) ?: false
}

object LocalPushPlaybackState {
    @Volatile var hasSong: Boolean = false
    @Volatile var isPlaying: Boolean = false
}

object LocalPushTelemetry {
    interface Listener {
        fun funnel(context: Context, values: Map<String, String>)
        fun report(context: Context, route: String, reason: String, result: String)
    }

    @Volatile var listener: Listener? = null

    fun logLpFunnel(
        context: Context, step: String, scene: String, sceneZh: String,
        bucket: String = "", skipReason: String = "", notifyId: String = "",
        router: String = "", fromHot: String = "",
    ) {
        listener?.funnel(context, mapOf(
            "step" to step, "scene" to scene, "scene_zh" to sceneZh,
            "bucket" to bucket, "skip_reason" to skipReason,
            "notify_id" to notifyId, "router" to router, "from_hot" to fromHot,
        ))
    }

    fun sceneZh(type: LocalEventPushHelper.EventType): String = when (type) {
        LocalEventPushHelper.EventType.SCREENSHOT -> "截图清理"
        LocalEventPushHelper.EventType.APP_INSTALL -> "应用安装"
        LocalEventPushHelper.EventType.APP_UNINSTALL -> "应用卸载"
        LocalEventPushHelper.EventType.BATTERY_BELOW_50 -> "电量低于50%"
        LocalEventPushHelper.EventType.BATTERY_BELOW_20 -> "电量低于20%"
        LocalEventPushHelper.EventType.CHARGING -> "充电中"
        LocalEventPushHelper.EventType.CHARGING_FULL -> "充电完成"
        LocalEventPushHelper.EventType.CHARGING_TO_80 -> "充电至80%"
        LocalEventPushHelper.EventType.CHARGING_DISCONNECT -> "断开充电"
        LocalEventPushHelper.EventType.APP_UPDATE -> "应用更新"
        LocalEventPushHelper.EventType.NETWORK_CHANGE -> "网络变化"
        LocalEventPushHelper.EventType.SCREEN_LOCK -> "锁屏"
        LocalEventPushHelper.EventType.SCREEN_ON_AFTER_LOCK -> "解锁亮屏"
        LocalEventPushHelper.EventType.STORAGE_FREE_DROP -> "存储减少3%"
        LocalEventPushHelper.EventType.SCHEDULED_PUSH_1 -> "定点推送1"
        LocalEventPushHelper.EventType.SCHEDULED_PUSH_2 -> "定点推送2-睡眠"
        LocalEventPushHelper.EventType.SCHEDULED_PUSH_3 -> "定点推送3-小组件"
        LocalEventPushHelper.EventType.APP_INACTIVE -> "未打开App"
        LocalEventPushHelper.EventType.WIRED_HEADSET_CONNECT -> "有线耳机连接"
        LocalEventPushHelper.EventType.WIRED_HEADSET_DISCONNECT -> "有线耳机断开"
        LocalEventPushHelper.EventType.BLUETOOTH_CONNECT -> "蓝牙设备连接"
        LocalEventPushHelper.EventType.BLUETOOTH_DISCONNECT -> "蓝牙设备断开"
        LocalEventPushHelper.EventType.AUDIO_FOCUS_LOST -> "音频焦点被抢占"
        LocalEventPushHelper.EventType.VOLUME_ABOVE_50 -> "音量大于50%"
        LocalEventPushHelper.EventType.VOLUME_BELOW_20 -> "音量小于20%"
        LocalEventPushHelper.EventType.USER_UNLOCK -> "用户解锁"
        LocalEventPushHelper.EventType.BOOT_COMPLETED -> "开机"
        LocalEventPushHelper.EventType.PLAYBACK_PAUSED -> "暂停续播"
        LocalEventPushHelper.EventType.PLAYBACK_BACKGROUND -> "后台播放"
        LocalEventPushHelper.EventType.APP_BACKGROUND -> "退后台"
        LocalEventPushHelper.EventType.AUDIO_DOWNLOAD -> "音频下载完成"
    }

    fun sceneZhFromTypeName(typeName: String): String =
        LocalEventPushHelper.EventType.entries.firstOrNull { it.name == typeName }
            ?.let(::sceneZh) ?: typeName.ifBlank { "未知场景" }
}

object LocalPushReporter {
    fun report(context: Context, route: String, reason: String, result: String) {
        LocalPushTelemetry.listener?.report(context, route, reason, result)
    }
}
