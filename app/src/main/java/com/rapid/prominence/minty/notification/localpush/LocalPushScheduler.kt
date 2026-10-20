package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

object LocalPushScheduler {
    private const val TAG = "LocalPushScheduler"
    private const val MIN_INTERVAL_MS = 1_000L

    private val handler = Handler(Looper.getMainLooper())
    private val scheduledBuckets = HashSet<String>()

    @Volatile
    private var generation: Int = 0

    fun start(context: Context) {
        val appContext = context.applicationContext
        val bucketKeys = LocalPushConfig.periodicBucketKeys()
        LogUtils.i(TAG, "本地推送周期调度启动：generation=$generation bucketKeys=$bucketKeys")
        bucketKeys.forEach { bucketKey ->
            if (scheduledBuckets.add(bucketKey)) {
                scheduleNext(appContext, bucketKey, initial = true)
            } else {
                LogUtils.i(TAG, "本地推送周期调度已存在，跳过重复启动：bucket=$bucketKey generation=$generation")
            }
        }
    }

    /** FRC 配置变更后热重启：清空旧 tick，按新 interval/开关重排。 */
    fun restart(context: Context) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            restartOnMain(appContext)
        } else {
            LogUtils.i(TAG, "restart 不在主线程，post 到主线程执行")
            handler.post { restartOnMain(appContext) }
        }
    }

    private fun restartOnMain(appContext: Context) {
        generation += 1
        handler.removeCallbacksAndMessages(null)
        val cleared = scheduledBuckets.toList()
        scheduledBuckets.clear()
        LogUtils.i(
            TAG,
            "本地推送周期调度热重启：newGeneration=$generation 已清空旧排程 buckets=$cleared",
        )
        start(appContext)
    }

    private fun scheduleNext(context: Context, bucketKey: String, initial: Boolean) {
        val intervalSec = LocalPushConfig.bucketIntervalSec(bucketKey)
        if (intervalSec < 0) {
            LogUtils.i(TAG, "桶不需要周期调度：bucket=$bucketKey intervalSec=$intervalSec")
            return
        }
        val delayMs = (intervalSec * 1000L).coerceAtLeast(MIN_INTERVAL_MS)
        val scheduledGeneration = generation
        LogUtils.i(
            TAG,
            "安排下一次本地推送周期tick：bucket=$bucketKey delayMs=$delayMs intervalSec=$intervalSec " +
                "initial=$initial generation=$scheduledGeneration",
        )
        handler.postDelayed({
            if (scheduledGeneration != generation) {
                LogUtils.i(
                    TAG,
                    "跳过过期周期tick：bucket=$bucketKey scheduledGeneration=$scheduledGeneration currentGeneration=$generation",
                )
                return@postDelayed
            }
            runBucketTick(context.applicationContext, bucketKey)
            scheduleNext(context.applicationContext, bucketKey, initial = false)
        }, delayMs)
    }

    private fun runBucketTick(context: Context, bucketKey: String) {
        LogUtils.i(TAG, "本地推送周期tick开始：bucket=$bucketKey")
        val types = LocalPushConfig.eventTypesForBucket(bucketKey)
        if (types.isEmpty()) {
            LogUtils.w(TAG, "本地推送周期tick无可用场景：bucket=$bucketKey")
            return
        }
        for (type in types) {
            LogUtils.i(TAG, "按优先级尝试周期场景：bucket=$bucketKey type=${type.name}")
            val shown = LocalEventPushHelper.show(context, type)
            LogUtils.i(TAG, "周期场景尝试结果：bucket=$bucketKey type=${type.name} shown=$shown")
            if (shown) {
                LogUtils.i(TAG, "本轮周期tick已成功展示，停止尝试后续低优先级场景：bucket=$bucketKey type=${type.name}")
                return
            }
        }
        LogUtils.w(TAG, "本轮周期tick所有场景均未展示：bucket=$bucketKey")
    }
}
