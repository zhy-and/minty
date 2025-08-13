package com.rapid.prominence.minty.database.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import com.rapid.prominence.minty.database.entities.ArtistEntity

@Immutable
data class Artist(
    @Embedded
    val artist: ArtistEntity,
    val songCount: Int,
) : LocalItem() {
    override val id: String
        get() = artist.id
    override val title: String
        get() = artist.name
    override val thumbnailUrl: String?
        get() = artist.thumbnailUrl
}
