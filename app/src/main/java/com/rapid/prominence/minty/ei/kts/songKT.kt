package com.rapid.prominence.minty.ei.kts

import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.Song

fun Song.toMediaMetadata() = MediaMetadata(
    id = id.toString(),
    title = title,
    artists = listOf(
        MediaMetadata.Artist(
            id = "",
            name = artist ?: "Unknown Artist" // 默认值处理
        )
    ),
    duration = duration.toInt() ?: -1, // 假设LocalMusic有duration属性
//    thumbnailUrl = t?.resize(544, 544), // 保持与SongItem相同的图片尺寸处理
    album = album?.let {
        MediaMetadata.Album(
            id = it,
            title = it
        )
    },
    explicit = false
   // 假设LocalMusic有explicit属性
)