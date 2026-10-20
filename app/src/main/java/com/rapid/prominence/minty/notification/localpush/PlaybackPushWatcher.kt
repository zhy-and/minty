package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.LogUtils


/**
 * 后台播放相关本地推送：
 * - 有曲目且暂停、App 不在前台 → [LocalEventPushHelper.EventType.PLAYBACK_PAUSED]
 * - 正在播放、App 不在前台 → [LocalEventPushHelper.EventType.PLAYBACK_BACKGROUND]
 */
object PlaybackPushWatcher {
    private const val TAG = "PlaybackPushWatcher"
    private const val DEBOUNCE_MS = 1_500L

    @Volatile
    private var started = false

    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEvaluate: Runnable? = null
    private var pendingShow: Runnable? = null

    /** Call from Minty's player listener when song or playback state changes. */
    fun onPlaybackChanged(context: Context, hasSong: Boolean, isPlaying: Boolean) {
        LocalPushPlaybackState.hasSong = hasSong
        LocalPushPlaybackState.isPlaying = isPlaying
        if (!started) start(context)
        scheduleEvaluate("playback_state playing=$isPlaying hasSong=$hasSong")
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            scheduleEvaluate("process_on_stop")
        }

        override fun onStart(owner: LifecycleOwner) {
            cancelPending("process_foreground")
        }
    }

    fun start(context: Context) {
        if (started) {
            LogUtils.i(TAG, "播放状态推送监听已启动，跳过重复 start")
            return
        }
        started = true
        appContext = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        LogUtils.i(TAG, "播放状态推送监听已启动")
    }

    private fun scheduleEvaluate(reason: String) {
        pendingEvaluate?.let { handler.removeCallbacks(it) }
        pendingShow?.let { handler.removeCallbacks(it) }
        pendingShow = null
        val task = Runnable {
            pendingEvaluate = null
            evaluateAndMaybeShow(reason)
        }
        pendingEvaluate = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun evaluateAndMaybeShow(reason: String) {
        val context = appContext ?: return
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        if (foreground) {
            LogUtils.i(TAG, "App 在前台，跳过播放推送 reason=$reason")
            return
        }
        if (!LocalPushPlaybackState.hasSong) {
            LogUtils.i(TAG, "无当前曲目，跳过播放推送 reason=$reason")
            return
        }
        val type = if (LocalPushPlaybackState.isPlaying) {
            LocalEventPushHelper.EventType.PLAYBACK_BACKGROUND
        } else {
            LocalEventPushHelper.EventType.PLAYBACK_PAUSED
        }
        val sceneKey = LocalPushConfig.sceneKey(type)
        val scene = LocalPushConfig.currentConfig().scenes[sceneKey]
        if (scene == null || !scene.enabled) {
            LogUtils.i(TAG, "播放场景未配置或已关闭，跳过调度：scene=$sceneKey enabled=${scene?.enabled}")
            return
        }
        val delaySec = PlaybackPushDelayPolicy.resolveDelaySec(scene.delaySec)
        LogUtils.i(TAG, "后台满足 $sceneKey 条件，${delaySec}s 后尝试推送 reason=$reason")
        val task = Runnable {
            pendingShow = null
            if (!isPlaybackPushStillEligible(type)) {
                LogUtils.i(TAG, "播放推送延迟到达时条件已失效，跳过 scene=$sceneKey")
                return@Runnable
            }
            LogUtils.i(TAG, "延迟到达，尝试 $sceneKey reason=$reason")
            LocalEventPushHelper.show(context, type)
        }
        pendingShow = task
        handler.postDelayed(task, delaySec * 1000L)
    }

    private fun isPlaybackPushStillEligible(type: LocalEventPushHelper.EventType): Boolean {
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        val expectedPlaying = type == LocalEventPushHelper.EventType.PLAYBACK_BACKGROUND
        return !foreground && LocalPushPlaybackState.hasSong &&
            LocalPushPlaybackState.isPlaying == expectedPlaying
    }

    private fun cancelPending(reason: String) {
        pendingEvaluate?.let { handler.removeCallbacks(it) }
        pendingEvaluate = null
        pendingShow?.let { handler.removeCallbacks(it) }
        pendingShow = null
        LogUtils.i(TAG, "取消播放推送待处理任务：reason=$reason")
    }
}
