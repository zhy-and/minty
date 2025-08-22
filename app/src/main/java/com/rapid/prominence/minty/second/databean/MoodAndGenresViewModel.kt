package com.rapid.prominence.minty.second.databean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MintyMusicBe
import com.zionhuang.innertube.pages.MoodAndGenres
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MoodAndGenresViewModel @Inject constructor() : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    init {
        viewModelScope.launch {
            MintyMusicBe.moodAndGenres().onSuccess {
                moodAndGenres.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}

