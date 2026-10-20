package com.rapid.prominence.minty.notification.localpush

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rapid.prominence.minty.R

import com.rapid.prominence.minty.view.AppStart



import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

object LocalEventPushHelper {
    const val CHANNEL_ID = "event_recall_channel_v2"
    private const val LEGACY_CHANNEL_ID = "event_recall_channel"
    /** Ping-pong two ids per type: cancel one, post the other. */
    private const val ID_SLOT_COUNT = 2
    /** Let SystemUI finish cancel before posting a replacement. */
    private const val REPOST_DELAY_MS = 120L
    private const val MIN_CTA_TEXT_CONTRAST = 4.5
    private const val DEFAULT_CTA_BACKGROUND = 0xFF0B7CFF.toInt()
    /**
     * PendingIntent requestCode 与 notifyId 解耦。
     * notifyId 只有 2 个槽位会复用；若沿用 notifyId 作 requestCode，在
     * FLAG_IMMUTABLE + UPDATE_CURRENT 下旧 PI 的 extras 无法刷新（曾 removeExtra 后更明显），
     * 真实点击会打开「空 extras」的 LocalPushTapActivity，导致点不中「点击/打开」。
     */
    private val tapRequestCodeSeq = AtomicInteger(50_000)
    /** 截图推送文案轮询下标（下次展示用哪一条） */
    private const val KEY_SCREENSHOT_TITLE_INDEX = "lp_screenshot_title_index"
    private const val KEY_SCHEDULED_PUSH_1_TITLE_INDEX = "lp_scheduled_push_1_title_index"
    private const val KEY_SCHEDULED_PUSH_2_TITLE_INDEX = "lp_scheduled_push_2_title_index"
    private const val KEY_SCHEDULED_PUSH_3_TITLE_INDEX = "lp_scheduled_push_3_title_index"
    private const val KEY_APP_INACTIVE_TITLE_INDEX = "lp_app_inactive_title_index"
    private const val KEY_SCREEN_LOCK_TITLE_INDEX = "lp_screen_lock_title_index"
    private const val KEY_SCREEN_ON_TITLE_INDEX = "lp_screen_on_title_index"
    private const val KEY_BATTERY_BELOW_50_TITLE_INDEX = "lp_battery_below_50_title_index"
    private const val KEY_BATTERY_BELOW_20_TITLE_INDEX = "lp_battery_below_20_title_index"
    private const val KEY_CHARGING_TITLE_INDEX = "lp_charging_title_index"
    private const val KEY_CHARGING_TO_80_TITLE_INDEX = "lp_charging_to_80_title_index"
    private const val KEY_CHARGING_DISCONNECT_TITLE_INDEX = "lp_charging_disconnect_title_index"
    private const val KEY_NETWORK_CHANGE_TITLE_INDEX = "lp_network_change_title_index"
    private const val KEY_WIRED_HEADSET_CONNECT_TITLE_INDEX = "lp_wired_headset_connect_title_index"
    private const val KEY_WIRED_HEADSET_DISCONNECT_TITLE_INDEX = "lp_wired_headset_disconnect_title_index"
    private const val KEY_BLUETOOTH_CONNECT_TITLE_INDEX = "lp_bluetooth_connect_title_index"
    private const val KEY_BLUETOOTH_DISCONNECT_TITLE_INDEX = "lp_bluetooth_disconnect_title_index"
    private const val KEY_AUDIO_FOCUS_LOST_TITLE_INDEX = "lp_audio_focus_lost_title_index"
    private const val KEY_VOLUME_ABOVE_50_TITLE_INDEX = "lp_volume_above_50_title_index"
    private const val KEY_VOLUME_BELOW_20_TITLE_INDEX = "lp_volume_below_20_title_index"
    private const val KEY_USER_UNLOCK_TITLE_INDEX = "lp_user_unlock_title_index"
    private const val KEY_BOOT_COMPLETED_TITLE_INDEX = "lp_boot_completed_title_index"
    private const val KEY_PLAYBACK_PAUSED_TITLE_INDEX = "lp_playback_paused_title_index"
    private const val KEY_PLAYBACK_BACKGROUND_TITLE_INDEX = "lp_playback_background_title_index"
    private const val KEY_APP_BACKGROUND_COPY_INDEX = "lp_app_background_copy_index"
    private const val KEY_AUDIO_DOWNLOAD_TITLE_INDEX = "lp_audio_download_title_index"

    @Volatile
    private var foregroundDismissRegistered = false
    /** 回前台清栏后递增，丢弃尚未执行的延迟 notify，避免清完又贴回来。 */
    private val postGeneration = AtomicInteger(0)
    private val pendingPostById = HashMap<Int, Runnable>()
    /** 保护文案下标的“选择-提交”检查，避免过期选择把轮播位置写回旧值。 */
    private val copyIndexLock = Any()

    /**
     * 本地推送点击后挂在热启动上的待跳转。
     * 广告成功展示时 AdLoaderHub 可能直接 finish LocalPushTapActivity，不走 navigateHomeAndFinish，
     * 所以用进程级缓存，在 Hot.onDestroy 再兜底打开业务页。
     */
    @Volatile
    private var pendingDeepLinkRouter: String = ""

    @Volatile
    private var pendingDeepLinkEventType: String = ""

    @Volatile
    private var lastTrackedNotifyId: Int = -1

    @Volatile
    private var lastTrackedAtMs: Long = 0L

    private const val CLICK_DEDUPE_MS = 5_000L

    fun rememberPendingDeepLink(router: String, eventTypeName: String) {
        pendingDeepLinkRouter = router
        pendingDeepLinkEventType = eventTypeName
        LogUtils.i(
            "LocalEventPush",
            "记住热启动后待跳转：router=$router eventType=$eventTypeName",
        )
    }

    /**
     * 通知点击统一入口：Receiver / Hot 都可调用。
     * [trackClick] 对同一 notifyId 短时去重，避免 Receiver + Hot 双报。
     */
    fun onNotificationClicked(
        context: Context,
        router: String,
        eventTypeName: String,
        notifyId: Int = -1,
    ) {
        if (router.isEmpty() && eventTypeName.isEmpty()) return
        rememberPendingDeepLink(router, eventTypeName)
        trackClick(
            context = context,
            router = router,
            eventTypeName = eventTypeName,
            notifyId = notifyId,
        )
    }

