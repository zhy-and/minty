package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import com.blankj.utilcode.util.LogUtils

/**
 * 播放被临时抢占音频焦点（如来电）时触发本地推送。
 * 由 Minty playback 在
 * [androidx.media3.common.Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS] 时回调。
 */
object AudioFocusPushBridge {
    private const val TAG = "AudioFocusPushBridge"
    private const val MIN_INTERVAL_MS = 30_000L

    @Volatile
    private var lastTriggeredAt = 0L

    fun onTransientAudioFocusLoss(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastTriggeredAt < MIN_INTERVAL_MS) {
            LogUtils.i(TAG, "音频焦点临时丢失去抖中，跳过 audio_focus_lost")
            return
        }
        lastTriggeredAt = now
        LogUtils.i(TAG, "播放被临时抢占音频焦点，尝试 audio_focus_lost")
        LocalEventPushHelper.show(context.applicationContext, LocalEventPushHelper.EventType.AUDIO_FOCUS_LOST)
    }
}
