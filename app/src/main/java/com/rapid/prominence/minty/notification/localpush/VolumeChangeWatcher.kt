package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils


/**
 * 播放中监测媒体音量：
 * - 音量 &gt; 50% → [LocalEventPushHelper.EventType.VOLUME_ABOVE_50]
 * - 音量 &lt; 20% → [LocalEventPushHelper.EventType.VOLUME_BELOW_20]
 */
object VolumeChangeWatcher {
    private const val TAG = "VolumeChangeWatcher"
    private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    private const val DEBOUNCE_MS = 1_200L

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingNotify: Runnable? = null

    @Volatile
    private var lastMusicVolume: Int? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != VOLUME_CHANGED_ACTION) return
            val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
            if (streamType != AudioManager.STREAM_MUSIC) return
            scheduleEvaluate(context.applicationContext)
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "音量监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        val am = appContext.getSystemService(AudioManager::class.java)
        lastMusicVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC)
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "音量监听注册成功 seedVolume=$lastMusicVolume")
        }.onFailure {
            LogUtils.e(TAG, "音量监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun scheduleEvaluate(context: Context) {
        pendingNotify?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingNotify = null
            evaluateAndMaybeShow(context)
        }
        pendingNotify = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun evaluateAndMaybeShow(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val previous = lastMusicVolume
        if (previous != null && current == previous) {
            LogUtils.i(TAG, "音量未变化，跳过：volume=$current")
            return
        }
        lastMusicVolume = current
        if (previous == null) {
            LogUtils.i(TAG, "音量种子化，不推送：volume=$current")
            return
        }
        if (!LocalPushPlaybackState.isPlaying) {
            LogUtils.i(TAG, "未在播放，跳过音量推送：$previous → $current")
            return
        }
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val percent = (current * 100) / max
        LogUtils.i(TAG, "播放中媒体音量变化：$previous → $current ($percent%)")
        when {
            percent > 50 -> {
                LogUtils.i(TAG, "音量>50%，尝试 volume_above_50")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.VOLUME_ABOVE_50)
            }
            percent < 20 -> {
                LogUtils.i(TAG, "音量<20%，尝试 volume_below_20")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.VOLUME_BELOW_20)
            }
            else -> LogUtils.i(TAG, "音量在 20%–50% 区间，不推送 percent=$percent")
        }
    }
}
