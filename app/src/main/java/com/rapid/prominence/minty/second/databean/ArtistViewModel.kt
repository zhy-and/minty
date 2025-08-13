package com.rapid.prominence.minty.second.databean

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.cons.HideExplicitKey
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.models.YTItem
import com.zionhuang.innertube.pages.ArtistPage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class ArtistViewModel  @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    artistId : String
) : ViewModel() {
    val artistPage = MutableStateFlow<ArtistPage?>(null)
    val itemsData = MutableStateFlow<List<YTItem>>(emptyList())
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {

            MainBe.artist(artistId)
                .onSuccess { page ->
//                    artistPage.value = page
                    artistPage.value = page.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                }.onFailure {
                    reportException(it)
                }
        }
    }
}

