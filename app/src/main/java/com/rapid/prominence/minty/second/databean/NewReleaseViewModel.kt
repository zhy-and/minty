package com.rapid.prominence.minty.second.databean

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.cons.HideExplicitKey
import com.rapid.prominence.minty.database.MusicDatabase
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.filterExplicit
import com.rapid.prominence.minty.database.entities.Artist
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


class NewReleaseViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    init {
        viewModelScope.launch {
            MainBe.newReleaseAlbums().onSuccess { albums ->
                val artists: Set<String>
                val favouriteArtists: Set<String>
                database.artistsByCreateDateAsc().first().let { list ->
                    artists = list.map(Artist::id).toHashSet()
                    favouriteArtists = list
                        .filter { it.artist.bookmarkedAt != null }
                        .map { it.id }
                        .toHashSet()
                }
                _newReleaseAlbums.value = albums
                    .sortedBy { album ->
                        if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                        else if (album.artists.orEmpty().any { it.id in artists }) 1
                        else 2
                    }
                    .filterExplicit(context.dataStore.get(HideExplicitKey, false))
            }.onFailure {
                reportException(it)
            }
        }
    }
}