    fun trackClick(
        context: Context,
        router: String,
        eventTypeName: String,
        notifyId: Int = -1,
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (notifyId >= 0 &&
            notifyId == lastTrackedNotifyId &&
            now - lastTrackedAtMs < CLICK_DEDUPE_MS
        ) {
            LogUtils.i(
                "LocalEventPush",
                "点击埋点去重：notifyId=$notifyId eventType=$eventTypeName",
            )
            return
        }
        lastTrackedNotifyId = notifyId
        lastTrackedAtMs = now
        val scene = eventTypeName.ifBlank { "unknown" }.lowercase()
        val type = EventType.entries.firstOrNull { it.name == eventTypeName }
        val sceneKey = type?.let { LocalPushConfig.sceneKey(it) } ?: scene
        val sceneZh = LocalPushTelemetry.sceneZhFromTypeName(eventTypeName)
        LocalPushTelemetry.logLpFunnel(
            context = context.applicationContext,
            step = "点击",
            scene = sceneKey,
            sceneZh = sceneZh,
            notifyId = if (notifyId >= 0) notifyId.toString() else "",
            router = router,
        )
        LocalPushReporter.report(
            context = context.applicationContext,
            route = "本地推送",
            reason = sceneZh,
            result = "点击",
        )
    }

    fun clearPendingDeepLink() {
        pendingDeepLinkRouter = ""
        pendingDeepLinkEventType = ""
    }

