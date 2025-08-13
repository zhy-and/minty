package com.rapid.prominence.minty.database.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.rapid.prominence.minty.database.entities.Song

data class PlaylistSong(
    @Embedded val map: PlaylistSongMap,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id",
        entity = SongEntity::class
    )
    val song: Song,
)
