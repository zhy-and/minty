package com.rapid.prominence.minty.second.databean

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.cons.HideExplicitKey
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.pages.BrowseResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class YouTubeBrowseViewModel @Inject constructor(
    @ApplicationContext context: Context,
    browseId : String,
    params : String,
) : ViewModel() {

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            MainBe.browse(browseId, params).onSuccess {
                result.value = it.filterExplicit(context.dataStore.get(HideExplicitKey, false))
            }.onFailure {
                reportException(it)
            }
        }
    }
}

