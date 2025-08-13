package com.rapid.prominence.minty.second.databean

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rapid.prominence.minty.bean.ItemsPage
import com.rapid.prominence.minty.cons.HideExplicitKey
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.reportException
import com.zionhuang.innertube.MainBe
import com.zionhuang.innertube.models.BrowseEndpoint
import com.zionhuang.innertube.models.filterExplicit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class ArtistItemsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    browseId : String,
    params : String
) : ViewModel() {

    val title = MutableStateFlow("")
    val itemsPage = MutableStateFlow<ItemsPage?>(null)

    init {
        viewModelScope.launch {
            MainBe.artistItems(
                BrowseEndpoint(
                    browseId = browseId,
                    params = params
                )
            ).onSuccess { artistItemsPage ->
                title.value = artistItemsPage.title
                itemsPage.value = ItemsPage(
                    items = artistItemsPage.items.distinctBy { it.id },
                    continuation = artistItemsPage.continuation
                )
            }.onFailure {
                reportException(it)
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val oldItemsPage = itemsPage.value ?: return@launch
            val continuation = oldItemsPage.continuation ?: return@launch
            MainBe.artistItemsContinuation(continuation)
                .onSuccess { artistItemsContinuationPage ->
                    itemsPage.update {
                        ItemsPage(
                            items = (oldItemsPage.items + artistItemsContinuationPage.items)
                                .distinctBy { it.id }
                                .filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                            continuation = artistItemsContinuationPage.continuation
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }
}

