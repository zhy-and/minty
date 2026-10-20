package com.rapid.prominence.minty.notification.localpush

import android.os.SystemClock

/**
 * DownloadManager 与 MediaStore(Download/) 可能先后命中同一文件，短时按文件名去重。
 */
object DownloadPushDeduper {
    private const val WINDOW_MS = 15_000L

    private val lock = Any()
    private var lastKey: String = ""
    private var lastAtElapsed = 0L

    /** @return true 表示本次应展示；false 表示短时内已推过同名下载。 */
    fun tryClaim(fileName: String): Boolean {
        val key = fileName.trim().lowercase()
        if (key.isEmpty()) return true
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            if (key == lastKey && now - lastAtElapsed < WINDOW_MS) {
                return false
            }
            lastKey = key
            lastAtElapsed = now
            return true
        }
    }
}
