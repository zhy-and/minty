package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.blankj.utilcode.util.LogUtils

/** 锁屏 / 亮屏本地推送监听（逻辑对齐 purgu_push）。 */
object ScreenStateWatcher {
    private const val TAG = "ScreenStateWatcher"

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            val appContext = context.applicationContext
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    LocalEventPushHelper.show(appContext, LocalEventPushHelper.EventType.SCREEN_LOCK)
                }
                Intent.ACTION_SCREEN_ON -> {
                    LocalEventPushHelper.show(appContext, LocalEventPushHelper.EventType.SCREEN_ON_AFTER_LOCK)
                }
            }
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "屏幕状态监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "屏幕状态监听注册成功：SCREEN_OFF / SCREEN_ON")
        }.onFailure {
            LogUtils.e(TAG, "屏幕状态监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }
}
