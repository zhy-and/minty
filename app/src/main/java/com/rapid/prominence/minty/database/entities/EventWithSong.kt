package com.rapid.prominence.minty.database.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import com.rapid.prominence.minty.database.entities.Song

@Immutable
data class EventWithSong(
    @Embedded
    val event: Event,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: Song,
)
