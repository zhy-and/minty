package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.LogUtils

/**
 * 退后台延迟本地推送，对齐 pur105 [APP_BACKGROUND]：
 * - 进程 onStop 后按 scene.delay_sec（默认 5s）尝试展示
 * - 回前台取消未发出任务
 * - 频控走场景体系
 *
 * 替换 Minty 里原先硬编码 5s 的 MMKV 文案推送。
 */
object AppBackgroundPushWatcher {
    private const val TAG = "AppBackgroundPushWatcher"
    private const val DEFAULT_APP_BACKGROUND_DELAY_SEC = 5

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private val pendingByScene = linkedMapOf<String, Runnable>()

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "退后台推送监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                scheduleAll(appContext)
            }

            override fun onStart(owner: LifecycleOwner) {
                cancelAll("process_foreground")
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        registered = true
        LogUtils.i(TAG, "退后台推送监听注册成功：ProcessLifecycle onStop/onStart")
    }

    private fun scheduleAll(appContext: Context) {
        cancelAll("onStop 重新计时")
        if (!LocalPushConfig.currentConfig().enabled) {
            LogUtils.i(TAG, "本地推送总开关关闭，跳过退后台调度")
            return
        }
        scheduleScene(
            appContext = appContext,
            type = LocalEventPushHelper.EventType.APP_BACKGROUND,
            defaultDelaySec = DEFAULT_APP_BACKGROUND_DELAY_SEC,
        )
    }

    private fun scheduleScene(
        appContext: Context,
        type: LocalEventPushHelper.EventType,
        defaultDelaySec: Int,
    ) {
        val sceneKey = LocalPushConfig.sceneKey(type)
        val scene = LocalPushConfig.currentConfig().scenes[sceneKey]
        if (scene == null || !scene.enabled) {
            LogUtils.i(TAG, "场景未配置或已关闭，跳过调度：scene=$sceneKey enabled=${scene?.enabled}")
            return
        }
        val delaySec = scene.delaySec.takeIf { it >= 0 } ?: defaultDelaySec
        val delayMs = delaySec * 1000L
        LogUtils.i(TAG, "进程退后台，${delaySec}s 后尝试 $sceneKey")
        val task = Runnable {
            pendingByScene.remove(sceneKey)
            LogUtils.i(TAG, "延迟到达，尝试展示 $sceneKey")
            val shown = LocalEventPushHelper.show(appContext, type)
            LogUtils.i(TAG, "$sceneKey 场景尝试结果：shown=$shown")
        }
        pendingByScene[sceneKey] = task
        handler.postDelayed(task, delayMs)
    }

    private fun cancelAll(reason: String) {
        if (pendingByScene.isEmpty()) return
        val keys = pendingByScene.keys.toList()
        pendingByScene.values.forEach { handler.removeCallbacks(it) }
        pendingByScene.clear()
        LogUtils.i(TAG, "取消未发出的退后台推送：reason=$reason scenes=${keys.joinToString()}")
    }
}
