package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils

/** 开机完成时尝试一次 boot_completed 场景推送。 */
class BootPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        ) {
            return
        }
        val appContext = context.applicationContext
        LogUtils.i(TAG, "收到系统广播 action=$action，恢复定点推送闹钟")
        LocalPushAlarmScheduler.restart(appContext)
        if (action != Intent.ACTION_BOOT_COMPLETED) return
        LocalEventPushHelper.ensureChannel(appContext)
        LocalEventPushHelper.show(appContext, LocalEventPushHelper.EventType.BOOT_COMPLETED)
    }

    companion object {
        private const val TAG = "BootPushReceiver"
    }
}
