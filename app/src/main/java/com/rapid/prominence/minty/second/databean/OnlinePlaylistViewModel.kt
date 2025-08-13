package com.rapid.prominence.minty.second.databean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OnlinePlaylistViewModel  constructor(
  playlistid : String,
) : ViewModel() {

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            MainBe.playlist(playlistid).completed()
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = playlistPage.songs
                }.onFailure {
                    reportException(it)
                }
        }
    }
}

