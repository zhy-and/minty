package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV

/**
 * 电量 / 充电相关本地推送监听（边沿触发，逻辑对齐 purgu_push）。
 */
object BatteryStatusWatcher {
    private const val TAG = "BatteryStatusWatcher"

    private const val THRESHOLD_50 = 50
    private const val THRESHOLD_20 = 20
    private const val THRESHOLD_80 = 80
    private const val THRESHOLD_FULL = 100

    private const val KEY_LAST_LEVEL = "local_push_battery_last_level"
    private const val KEY_LAST_PLUGGED = "local_push_battery_last_plugged"
    private const val KEY_80_NOTIFIED_SESSION = "local_push_battery_80_notified_session"
    private const val KEY_FULL_NOTIFIED_SESSION = "local_push_battery_full_notified_session"
    private const val UNSET_LEVEL = -1

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            handleBatteryChanged(context.applicationContext, intent, fromStickySeed = false)
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "电量监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        ensureMmkv(appContext)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        runCatching {
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "电量监听动态注册成功：ACTION_BATTERY_CHANGED")
            if (sticky != null) {
                LogUtils.i(TAG, "使用 sticky BATTERY_CHANGED 做初始种子，不触发推送")
                handleBatteryChanged(appContext, sticky, fromStickySeed = true)
            }
        }.onFailure {
            LogUtils.e(TAG, "电量监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun handleBatteryChanged(context: Context, intent: Intent, fromStickySeed: Boolean) {
        val level = readBatteryPercent(intent)
        val plugged = isPlugged(intent)
        val store = MMKV.defaultMMKV()
        val lastLevel = store.getInt(KEY_LAST_LEVEL, UNSET_LEVEL)
        val hasLastPlugged = store.contains(KEY_LAST_PLUGGED)
        val lastPlugged = store.getBoolean(KEY_LAST_PLUGGED, false)

        if (fromStickySeed || lastLevel == UNSET_LEVEL || !hasLastPlugged) {
            persistState(store, level, plugged)
            if (!plugged) {
                store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
                store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
            }
            LogUtils.i(TAG, "电量状态已种子化，本次不触发场景：level=$level plugged=$plugged")
            return
        }

        if (!lastPlugged && plugged) {
            LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING)
            store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
            store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        } else if (lastPlugged && !plugged) {
            LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING_DISCONNECT)
            store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
            store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        }

        if (lastLevel >= THRESHOLD_20 && level < THRESHOLD_20) {
            val shown20 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_20)
            if (!shown20 && lastLevel >= THRESHOLD_50 && level < THRESHOLD_50) {
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_50)
            }
        } else if (lastLevel >= THRESHOLD_50 && level < THRESHOLD_50) {
            LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_50)
        }

        val eightyNotifiedNow = store.getBoolean(KEY_80_NOTIFIED_SESSION, false)
        if (plugged && level == THRESHOLD_80 && !eightyNotifiedNow) {
            val shown80 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING_TO_80)
            if (shown80) store.putBoolean(KEY_80_NOTIFIED_SESSION, true)
        }

        val fullNotifiedNow = store.getBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        if (plugged && level >= THRESHOLD_FULL && !fullNotifiedNow) {
            val shownFull = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING_FULL)
            if (shownFull) store.putBoolean(KEY_FULL_NOTIFIED_SESSION, true)
        }

        persistState(store, level, plugged)
    }

    private fun persistState(store: MMKV, level: Int, plugged: Boolean) {
        if (level in 0..100) store.putInt(KEY_LAST_LEVEL, level)
        store.putBoolean(KEY_LAST_PLUGGED, plugged)
    }

    private fun readBatteryPercent(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return UNSET_LEVEL
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    private fun isPlugged(intent: Intent): Boolean {
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }
}
