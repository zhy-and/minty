package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

object LocalEventPushBootstrap {
    private const val APP_INACTIVE_CHECK_DELAY_MS = 1_500L
    private val started = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingInactiveCheck: Runnable? = null

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (!started.compareAndSet(false, true)) {
            LogUtils.i("LocalEventPush", "本地推送 Bootstrap 已启动，跳过")
            return
        }
        LogUtils.i(
            "LocalEventPush",
            "本地推送 Bootstrap 开始：FRC、渠道、前台清栏、未打开、电量/充电、网络、锁屏亮屏、" +
                "耳机蓝牙、音量、解锁、后台播放、退后台延迟、音频下载、周期/定点调度",
        )
        LocalPushRemoteConfig.refreshIfNeeded(appContext, LocalPushRemoteConfig.RefreshReason.COLD_START)
        LocalEventPushHelper.ensureChannel(appContext)
        LocalEventPushHelper.startForegroundDismissTracking(appContext)
        AppInactiveState.startTracking(appContext)
        scheduleAppInactiveCheck(appContext)
        BatteryStatusWatcher.start(appContext)
        NetworkChangeWatcher.start(appContext)
        ScreenStateWatcher.start(appContext)
        HeadsetStateWatcher.start(appContext)
        VolumeChangeWatcher.start(appContext)
        UserUnlockWatcher.start(appContext)
        PlaybackPushWatcher.start(appContext)
        AppBackgroundPushWatcher.start(appContext)
        DownloadCompleteWatcher.start(appContext)
        MediaStoreDownloadWatcher.start(appContext)
        LocalPushScheduler.start(appContext)
        LocalPushAlarmScheduler.start(appContext)
        LogUtils.i("LocalEventPush", "本地推送 Bootstrap 完成")
    }

    /** EnMusic hot-start FRC refresh; ProcessLifecycleOwner calls this from the host app. */
    fun refreshOnForeground(context: Context) {
        LocalPushRemoteConfig.refreshIfNeeded(
            context.applicationContext,
            LocalPushRemoteConfig.RefreshReason.HOT_START,
        )
    }

    private fun scheduleAppInactiveCheck(context: Context) {
        pendingInactiveCheck?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingInactiveCheck = null
            val inactiveStatus = AppInactiveState.status(context)
            if (!inactiveStatus.shouldShow) {
                LogUtils.i(
                    "LocalEventPush",
                    "app_inactive 未达到 24 小时未打开条件，跳过；${inactiveStatus.describe()}",
                )
            } else {
                LogUtils.i(
                    "LocalEventPush",
                    "app_inactive 达到条件，尝试展示；${inactiveStatus.describe()}",
                )
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.APP_INACTIVE)
            }
        }
        pendingInactiveCheck = task
        handler.postDelayed(task, APP_INACTIVE_CHECK_DELAY_MS)
    }
}
