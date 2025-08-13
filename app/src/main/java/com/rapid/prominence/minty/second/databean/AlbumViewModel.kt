package com.rapid.prominence.minty.second.databean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlbumViewModel @Inject constructor(
    database: MusicDatabase,
    albumsId: String,
) : ViewModel() {
    val albumId = albumsId
    val albumWithSongs = database.albumWithSongs(albumId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())
    val songLists = MutableStateFlow<List<SongItem>>(emptyList())

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            MainBe.album(albumId).onSuccess {
                if (album == null || album.album.songCount == 0) {
                    database.transaction {
                        if (album == null) insert(it)
                        else update(album.album, it)
                    }
                }
                songLists.value = it.songs
                otherVersions.value = it.otherVersions
            }.onFailure {
                reportException(it)
                if (it.message?.contains("NOT_FOUND") == true) {
                    // This album no longer exists in YouTube Music
                    database.query {
                        album?.album?.let(::delete)
                    }
                }
            }
        }
    }
}

