package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.blankj.utilcode.util.LogUtils

/**
 * MediaStore 音频新增且落在 Download/ → [AUDIO_DOWNLOAD]。
 * 覆盖不走系统 DownloadManager、直接写文件的路径。
 *
 * 注意：API 29+ 常见流程是先插入 `IS_PENDING=1` 再写完置 0；必须等写完才推，
 * 否则会在「开始下载」就触发，并被 [DownloadPushDeduper] 挡住真正完成时的推送。
 */
object MediaStoreDownloadWatcher {
    private const val TAG = "MediaStoreDownloadWatcher"
    private const val DEBOUNCE_MS = 800L
    private const val RECENT_WINDOW_SEC = 90L

    @Volatile
    private var registered = false
    private var pendingUri: Uri? = null
    private var pendingCheck: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "已注册，跳过")
            return
        }
        val appContext = context.applicationContext
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                scheduleCheck(appContext, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scheduleCheck(appContext, uri)
            }
        }
        val cr = appContext.contentResolver
        runCatching {
            cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
            registered = true
            LogUtils.i(TAG, "MediaStore Audio Download 监听注册成功")
        }.onFailure {
            LogUtils.e(TAG, "注册失败：${it.message}")
        }
    }

    private fun scheduleCheck(context: Context, uri: Uri?) {
        if (uri != null) pendingUri = uri
        pendingCheck?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            val target = pendingUri
            pendingUri = null
            checkDownload(context, target)
        }
        pendingCheck = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun checkDownload(context: Context, changedUri: Uri?) {
        val row = resolveLatest(context, changedUri) ?: run {
            LogUtils.w(TAG, "无音频行 uri=$changedUri")
            return
        }
        val nowSec = System.currentTimeMillis() / 1000
        val newest = maxOf(row.dateAdded, row.dateModified)
        if (nowSec - newest > RECENT_WINDOW_SEC) {
            LogUtils.w(
                TAG,
                "过期忽略 id=${row.id} age=${nowSec - newest}s name=${row.name} path=${row.path}",
            )
            return
        }
        if (!DownloadMediaClassifier.isDownloadPath(row.path)) {
            return
        }
        if (!DownloadMediaClassifier.isAudio(row.mime, row.name, row.path)) {
            LogUtils.i(TAG, "非音频，跳过 name=${row.name} mime=${row.mime}")
            return
        }
        if (!DownloadMediaClassifier.isReadyForDownloadPush(row.isPending, row.sizeBytes)) {
            LogUtils.i(
                TAG,
                "下载未写完，等待完成：name=${row.name} isPending=${row.isPending} size=${row.sizeBytes}",
            )
            return
        }
        if (!DownloadPushDeduper.tryClaim(row.name)) {
            LogUtils.i(TAG, "已推过同名下载，跳过 name=${row.name}")
            return
        }
        LogUtils.i(
            TAG,
            "MediaStore Audio Download → AUDIO_DOWNLOAD name=${row.name} path=${row.path} mime=${row.mime}",
        )
        LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.AUDIO_DOWNLOAD)
    }

    private fun resolveLatest(context: Context, changedUri: Uri?): MediaRow? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        if (changedUri != null && changedUri != collection) {
            queryRow(context, changedUri)?.let { return it }
        }
        return queryLatest(context, collection)
    }

    private fun queryLatest(context: Context, collection: Uri): MediaRow? {
        val pathColumn = pathColumn()
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        return runCatching {
            context.contentResolver.query(collection, projection(pathColumn), null, null, sort)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    readRow(cursor, pathColumn)
                }
        }.onFailure {
            LogUtils.e(TAG, it.message)
        }.getOrNull()
    }

    private fun queryRow(context: Context, uri: Uri): MediaRow? {
        val pathColumn = pathColumn()
        return runCatching {
            context.contentResolver.query(uri, projection(pathColumn), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    readRow(cursor, pathColumn)
                }
        }.getOrNull()
    }

    private fun pathColumn(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
    }

    private fun projection(pathColumn: String): Array<String> {
        val cols = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            pathColumn,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cols.add(MediaStore.MediaColumns.IS_PENDING)
        }
        return cols.toTypedArray()
    }

    private fun readRow(cursor: android.database.Cursor, pathColumn: String): MediaRow {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val pathIndex = cursor.getColumnIndexOrThrow(pathColumn)
        val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
        val pendingIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
        } else {
            -1
        }
        return MediaRow(
            id = cursor.getLong(idIndex),
            name = cursor.getString(nameIndex).orEmpty(),
            path = cursor.getString(pathIndex).orEmpty(),
            dateAdded = cursor.getLong(addedIndex),
            dateModified = cursor.getLong(modifiedIndex),
            mime = cursor.getString(mimeIndex).orEmpty(),
            sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                null
            },
            isPending = if (pendingIndex >= 0 && !cursor.isNull(pendingIndex)) {
                cursor.getInt(pendingIndex)
            } else {
                null
            },
        )
    }

    private data class MediaRow(
        val id: Long,
        val name: String,
        val path: String,
        val dateAdded: Long,
        val dateModified: Long,
        val mime: String,
        val sizeBytes: Long?,
        val isPending: Int?,
    )
}