    /** @return true 表示确实发起了业务页跳转 */
    fun consumePendingDeepLink(context: Context): Boolean {
        val router = pendingDeepLinkRouter
        val eventType = pendingDeepLinkEventType
        if (router.isEmpty() && eventType.isEmpty()) {
            return false
        }
        clearPendingDeepLink()
        val target = buildTapTargetIntent(context, router, eventType)
        if (target == null) {
            LogUtils.e(
                "LocalEventPush",
                "消费热启动待跳转失败：router=$router eventType=$eventType",
            )
            return false
        }
        LogUtils.i(
            "LocalEventPush",
            "消费热启动待跳转并打开：router=$router eventType=$eventType " +
                "component=${target.component?.className}",
        )
        val type = EventType.entries.firstOrNull { it.name == eventType }
        val sceneKey = type?.let { LocalPushConfig.sceneKey(it) }
            ?: eventType.ifBlank { "unknown" }.lowercase()
        // 先打点再跳转：后台限制导致 startActivity 失败时也不丢「打开」
        LocalPushTelemetry.logLpFunnel(
            context = context.applicationContext,
            step = "打开",
            scene = sceneKey,
            sceneZh = LocalPushTelemetry.sceneZhFromTypeName(eventType),
            router = router,
            fromHot = "是",
        )
        return runCatching { context.startActivity(target) }.onFailure {
            LogUtils.e("LocalEventPush", "消费热启动待跳转 startActivity 失败：${it.message}")
        }.isSuccess
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val seqByType = Array(EventType.entries.size) { AtomicInteger(0) }
    private val activeIdByType = IntArray(EventType.entries.size) { -1 }
    private val activeIds = ArrayDeque<Int>()
    private val buttonPulseIds = intArrayOf(
        R.id.notif_event_action_pulse,
        R.id.notif_event_action_pulse_blue,
        R.id.notif_event_action_pulse_green,
        R.id.notif_event_action_pulse_red,
        R.id.notif_event_action_pulse_yellow,
        R.id.notif_event_action_pulse_black,
    )

    /**
     * 竞品同款：进程回到前台时清掉通知栏里的本地场景推送（不影响工具栏常驻通知）。
     */
    fun startForegroundDismissTracking(context: Context) {
        if (foregroundDismissRegistered) return
        val appContext = context.applicationContext
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                dismissAllActive(appContext, reason = "process_foreground")
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        foregroundDismissRegistered = true
        LogUtils.i("LocalEventPush", "已注册前回台清本地推送监听")
    }

    /**
     * 取消所有本地场景推送，并丢弃未执行的延迟展示。
     * 不触碰 [1901] 等其它通知。
     */
    @Synchronized
    fun dismissAllActive(context: Context, reason: String = "manual") {
        val appContext = context.applicationContext
        val generation = postGeneration.incrementAndGet()
        clearPendingPosts()
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val fromMemory = activeIds.toList()
        for (id in fromMemory) {
            manager.cancel(id)
        }
        activeIds.clear()
        activeIdByType.fill(-1)

        var fromShade = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (status in manager.activeNotifications) {
                if (status.id == 1901) continue
                val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    status.notification.channelId
                } else {
                    null
                }
                val isLocalPush = isLocalPushNotifyId(status.id) ||
                    channelId == CHANNEL_ID ||
                    channelId == LEGACY_CHANNEL_ID
                if (!isLocalPush) continue
                manager.cancel(status.id)
                fromShade += 1
            }
        }
        LogUtils.i(
            "LocalEventPush",
            "前回台/主动清本地推送：reason=$reason generation=$generation " +
                "memoryCancelled=${fromMemory.size} shadeCancelled=$fromShade ids=${fromMemory.joinToString()}",
        )
    }

    private fun clearPendingPosts() {
        for (runnable in pendingPostById.values) {
            mainHandler.removeCallbacks(runnable)
        }
        pendingPostById.clear()
    }

    private fun cancelPendingPost(notifyId: Int) {
        pendingPostById.remove(notifyId)?.let { mainHandler.removeCallbacks(it) }
    }

    private fun isLocalPushNotifyId(id: Int): Boolean {
        return EventType.entries.any { type ->
            id >= type.notificationIdBase && id < type.notificationIdBase + ID_SLOT_COUNT
        }
    }

    private enum class NotificationLayout {
        SMALL,
        REMINDER_DAY,
        BIG,
    }

    /**
     * 本地事件推送类型。
     * notificationIdBase 彼此错开，方便调试时在通知栏/log 里区分场景。
     */
    enum class EventType(
        val notificationIdBase: Int,
        val titleRes: Int,
        val actionRes: Int,
        val iconRes: Int,
    ) {
        SCREENSHOT(
            notificationIdBase = 2100,
            titleRes = R.string.notif_event_screenshot_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_screenshot,
        ),
        APP_INSTALL(
            notificationIdBase = 2200,
            titleRes = R.string.notif_event_install_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_apps,
        ),
        APP_UNINSTALL(
            notificationIdBase = 2300,
            titleRes = R.string.notif_event_uninstall_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_apps_off,
        ),
        /** 电量向下穿越 50% */
        BATTERY_BELOW_50(
            notificationIdBase = 2500,
            titleRes = R.string.notif_event_battery_below_50_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_mid,
        ),
        /** 电量向下穿越 20% */
        BATTERY_BELOW_20(
            notificationIdBase = 2510,
            titleRes = R.string.notif_event_battery_below_20_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_low,
        ),
        /** 刚插上充电器 */
        CHARGING(
            notificationIdBase = 2520,
            titleRes = R.string.notif_event_charging_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_charging,
        ),
        /** 充电中首次到 100% */
        CHARGING_FULL(
            notificationIdBase = 2530,
            titleRes = R.string.notif_event_charging_full_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_full,
        ),
        /** 充电中首次到 80% */
        CHARGING_TO_80(
            notificationIdBase = 2535,
            titleRes = R.string.notif_event_charging_to_80_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_charging,
        ),
        /** 拔掉充电器（充电断电） */
        CHARGING_DISCONNECT(
            notificationIdBase = 2540,
            titleRes = R.string.notif_event_charging_disconnect_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_battery_disconnect,
        ),
        /** 其他 App 更新完成 */
        APP_UPDATE(
            notificationIdBase = 2600,
            titleRes = R.string.notif_event_app_update_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_apps,
        ),
        /** 任意网络状态变化（去抖后） */
        NETWORK_CHANGE(
            notificationIdBase = 2700,
            titleRes = R.string.notif_event_network_change_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_network,
        ),
        /** 锁屏 / 灭屏 */
        SCREEN_LOCK(
            notificationIdBase = 2800,
            titleRes = R.string.notif_event_screen_lock_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_screen_lock,
        ),
        /** 锁屏后再次亮屏（未必已解锁） */
        SCREEN_ON_AFTER_LOCK(
            notificationIdBase = 2810,
            titleRes = R.string.notif_event_screen_on_after_lock_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_screen_on,
        ),
        /** 剩余空间相对基线下降 ≥3 个百分点 */
        STORAGE_FREE_DROP(
            notificationIdBase = 2900,
            titleRes = R.string.notif_event_storage_free_drop_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_storage,
        ),
        /** 定点推送 1：曲库/歌单运营（FRC times，每天两个时刻） */
        SCHEDULED_PUSH_1(
            notificationIdBase = 3000,
            titleRes = R.string.notif_event_scheduled_push_1_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_library,
        ),
        /** 定点推送 2：睡眠时间（FRC times，每天两个时刻） */
        SCHEDULED_PUSH_2(
            notificationIdBase = 3010,
            titleRes = R.string.notif_event_scheduled_push_2_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_sleep,
        ),
        /** 定点推送 3：小组件引导（FRC times，每天两个时刻） */
        SCHEDULED_PUSH_3(
            notificationIdBase = 3020,
            titleRes = R.string.notif_event_scheduled_push_3_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_widget,
        ),
        /** ≥24h 未打开 App；天数动态展示；5 条文案轮询 */
        APP_INACTIVE(
            notificationIdBase = 3100,
            titleRes = R.string.notif_event_app_inactive_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_note,
        ),
        /** 有线耳机插入 */
        WIRED_HEADSET_CONNECT(
            notificationIdBase = 3200,
            titleRes = R.string.notif_event_wired_headset_connect_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_headset,
        ),
        /** 有线耳机拔出 */
        WIRED_HEADSET_DISCONNECT(
            notificationIdBase = 3210,
            titleRes = R.string.notif_event_wired_headset_disconnect_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_headset_off,
        ),
        /** 蓝牙设备连接 */
        BLUETOOTH_CONNECT(
            notificationIdBase = 3220,
            titleRes = R.string.notif_event_bluetooth_connect_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_bluetooth,
        ),
        /** 蓝牙设备断开 */
        BLUETOOTH_DISCONNECT(
            notificationIdBase = 3230,
            titleRes = R.string.notif_event_bluetooth_disconnect_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_bluetooth_off,
        ),
        /** 音频焦点被临时抢占（如来电） */
        AUDIO_FOCUS_LOST(
            notificationIdBase = 3240,
            titleRes = R.string.notif_event_audio_focus_lost_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_audio_interrupt,
        ),
        /** 播放中媒体音量 > 50% */
        VOLUME_ABOVE_50(
            notificationIdBase = 3250,
            titleRes = R.string.notif_event_volume_above_50_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_volume_high,
        ),
        /** 播放中媒体音量 < 20% */
        VOLUME_BELOW_20(
            notificationIdBase = 3255,
            titleRes = R.string.notif_event_volume_below_20_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_volume_low,
        ),
        /** 用户解锁设备 */
        USER_UNLOCK(
            notificationIdBase = 3260,
            titleRes = R.string.notif_event_user_unlock_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_unlock,
        ),
        /** 开机完成 */
        BOOT_COMPLETED(
            notificationIdBase = 3270,
            titleRes = R.string.notif_event_boot_completed_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_boot,
        ),
        /** 歌曲暂停且 App 不在前台 */
        PLAYBACK_PAUSED(
            notificationIdBase = 3280,
            titleRes = R.string.notif_event_playback_paused_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_pause,
        ),
        /** 后台正在播放；3 条文案轮询 */
        PLAYBACK_BACKGROUND(
            notificationIdBase = 3290,
            titleRes = R.string.notif_event_playback_background_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_play,
        ),
        /** 退后台延迟推送；标题+正文成对轮询（对齐 pur105） */
        APP_BACKGROUND(
            notificationIdBase = 3300,
            titleRes = R.string.notif_event_app_background_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_note,
        ),
        /** 音频下载完成（DownloadManager / MediaStore Download/） */
        AUDIO_DOWNLOAD(
            notificationIdBase = 3310,
            titleRes = R.string.notif_event_audio_download_title,
            actionRes = R.string.notif_event_action_open_home,
            iconRes = R.drawable.ic_notif_download,
        ),
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LogUtils.i("LocalEventPush", "系统版本低于 Android O，不需要创建通知渠道")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        LogUtils.i(
            "LocalEventPush",
            "创建/刷新本地推送通知渠道：channelId=$CHANNEL_ID legacyChannelId=$LEGACY_CHANNEL_ID importance=HIGH sound=true vibrate=true badge=true",
        )
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_event_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_event_channel_desc)
            setShowBadge(true)
            enableVibration(true)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    @Synchronized
    fun show(context: Context, type: EventType): Boolean {
        val appContext = context.applicationContext
        val scene = LocalPushConfig.sceneKey(type)
        val sceneZh = LocalPushTelemetry.sceneZh(type)
        LogUtils.i("LocalEventPush", "收到本地推送展示请求：type=${type.name}")
        LocalPushTelemetry.logLpFunnel(
            context = appContext,
            step = "尝试",
            scene = scene,
            sceneZh = sceneZh,
        )
        if (type == EventType.STORAGE_FREE_DROP) {
            val dropStatus = StorageFreeDropState.evaluateAndCommit(appContext)
            if (!dropStatus.shouldShow) {
                LogUtils.w(
                    "LocalEventPush",
                    "跳过本地推送：type=${type.name} 未达存储下降条件；${dropStatus.describe()}",
                )
                LocalPushTelemetry.logLpFunnel(
                    context = appContext,
                    step = "跳过",
                    scene = scene,
                    sceneZh = sceneZh,
                    skipReason = "未到存储下降条件",
                )
                return false
            }
            LogUtils.i(
                "LocalEventPush",
                "存储下降已达到展示条件：type=${type.name}；${dropStatus.describe()}",
            )
        }
        if (type == EventType.APP_INACTIVE) {
            val inactiveStatus = AppInactiveState.status(appContext)
            if (!inactiveStatus.shouldShow) {
                LogUtils.w(
                    "LocalEventPush",
                    "跳过本地推送：type=${type.name} 未达未打开条件；${inactiveStatus.describe()}",
                )
                LocalPushTelemetry.logLpFunnel(
                    context = appContext,
                    step = "跳过",
                    scene = scene,
                    sceneZh = sceneZh,
                    skipReason = "未到未打开条件",
                )
                return false
            }
            LogUtils.i(
                "LocalEventPush",
                "未打开提醒已达到展示条件：type=${type.name}；${inactiveStatus.describe()}",
            )
        }
        if (type == EventType.SCHEDULED_PUSH_3 && LocalPushUi.hasPinnedWidget(appContext)) {
            LogUtils.w(
                "LocalEventPush",
                "跳过本地推送：type=${type.name} 桌面已有小组件，无需再引导 pin",
            )
            LocalPushTelemetry.logLpFunnel(
                context = appContext,
                step = "跳过",
                scene = scene,
                sceneZh = sceneZh,
                skipReason = "已添加小组件",
            )
            return false
        }
        val runtime = LocalPushConfig.sceneConfig(appContext, type)
        if (runtime == null) {
            LogUtils.w("LocalEventPush", "跳过本地推送：type=${type.name} 未找到可用本地配置")
            LocalPushTelemetry.logLpFunnel(
                context = appContext,
                step = "跳过",
                scene = scene,
                sceneZh = sceneZh,
                skipReason = "无可用配置",
            )
            return false
        }
        if (!LocalPushUi.isNotificationGranted(appContext)) {
            LogUtils.w("LocalEventPush", "跳过本地推送：type=${type.name} 没有通知权限 POST_NOTIFICATIONS")
            LocalPushTelemetry.logLpFunnel(
                context = appContext,
                step = "跳过",
                scene = runtime.sceneKey,
                sceneZh = sceneZh,
                bucket = runtime.bucketKey,
                skipReason = "无通知权限",
            )
            return false
        }
        val appInForeground = isAppInForeground()
        LogUtils.i(
            "LocalEventPush",
            "准备进入配置判断：type=${type.name} scene=${runtime.sceneKey} bucket=${runtime.bucketKey} " +
                "router=${runtime.scene.router} appInForeground=$appInForeground layout=${runtime.template.layout} " +
                "buttonBg=${runtime.template.buttonBg} buttonText=${runtime.template.buttonText} buttonAnim=${runtime.template.buttonAnim}",
        )
        if (!LocalPushConfig.canShow(appContext, runtime, appInForeground)) {
            val skipReason = if (appInForeground) "应用在前台" else "频控拦截"
            LogUtils.w(
                "LocalEventPush",
                "跳过本地推送：type=${type.name} 被配置频控拦截 scene=${runtime.sceneKey} bucket=${runtime.bucketKey}",
            )
            LocalPushTelemetry.logLpFunnel(
                context = appContext,
                step = "跳过",
                scene = runtime.sceneKey,
                sceneZh = sceneZh,
                bucket = runtime.bucketKey,
                skipReason = skipReason,
            )
            return false
        }

        ensureChannel(appContext)
        val titleSelection = selectTitle(appContext, type)
        val title = titleSelection.displayTitle
        val action = appContext.getString(type.actionRes)
        val typeIndex = type.ordinal
        val previousId = activeIdByType[typeIndex]
        val notifyId = nextNotifyId(type)
        LogUtils.i(
            "LocalEventPush",
            "生成通知ID：type=${type.name} notifyId=$notifyId previousId=$previousId typeIndex=$typeIndex",
        )
        val tapIntent = tapPendingIntent(appContext, type, runtime.scene.router, notifyId)
        val collapsed = buildCollapsedViews(appContext, type, runtime.template, title, action, tapIntent)
        val expanded = buildExpandedViews(appContext, type, runtime.template, title, action, tapIntent)
        LogUtils.i(
            "LocalEventPush",
            "通知RemoteViews构建完成：type=${type.name} title=$title action=$action layout=${runtime.template.layout} router=${runtime.scene.router}",
        )

        val now = System.currentTimeMillis()
        // 每条独立 group，避免 Android 7+ 同 App 多通知被系统自动叠成一组
        val groupKey = "lp_${type.name}_$notifyId"
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_minty)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setCustomHeadsUpContentView(expanded)
            .setContentTitle(title)
            .setContentText(action)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setWhen(now)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOnlyAlertOnce(false)
            .setGroup(groupKey)
            .build()

        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return false
        if (previousId >= 0) {
            LogUtils.i("LocalEventPush", "取消同类型上一条通知：type=${type.name} previousId=$previousId")
            cancelPendingPost(previousId)
            manager.cancel(previousId)
            activeIds.remove(previousId)
        }
        trimVisibleNotifications(manager, runtime)
        activeIdByType[typeIndex] = notifyId
        activeIds.addLast(notifyId)
        LogUtils.i(
            "LocalEventPush",
            "通知进入待展示队列：notifyId=$notifyId activeIds=${activeIds.joinToString()} repostDelayMs=$REPOST_DELAY_MS",
        )
        LocalPushConfig.markShown(appContext, runtime)

        val generation = postGeneration.get()
        cancelPendingPost(notifyId)
        val postRunnable = Runnable {
            synchronized(LocalEventPushHelper) {
                pendingPostById.remove(notifyId)
                if (generation != postGeneration.get()) {
                    LogUtils.i(
                        "LocalEventPush",
                        "丢弃延迟展示：已回前台清栏 type=${type.name} notifyId=$notifyId generation=$generation current=${postGeneration.get()}",
                    )
                    return@synchronized
                }
                if (!activeIds.contains(notifyId)) {
                    LogUtils.i(
                        "LocalEventPush",
                        "丢弃延迟展示：notifyId=$notifyId 已不在 activeIds",
                    )
                    return@synchronized
                }
                manager.notify(notifyId, notification)
                titleSelection.commit()
                LogUtils.i(
                    "LocalEventPush",
                    "本地推送已展示：type=${type.name} notifyId=$notifyId group=$groupKey previousId=$previousId " +
                        "scene=${runtime.sceneKey} bucket=${runtime.bucketKey} router=${runtime.scene.router} activeIds=${activeIds.joinToString()}",
                )
                LocalPushTelemetry.logLpFunnel(
                    context = appContext,
                    step = "展示",
                    scene = runtime.sceneKey,
                    sceneZh = sceneZh,
                    bucket = runtime.bucketKey,
                    notifyId = notifyId.toString(),
                    router = runtime.scene.router,
                )
                // 仅展示成功上报中台 10008（跳过/失败不上报）
                LocalPushReporter.report(
                    context = appContext,
                    route = "本地推送",
                    reason = sceneZh,
                    result = "成功",
                )
            }
        }
        pendingPostById[notifyId] = postRunnable
        mainHandler.postDelayed(postRunnable, REPOST_DELAY_MS)
        return true
    }

    private fun nextNotifyId(type: EventType): Int {
        val slot = seqByType[type.ordinal].getAndIncrement() % ID_SLOT_COUNT
        return type.notificationIdBase + slot
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }

    private fun trimVisibleNotifications(
        manager: NotificationManager,
        runtime: LocalPushConfig.SceneRuntime,
    ) {
        val maxVisible = runtime.bucket.maxVisible
            .takeIf { it >= 0 }
            ?.let { bucketMax ->
                runtime.globalMaxVisible.takeIf { it >= 0 }?.let { globalMax ->
                    minOf(bucketMax, globalMax)
                } ?: bucketMax
            }
            ?: return
        LogUtils.i(
            "LocalEventPush",
            "检查通知栏最大可见数：scene=${runtime.sceneKey} bucket=${runtime.bucketKey} " +
                "bucketMax=${runtime.bucket.maxVisible} globalMax=${runtime.globalMaxVisible} finalMax=$maxVisible 当前active=${activeIds.size}",
        )
        while (activeIds.size >= maxVisible && activeIds.isNotEmpty()) {
            val removed = activeIds.removeFirst()
            LogUtils.i("LocalEventPush", "超过最大可见数，取消最旧本地推送：notifyId=$removed")
            cancelPendingPost(removed)
            manager.cancel(removed)
            for (i in activeIdByType.indices) {
                if (activeIdByType[i] == removed) {
                    activeIdByType[i] = -1
                }
            }
        }
    }

    private fun buildCollapsedViews(
        context: Context,
        type: EventType,
        template: LocalPushConfig.Template,
        title: String,
        action: String,
        tapIntent: PendingIntent,
    ): RemoteViews {
        val layout = notificationLayout(template.layout, type)
        LogUtils.i(
            "LocalEventPush",
            "构建折叠态RemoteViews：type=${type.name} layout=${template.layout} resolvedLayout=$layout",
        )
        return when (layout) {
            NotificationLayout.REMINDER_DAY -> {
                val days = AppInactiveState.daysSinceLastOpen(context)
                buildCleanReminderViews(
                    context,
                    R.layout.notification_clean_reminder_collapsed,
                    template,
                    title,
                    action,
                    tapIntent,
                    days,
                )
            }
            NotificationLayout.BIG -> {
                buildBigViews(
                    context,
                    R.layout.notification_big_collapsed,
                    template,
                    title,
                    action,
                    tapIntent,
                )
            }
            NotificationLayout.SMALL -> {
                buildEventViews(
                    context,
                    R.layout.notification_event_collapsed,
                    type,
                    template,
                    title,
                    action,
                    tapIntent,
                )
            }
        }
    }

    private fun buildExpandedViews(
        context: Context,
        type: EventType,
        template: LocalPushConfig.Template,
        title: String,
        action: String,
        tapIntent: PendingIntent,
    ): RemoteViews {
        val layout = notificationLayout(template.layout, type)
        LogUtils.i(
            "LocalEventPush",
            "构建展开态RemoteViews：type=${type.name} layout=${template.layout} resolvedLayout=$layout",
        )
        return when (layout) {
            NotificationLayout.REMINDER_DAY -> {
                val days = AppInactiveState.daysSinceLastOpen(context)
                buildCleanReminderViews(
                    context,
                    R.layout.notification_clean_reminder_expanded,
                    template,
                    title,
                    action,
                    tapIntent,
                    days,
                )
            }
            NotificationLayout.BIG -> {
                buildBigViews(
                    context,
                    R.layout.notification_big_expanded,
                    template,
                    title,
                    action,
                    tapIntent,
                )
            }
            NotificationLayout.SMALL -> {
                buildEventViews(
                    context,
                    R.layout.notification_event_expanded,
                    type,
                    template,
                    title,
                    action,
                    tapIntent,
                )
            }
        }
    }

    private fun notificationLayout(rawLayout: String, type: EventType): NotificationLayout {
        return when (rawLayout.trim().lowercase(Locale.US)) {
            "small", "event" -> NotificationLayout.SMALL
            "reminder_day" -> NotificationLayout.REMINDER_DAY
            "big" -> NotificationLayout.BIG
            else -> {
                LogUtils.w(
                    "LocalEventPush",
                    "未知通知layout=$rawLayout，按type=${type.name}选择兜底布局",
                )
                if (type == EventType.APP_INACTIVE) {
                    NotificationLayout.REMINDER_DAY
                } else {
                    NotificationLayout.SMALL
                }
            }
        }
    }

    private fun buildEventViews(
        context: Context,
        layoutRes: Int,
        type: EventType,
        template: LocalPushConfig.Template,
        title: String,
        action: String,
        tapIntent: PendingIntent,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutRes).apply {
            LogUtils.i(
                "LocalEventPush",
                "绑定普通事件通知视图：layoutRes=$layoutRes type=${type.name} iconRes=${type.iconRes} " +
                    "title=$title action=$action buttonBg=${template.buttonBg} buttonText=${template.buttonText} buttonAnim=${template.buttonAnim}",
            )
            setImageViewResource(R.id.notif_event_icon, type.iconRes)
            setTextViewText(R.id.notif_event_title, title)
            setTextViewText(R.id.notif_event_action, action)
            val pulseViewId = applyButtonStyle(this, template)
            // Forces indeterminate; SystemUI starts AnimationDrawable for RemoteViews.
            startButtonPulse(this, pulseViewId)
            bindTapTargets(this, tapIntent, hasIcon = true)
        }
    }

    private fun buildCleanReminderViews(
        context: Context,
        layoutRes: Int,
        template: LocalPushConfig.Template,
        title: String,
        action: String,
        tapIntent: PendingIntent,
        days: Int,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutRes).apply {
            LogUtils.i(
                "LocalEventPush",
                "绑定天数提醒通知视图：layoutRes=$layoutRes title=$title action=$action " +
                    "days=$days buttonBg=${template.buttonBg} " +
                    "buttonText=${template.buttonText} buttonAnim=${template.buttonAnim}",
            )
            setTextViewText(R.id.notif_days_number, days.toString())
            setTextViewText(R.id.notif_days_unit, context.getString(R.string.notif_days_unit))
            setTextViewText(R.id.notif_event_title, title)
            setTextViewText(R.id.notif_event_action, action)
            val pulseViewId = applyButtonStyle(this, template)
            startButtonPulse(this, pulseViewId)
            bindTapTargets(this, tapIntent, hasIcon = false)
        }
    }

    private fun buildBigViews(
        context: Context,
        layoutRes: Int,
        template: LocalPushConfig.Template,
        title: String,
        action: String,
        tapIntent: PendingIntent,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutRes).apply {
            LogUtils.i(
                "LocalEventPush",
                "绑定大图标通知视图：layoutRes=$layoutRes title=$title action=$action " +
                    "buttonBg=${template.buttonBg} buttonText=${template.buttonText} buttonAnim=${template.buttonAnim}",
            )
            setTextViewText(R.id.notif_event_title, title)
            setTextViewText(R.id.notif_event_action, action)
            val pulseViewId = applyButtonStyle(this, template)
            startButtonPulse(this, pulseViewId)
            bindTapTargets(this, tapIntent, hasIcon = false)
        }
    }

    private fun applyButtonStyle(views: RemoteViews, template: LocalPushConfig.Template): Int {
        val parsedButtonBg = LocalPushConfig.colorOrNull(template.buttonBg)
        val buttonBg = parsedButtonBg?.takeIf { Color.alpha(it) == 255 } ?: run {
            LogUtils.w(
                "LocalEventPush",
                "按钮背景色为空、非法或含透明度，使用默认蓝色背景：raw=${template.buttonBg}",
            )
            DEFAULT_CTA_BACKGROUND
        }
        val style = NotificationCtaStylePolicy.resolve(buttonBg, template.buttonAnim)
        val activeViewId = animationViewId(style.animation)
        views.setInt(R.id.notif_event_action_wrap, "setBackgroundResource", R.drawable.bg_notif_cta_clip)
        if (style.usesTintableBackground) {
            views.setImageViewResource(R.id.notif_event_action_bg, R.drawable.bg_notif_cta_tintable)
            views.setInt(R.id.notif_event_action_bg, "setColorFilter", buttonBg)
            views.setViewVisibility(R.id.notif_event_action_bg, View.VISIBLE)
        } else if (activeViewId == View.NO_ID) {
            views.setInt(
                R.id.notif_event_action_wrap,
                "setBackgroundResource",
                presetButtonBackground(buttonBg) ?: R.drawable.bg_notif_cta_clip,
            )
            views.setViewVisibility(R.id.notif_event_action_bg, View.GONE)
        } else {
            views.setViewVisibility(R.id.notif_event_action_bg, View.GONE)
        }
        val requestedTextColor = LocalPushConfig.colorOrNull(template.buttonText)
        val buttonTextColor = resolveButtonTextColor(requestedTextColor, buttonBg)
        views.setTextColor(R.id.notif_event_action, buttonTextColor)
        LogUtils.i(
            "LocalEventPush",
            "应用按钮动画：configButtonAnim=${template.buttonAnim} buttonBg=$buttonBg " +
                "animation=${style.animation} activeViewId=$activeViewId",
        )
        return activeViewId
    }

    private fun resolveButtonTextColor(requested: Int?, background: Int): Int {
        if (requested != null &&
            Color.alpha(requested) == 255 &&
            ColorUtils.calculateContrast(requested, background) >= MIN_CTA_TEXT_CONTRAST
        ) {
            return requested
        }
        val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, background)
        val blackContrast = ColorUtils.calculateContrast(Color.BLACK, background)
        return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
    }

    private fun presetButtonBackground(color: Int): Int? {
        return when (color) {
            0xFF0B7CFF.toInt() -> R.drawable.bg_notif_cta_blue
            0xFF14B86A.toInt() -> R.drawable.bg_notif_cta_green
            0xFFFF5A3D.toInt() -> R.drawable.bg_notif_cta_red
            0xFFFFE66D.toInt() -> R.drawable.bg_notif_cta_yellow
            else -> null
        }
    }

    private fun startButtonPulse(views: RemoteViews, activeViewId: Int) {
        buttonPulseIds.forEach { viewId ->
            views.setProgressBar(viewId, 100, 0, true)
            views.setViewVisibility(viewId, if (viewId == activeViewId) View.VISIBLE else View.GONE)
        }
    }

    private fun animationViewId(animation: NotificationCtaAnimation): Int {
        return when (animation) {
            NotificationCtaAnimation.NONE -> View.NO_ID
            NotificationCtaAnimation.SCALE_BLUE -> R.id.notif_event_action_pulse_blue
            NotificationCtaAnimation.SCALE_GREEN -> R.id.notif_event_action_pulse_green
            NotificationCtaAnimation.SCALE_RED -> R.id.notif_event_action_pulse_red
            NotificationCtaAnimation.SCALE_YELLOW -> R.id.notif_event_action_pulse_yellow
            NotificationCtaAnimation.SCALE_BLACK -> R.id.notif_event_action_pulse_black
            NotificationCtaAnimation.SHIMMER -> R.id.notif_event_action_pulse
        }
    }

    /** 按钮 + 标题/图标都绑同一 PI；MIUI 上仅 contentIntent 时点空白区偶发丢 extras。 */
    private fun bindTapTargets(
        views: RemoteViews,
        tapIntent: PendingIntent,
        hasIcon: Boolean,
    ) {
        views.setOnClickPendingIntent(R.id.notif_event_action_wrap, tapIntent)
        views.setOnClickPendingIntent(R.id.notif_event_action, tapIntent)
        views.setOnClickPendingIntent(R.id.notif_event_title, tapIntent)
        if (hasIcon) {
            views.setOnClickPendingIntent(R.id.notif_event_icon, tapIntent)
        }
    }

    private fun tapPendingIntent(
        context: Context,
        type: EventType,
        router: String,
        notifyId: Int,
    ): PendingIntent {
        val requestCode = tapRequestCodeSeq.getAndIncrement()
        LogUtils.i(
            "LocalEventPush",
            "开始构建点击路由：type=${type.name} router=$router notifyId=$notifyId " +
                "requestCode=$requestCode → LocalPushTapActivity",
        )
        // 必须 getActivity：系统才会收起通知栏并把 App 拉到前台。
        // getBroadcast 再 startActivity 会把页面开在下拉栏下面，看起来像“点了没进 App”。
        // extras + data URI 双写，避免部分机型只丢 Bundle。
        val tap = Intent(context, LocalPushTapActivity::class.java).apply {
            action = LocalPushTapActivity.ACTION_LOCAL_PUSH_TAP
            data = Uri.parse(
                "minty://local_push" +
                    "?router=${Uri.encode(router)}" +
                    "&type=${Uri.encode(type.name)}" +
                    "&nid=$notifyId",
            )
            putExtra(LocalPushTapActivity.EXTRA_LOCAL_PUSH_NOTIFY_ID, notifyId)
            putExtra(LocalPushTapActivity.EXTRA_LOCAL_PUSH_ROUTER, router)
            putExtra(LocalPushTapActivity.EXTRA_LOCAL_PUSH_EVENT_TYPE, type.name)
            putExtra(LocalPushTapActivity.EXTRA_NOTIFY_ID, notifyId)
            putExtra(LocalPushTapActivity.EXTRA_ROUTER, router)
            putExtra(LocalPushTapActivity.EXTRA_EVENT_TYPE, type.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        LogUtils.i(
            "LocalEventPush",
            "点击 PendingIntent 构建完成：notifyId=$notifyId requestCode=$requestCode flags=$flags " +
                "router=$router eventType=${type.name} target=LocalPushTapActivity",
        )
        return PendingIntent.getActivity(context, requestCode, tap, flags)
    }

    /**
     * 供 [LocalPushTapActivity] 在本进程内构建显式跳转 Intent。
     */
    fun buildTapTargetIntent(context: Context, router: String, eventTypeName: String): Intent? {
        val type = EventType.entries.firstOrNull { it.name == eventTypeName }
        val normalized = router.trim().lowercase(Locale.US)
        LogUtils.i(
            "LocalEventPush",
            "解析点击目标：router=$router normalized=$normalized eventTypeName=$eventTypeName matchedType=${type?.name}",
        )
        val target = when (normalized) {
            ROUTER_PLAYER -> Intent(context, AppStart::class.java).apply {
                putExtra(LocalPushTapActivity.EXTRA_OPEN_PLAYER, true)
            }
            ROUTER_WIDGET_PIN -> Intent(context, AppStart::class.java).apply {
                putExtra(LocalPushTapActivity.EXTRA_REQUEST_WIDGET_PIN, true)
            }
            ROUTER_HOME, "" -> fallbackIntentByType(context, type)
            else -> {
                LogUtils.w("LocalEventPush", "未知 router=$router，回退首页")
                fallbackIntentByType(context, type)
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        LogUtils.i(
            "LocalEventPush",
            "点击目标构建完成：router=$normalized target=${target.component?.className} extras=${target.extras}",
        )
        return target
    }

    /** router 缺失或写错时的兜底跳转，保证调试时点通知不会落到空白页 */
    private fun fallbackIntentByType(context: Context, type: EventType?): Intent {
        LogUtils.i("LocalEventPush", "本地推送兜底打开 AppStart：type=${type?.name}")
        return Intent(context, AppStart::class.java)
    }

    const val ROUTER_HOME = "home"
    const val ROUTER_PLAYER = "player"
    const val ROUTER_WIDGET_PIN = "widget_pin"

    /** 测试/预览入口：立即消费一次文案；真实通知使用 [selectTitle] 延迟到展示成功再提交。 */
    private fun resolveTitle(context: Context, type: EventType): String {
        val selection = selectTitle(context, type)
        selection.commit()
        return selection.displayTitle
    }

    private fun selectTitle(context: Context, type: EventType): TitleSelection {
        return when (type) {
            EventType.SCREENSHOT -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_screenshot_titles,
                indexKey = KEY_SCREENSHOT_TITLE_INDEX,
                fallbackRes = EventType.SCREENSHOT.titleRes,
                logLabel = "截图",
            )
            EventType.SCHEDULED_PUSH_1 -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_scheduled_push_1_titles,
                indexKey = KEY_SCHEDULED_PUSH_1_TITLE_INDEX,
                fallbackRes = EventType.SCHEDULED_PUSH_1.titleRes,
                logLabel = "定点推送1",
            )
            EventType.SCHEDULED_PUSH_2 -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_scheduled_push_2_titles,
                indexKey = KEY_SCHEDULED_PUSH_2_TITLE_INDEX,
                fallbackRes = EventType.SCHEDULED_PUSH_2.titleRes,
                logLabel = "定点推送2-睡眠",
            )
            EventType.SCHEDULED_PUSH_3 -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_scheduled_push_3_titles,
                indexKey = KEY_SCHEDULED_PUSH_3_TITLE_INDEX,
                fallbackRes = EventType.SCHEDULED_PUSH_3.titleRes,
                logLabel = "定点推送3-小组件",
            )
            EventType.BATTERY_BELOW_50 -> nextRotatingTitle(
                context, R.array.notif_event_battery_below_50_titles,
                KEY_BATTERY_BELOW_50_TITLE_INDEX, type.titleRes, "电量低于50%",
            )
            EventType.BATTERY_BELOW_20 -> nextRotatingTitle(
                context, R.array.notif_event_battery_below_20_titles,
                KEY_BATTERY_BELOW_20_TITLE_INDEX, type.titleRes, "电量低于20%",
            )
            EventType.CHARGING -> nextRotatingTitle(
                context, R.array.notif_event_charging_titles,
                KEY_CHARGING_TITLE_INDEX, type.titleRes, "连接充电器",
            )
            EventType.CHARGING_TO_80 -> nextRotatingTitle(
                context, R.array.notif_event_charging_to_80_titles,
                KEY_CHARGING_TO_80_TITLE_INDEX, type.titleRes, "充电至80%",
            )
            EventType.CHARGING_DISCONNECT -> nextRotatingTitle(
                context, R.array.notif_event_charging_disconnect_titles,
                KEY_CHARGING_DISCONNECT_TITLE_INDEX, type.titleRes, "断开充电器",
            )
            EventType.NETWORK_CHANGE -> nextRotatingTitle(
                context, R.array.notif_event_network_change_titles,
                KEY_NETWORK_CHANGE_TITLE_INDEX, type.titleRes, "网络变化",
            )
            EventType.SCREEN_LOCK -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_screen_lock_titles,
                indexKey = KEY_SCREEN_LOCK_TITLE_INDEX,
                fallbackRes = EventType.SCREEN_LOCK.titleRes,
                logLabel = "熄屏",
            )
            EventType.SCREEN_ON_AFTER_LOCK -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_screen_on_after_lock_titles,
                indexKey = KEY_SCREEN_ON_TITLE_INDEX,
                fallbackRes = EventType.SCREEN_ON_AFTER_LOCK.titleRes,
                logLabel = "亮屏",
            )
            EventType.APP_INACTIVE -> {
                val days = AppInactiveState.daysSinceLastOpen(context)
                val rawSelection = nextRotatingTitle(
                    context = context,
                    arrayRes = R.array.notif_event_app_inactive_titles,
                    indexKey = KEY_APP_INACTIVE_TITLE_INDEX,
                    fallbackRes = EventType.APP_INACTIVE.titleRes,
                    logLabel = "未打开",
                )
                TitleSelection(formatTitleWithDays(rawSelection.displayTitle, days)) {
                    rawSelection.commit()
                }
            }
            EventType.WIRED_HEADSET_CONNECT -> nextRotatingTitle(
                context, R.array.notif_event_wired_headset_connect_titles,
                KEY_WIRED_HEADSET_CONNECT_TITLE_INDEX, type.titleRes, "有线耳机连接",
            )
            EventType.WIRED_HEADSET_DISCONNECT -> nextRotatingTitle(
                context, R.array.notif_event_wired_headset_disconnect_titles,
                KEY_WIRED_HEADSET_DISCONNECT_TITLE_INDEX, type.titleRes, "有线耳机断开",
            )
            EventType.BLUETOOTH_CONNECT -> nextRotatingTitle(
                context, R.array.notif_event_bluetooth_connect_titles,
                KEY_BLUETOOTH_CONNECT_TITLE_INDEX, type.titleRes, "蓝牙连接",
            )
            EventType.BLUETOOTH_DISCONNECT -> nextRotatingTitle(
                context, R.array.notif_event_bluetooth_disconnect_titles,
                KEY_BLUETOOTH_DISCONNECT_TITLE_INDEX, type.titleRes, "蓝牙断开",
            )
            EventType.AUDIO_FOCUS_LOST -> nextRotatingTitle(
                context, R.array.notif_event_audio_focus_lost_titles,
                KEY_AUDIO_FOCUS_LOST_TITLE_INDEX, type.titleRes, "音频焦点丢失",
            )
            EventType.VOLUME_ABOVE_50 -> nextRotatingTitle(
                context, R.array.notif_event_volume_above_50_titles,
                KEY_VOLUME_ABOVE_50_TITLE_INDEX, type.titleRes, "音量高于50%",
            )
            EventType.VOLUME_BELOW_20 -> nextRotatingTitle(
                context, R.array.notif_event_volume_below_20_titles,
                KEY_VOLUME_BELOW_20_TITLE_INDEX, type.titleRes, "音量低于20%",
            )
            EventType.USER_UNLOCK -> nextRotatingTitle(
                context, R.array.notif_event_user_unlock_titles,
                KEY_USER_UNLOCK_TITLE_INDEX, type.titleRes, "用户解锁",
            )
            EventType.BOOT_COMPLETED -> nextRotatingTitle(
                context, R.array.notif_event_boot_completed_titles,
                KEY_BOOT_COMPLETED_TITLE_INDEX, type.titleRes, "开机完成",
            )
            EventType.PLAYBACK_PAUSED -> nextRotatingTitle(
                context, R.array.notif_event_playback_paused_titles,
                KEY_PLAYBACK_PAUSED_TITLE_INDEX, type.titleRes, "暂停续播",
            )
            EventType.PLAYBACK_BACKGROUND -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_playback_background_titles,
                indexKey = KEY_PLAYBACK_BACKGROUND_TITLE_INDEX,
                fallbackRes = EventType.PLAYBACK_BACKGROUND.titleRes,
                logLabel = "后台播放",
            )
            EventType.APP_BACKGROUND -> {
                nextRotatingTitleContent(
                    context = context,
                    titleArrayRes = R.array.notif_event_app_background_titles,
                    contentArrayRes = R.array.notif_event_app_background_contents,
                    indexKey = KEY_APP_BACKGROUND_COPY_INDEX,
                    fallbackTitleRes = EventType.APP_BACKGROUND.titleRes,
                    fallbackContentRes = R.string.notif_event_app_background_content,
                    logLabel = "退后台",
                )
            }
            EventType.AUDIO_DOWNLOAD -> nextRotatingTitle(
                context = context,
                arrayRes = R.array.notif_event_audio_download_titles,
                indexKey = KEY_AUDIO_DOWNLOAD_TITLE_INDEX,
                fallbackRes = EventType.AUDIO_DOWNLOAD.titleRes,
                logLabel = "音频下载",
            )
            else -> TitleSelection(context.getString(type.titleRes))
        }
    }

    private class TitleSelection(
        val displayTitle: String,
        private val onCommit: () -> Unit = {},
    ) {
        private var committed = false

        @Synchronized
        fun commit() {
            if (committed) return
            committed = true
            onCommit()
        }
    }

    private data class PushCopy(
        val title: String,
        val content: String = "",
    ) {
        val displayTitle: String
            get() = if (content.isBlank()) title else "$title\n$content"
    }

    private fun nextRotatingTitleContent(
        context: Context,
        titleArrayRes: Int,
        contentArrayRes: Int,
        indexKey: String,
        fallbackTitleRes: Int,
        fallbackContentRes: Int,
        logLabel: String,
    ): TitleSelection {
        val titles = context.resources.getStringArray(titleArrayRes)
        val contents = context.resources.getStringArray(contentArrayRes)
        val size = minOf(titles.size, contents.size)
        if (size <= 0) {
            return TitleSelection(
                PushCopy(
                    title = context.getString(fallbackTitleRes),
                    content = context.getString(fallbackContentRes),
                ).displayTitle,
            )
        }
        if (titles.size != contents.size) {
            LogUtils.w(
                "LocalEventPush",
                "${logLabel}标题数=${titles.size} 与正文数=${contents.size} 不一致，按 min=$size 轮询",
            )
        }
        ensureMmkv(context)
        val store = MMKV.defaultMMKV()
        val index = synchronized(copyIndexLock) {
            store.getInt(indexKey, 0).let { ((it % size) + size) % size }
        }
        val title = titles[index]
        val content = contents[index]
        val next = (index + 1) % size
        LogUtils.i(
            "LocalEventPush",
            "${logLabel}文案对已选择：index=$index next=$next title=$title content=$content poolSize=$size",
        )
        return TitleSelection(PushCopy(title = title, content = content).displayTitle) onCommit@{
            synchronized(copyIndexLock) {
                val current = store.getInt(indexKey, 0).let { ((it % size) + size) % size }
                if (current != index) {
                    LogUtils.i(
                        "LocalEventPush",
                        "${logLabel}文案对提交已过期，忽略：selected=$index current=$current poolSize=$size",
                    )
                    return@onCommit
                }
                store.putInt(indexKey, next)
                LogUtils.i(
                    "LocalEventPush",
                    "${logLabel}文案对展示成功后推进：index=$index next=$next poolSize=$size",
                )
            }
        }
    }

    /** 文案含 `%1$d` 时填入天数；不含格式符则原样返回。 */
    private fun formatTitleWithDays(template: String, days: Int): String {
        return runCatching { String.format(Locale.getDefault(), template, days) }
            .getOrDefault(template)
    }

    /** 多文案轮询：0→1→…→N-1→0。仅真正展示时推进下标。 */
    private fun nextRotatingTitle(
        context: Context,
        arrayRes: Int,
        indexKey: String,
        fallbackRes: Int,
        logLabel: String,
    ): TitleSelection {
        val titles = context.resources.getStringArray(arrayRes)
        if (titles.isEmpty()) {
            LogUtils.w("LocalEventPush", "${logLabel}文案数组为空，回退到单一 titleRes")
            return TitleSelection(context.getString(fallbackRes))
        }
        ensureMmkv(context)
        val store = MMKV.defaultMMKV()
        val size = titles.size
        val index = synchronized(copyIndexLock) {
            val raw = store.getInt(indexKey, 0)
            ((raw % size) + size) % size
        }
        val title = titles[index]
        val next = (index + 1) % size
        LogUtils.i(
            "LocalEventPush",
            "${logLabel}文案已选择：index=$index next=$next title=$title poolSize=$size",
        )
        return TitleSelection(title) onCommit@{
            synchronized(copyIndexLock) {
                val current = store.getInt(indexKey, 0).let { ((it % size) + size) % size }
                if (current != index) {
                    LogUtils.i(
                        "LocalEventPush",
                        "${logLabel}文案提交已过期，忽略：selected=$index current=$current poolSize=$size",
                    )
                    return@onCommit
                }
                store.putInt(indexKey, next)
                LogUtils.i(
                    "LocalEventPush",
                    "${logLabel}文案展示成功后推进：index=$index next=$next poolSize=$size",
                )
            }
        }
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }
}
