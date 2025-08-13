package com.rapid.prominence.minty

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.rapid.prominence.minty.ei.main.EiMainPage.Companion.playerConnection
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.ei.kts.toMediaMetadata
import com.rapid.prominence.minty.service.queues.ListQueue

fun randomBitmap(): Int {
   val type = (1..3).random()
     if (type == 1){
         return R.mipmap.t1
     }else if (type == 2){
         return R.mipmap.t2
     }else{
         return R.mipmap.t3
     }
}

fun posBitmap(type : Int): Int {
    return if (type % 3 == 0){
        R.mipmap.t1
    }else if (type % 3 == 1){
        R.mipmap.t2
    }else if (type % 3 == 2){
        R.mipmap.t3
    }else{
        R.mipmap.t1
    }
}


fun playLocalMusic(localMusic: ArrayList<Song>, position: Int) {
    try {
        val mediaItems = localMusic.map { music ->

            MediaItem.Builder()
                .setMediaId(music.id.toString())
                .setUri(Uri.parse(music.uri).buildUpon().appendPath(music.uriId.toString()).build())
                .setCustomCacheKey(music.id.toString())
                .setTag(music.toMediaMetadata())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(music.title)
                        .setSubtitle(music.artist)
                        .setArtist(music.artist)
//                        .setArtworkUri(music.albumArtUri.uri)
//                        .setAlbumTitle(music.album)
                        .setMediaType(MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()

        }
        val queue = ListQueue(title = "Local Music",mediaItems,  startIndex = position)
        playerConnection?.playQueue(queue)
    }catch (e : Exception){
        e.printStackTrace()
    }


}
