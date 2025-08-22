package com.rapid.prominence.minty.second.databean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MintyMusicBe
import com.zionhuang.innertube.models.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)

    init {
        viewModelScope.launch {
            MintyMusicBe.likedPlaylists().onSuccess {
                playlists.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}

