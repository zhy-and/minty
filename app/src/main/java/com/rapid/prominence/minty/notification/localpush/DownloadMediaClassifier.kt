package com.rapid.prominence.minty.notification.localpush

/**
 * Download/ 路径判定与 MIME/后缀分类（音频下载完成场景）。
 */
object DownloadMediaClassifier {

    fun isDownloadPath(path: String): Boolean {
        val p = path.lowercase().replace('\\', '/')
        return p.contains("download/") ||
            p.contains("/download") ||
            p.contains("downloads/") ||
            p.contains("/downloads") ||
            p == "download" ||
            p == "downloads"
    }

    fun isAudio(mime: String, name: String, pathOrUri: String): Boolean {
        val hay = "$mime $name $pathOrUri".lowercase()
        return mime.startsWith("audio/") ||
            hasExt(hay, listOf(".mp3", ".aac", ".wav", ".flac", ".m4a", ".ogg", ".opus", ".wma"))
    }

    /**
     * 是否已真正写完、可触发「下载完成」推送。
     *
     * - API 29+：`IS_PENDING=1` 时 MediaStore 行已插入但内容仍在写入，此时推送会变成「开始下载就推」。
     * - 无 pending 信息时：用 size 兜底，0 字节视为未完成。
     */
    fun isReadyForDownloadPush(isPending: Int?, sizeBytes: Long?): Boolean {
        if (isPending != null && isPending != 0) return false
        if (sizeBytes != null && sizeBytes <= 0L) return false
        return true
    }

    private fun hasExt(hay: String, exts: List<String>): Boolean = exts.any { hay.contains(it) }
}
