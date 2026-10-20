package com.rapid.prominence.minty.runtime

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFER_INFO
import com.tencent.mmkv.MMKV
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** Push/FRC cohort only. This never changes Minty's existing home-screen cohort. */
object UserSegmentHelper {
    private const val KEY_LOCKED = "user_segment_locked"
    private const val TIMEOUT_MS = 10_000L
    private val gateStarted = AtomicBoolean(false)
    private val resolved = AtomicBoolean(false)
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    @Volatile private var referFlowFinished = false

    fun isResolved(): Boolean = resolved.get()

    fun currentSegmentKey(): String {
        ensureMmkv(null)
        val locked = MMKV.defaultMMKV().getString(KEY_LOCKED, "")
        if (locked == "paid") return "paid"
        val refer = MMKV.defaultMMKV().getString(MINTY_APP_APP_REFER_INFO, "").orEmpty()
        return segmentForRefer(refer)
    }

    fun addOnResolvedListener(listener: (String) -> Unit) {
        listeners.add(listener)
        if (resolved.get()) listener(currentSegmentKey())
    }

    fun startGate(context: Context) {
        if (!gateStarted.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread({
            ensureMmkv(app)
            val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
            while (SystemClock.elapsedRealtime() < deadline) {
                if (MMKV.defaultMMKV().getString(MINTY_APP_APP_REFER_INFO, "").orEmpty().isNotEmpty() || referFlowFinished) break
                try { Thread.sleep(100L) } catch (_: InterruptedException) { break }
            }
            lockAndNotify(app)
        }, "PushSegmentGate").start()
    }

    fun onReferInfoUpdated(context: Context) {
        if (resolved.get()) lockAndNotify(context.applicationContext)
    }

    fun notifyReferFlowFinished(context: Context) {
        referFlowFinished = true
        if (resolved.get()) lockAndNotify(context.applicationContext)
    }

    private fun lockAndNotify(context: Context) {
        ensureMmkv(context)
        val mmkv = MMKV.defaultMMKV()
        val previous = mmkv.getString(KEY_LOCKED, "")
        val refer = mmkv.getString(MINTY_APP_APP_REFER_INFO, "").orEmpty()
        val next = if (previous == "paid" || segmentForRefer(refer) == "paid") "paid" else "organic"
        if (previous != next) mmkv.putString(KEY_LOCKED, next)
        val first = resolved.compareAndSet(false, true)
        if (first || previous != next) listeners.forEach { listener ->
            runCatching { listener(next) }.onFailure { Log.w("PushSegment", "Listener failed", it) }
        }
    }

    internal fun segmentForRefer(refer: String): String =
        if (refer.isEmpty() || refer.contains("organic")) "organic" else "paid"

    private fun ensureMmkv(context: Context?) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            if (context != null) MMKV.initialize(context)
        }
    }
}
