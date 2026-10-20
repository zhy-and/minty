package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.LogUtils

import com.tencent.mmkv.MMKV

object AppInactiveState {
    private const val TAG = "AppInactiveState"
    private const val KEY_FIRST_OPEN_AT = "local_push_first_open_at"
    private const val KEY_LAST_OPEN_AT = "local_push_app_last_open_at"
    private const val DAY_MS = 86_400_000L

    @Volatile
    private var observerRegistered = false

    fun startTracking(context: Context) {
        ensureMmkv(context)
        if (observerRegistered) return
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    markOpened(context.applicationContext, "process_foreground")
                }
            },
        )
        observerRegistered = true
        LogUtils.i(TAG, "已注册进程前台监听")
    }

    fun markOpened(context: Context, reason: String) {
        ensureMmkv(context)
        val now = System.currentTimeMillis()
        MMKV.defaultMMKV().putLong(KEY_LAST_OPEN_AT, now)
        LogUtils.i(TAG, "刷新 last_open_at=$now reason=$reason")
    }

    fun daysSinceLastOpen(context: Context): Int = status(context).daysSinceOpen

    fun status(context: Context): Status {
        val baseline = baselineAt(context)
        val elapsed = (System.currentTimeMillis() - baseline).coerceAtLeast(0L)
        return Status(
            baselineAt = baseline,
            elapsedMs = elapsed,
            remainingMs = (DAY_MS - elapsed).coerceAtLeast(0L),
            daysSinceOpen = (elapsed / DAY_MS).toInt().coerceIn(1, 99),
        )
    }

    private fun baselineAt(context: Context): Long {
        ensureMmkv(context)
        val store = MMKV.defaultMMKV()
        val lastOpen = store.getLong(KEY_LAST_OPEN_AT, 0L)
        if (lastOpen > 0L) return lastOpen
        val firstOpenAt = store.getLong(KEY_FIRST_OPEN_AT, 0L)
        val baseline = if (firstOpenAt > 0L) firstOpenAt else System.currentTimeMillis()
        store.putLong(KEY_LAST_OPEN_AT, baseline)
        return baseline
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }

    data class Status(
        val baselineAt: Long,
        val elapsedMs: Long,
        val remainingMs: Long,
        val daysSinceOpen: Int,
    ) {
        val shouldShow: Boolean = remainingMs == 0L

        fun describe(): String {
            return "baselineAt=$baselineAt elapsedMs=$elapsedMs remainingMs=$remainingMs " +
                "days=$daysSinceOpen shouldShow=$shouldShow"
        }
    }
}
