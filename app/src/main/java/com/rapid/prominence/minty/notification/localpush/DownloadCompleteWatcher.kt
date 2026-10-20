package com.rapid.prominence.minty.notification.localpush

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.blankj.utilcode.util.LogUtils

/**
 * 系统 DownloadManager 下载完成 → [AUDIO_DOWNLOAD]（仅音频）。
 */
object DownloadCompleteWatcher {
    private const val TAG = "DownloadCompleteWatcher"

    @Volatile
    private var registered = false

    private var receiver: BroadcastReceiver? = null

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "下载完成监听已注册，跳过")
            return
        }
        val appContext = context.applicationContext
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id < 0L) {
                    LogUtils.w(TAG, "下载完成广播无有效 downloadId")
                    return
                }
                handleComplete(appContext, id)
            }
        }
        receiver = broadcast
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(broadcast, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(broadcast, filter)
            }
            registered = true
            LogUtils.i(TAG, "DownloadManager 完成监听注册成功")
        }.onFailure {
            LogUtils.e(TAG, "注册失败：${it.message}")
        }
    }

    private fun handleComplete(context: Context, downloadId: Long) {
        val dm = context.getSystemService(DownloadManager::class.java) ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)
        runCatching {
            dm.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    LogUtils.w(TAG, "找不到下载记录 id=$downloadId")
                    return
                }
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    LogUtils.i(TAG, "下载未成功，忽略：id=$downloadId status=$status")
                    return
                }
                val mimeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                val nameIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
                val title = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                val localUri = if (uriIdx >= 0) cursor.getString(uriIdx).orEmpty() else ""
                if (!DownloadMediaClassifier.isAudio(mime, title, localUri)) {
                    LogUtils.i(TAG, "非音频下载，忽略：id=$downloadId mime=$mime title=$title")
                    return
                }
                val claimName = title.ifBlank {
                    localUri.substringAfterLast('/').substringBefore('?')
                }
                if (!DownloadPushDeduper.tryClaim(claimName)) {
                    LogUtils.i(TAG, "下载完成已由 MediaStore 推过，跳过：id=$downloadId title=$title")
                    return
                }
                LogUtils.i(TAG, "音频下载完成：id=$downloadId mime=$mime title=$title")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.AUDIO_DOWNLOAD)
            }
        }.onFailure {
            LogUtils.e(TAG, "处理下载完成失败：${it.message}")
        }
    }
}
