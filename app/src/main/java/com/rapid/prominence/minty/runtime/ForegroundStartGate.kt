package com.rapid.prominence.minty.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/** Android 12+ only permits a background FGS start in an actual exemption window. */
object ForegroundStartGate {
    fun isAllowed(context: Context, hasBackgroundExemption: Boolean): Boolean {
        val started = runCatching {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }.getOrDefault(false)
        val info = ActivityManager.RunningAppProcessInfo()
        val importance = runCatching {
            ActivityManager.getMyMemoryState(info)
            info.importance
        }.getOrDefault(ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED)
        return allowsStart(Build.VERSION.SDK_INT, started, importance, hasBackgroundExemption)
    }

    fun allowsStart(sdkInt: Int, lifecycleStarted: Boolean, importance: Int, hasBackgroundExemption: Boolean): Boolean =
        sdkInt < Build.VERSION_CODES.S || hasBackgroundExemption || lifecycleStarted ||
            importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
}
