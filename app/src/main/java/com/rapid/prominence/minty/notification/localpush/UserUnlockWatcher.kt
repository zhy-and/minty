package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.blankj.utilcode.util.LogUtils

/** 用户解锁设备（ACTION_USER_PRESENT）本地推送。 */
object UserUnlockWatcher {
    private const val TAG = "UserUnlockWatcher"

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT) return
            LogUtils.i(TAG, "收到 USER_PRESENT，尝试 user_unlock")
            LocalEventPushHelper.show(context.applicationContext, LocalEventPushHelper.EventType.USER_UNLOCK)
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "用户解锁监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "用户解锁监听注册成功：USER_PRESENT")
        }.onFailure {
            LogUtils.e(TAG, "用户解锁监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }
}
