package com.rapid.prominence.minty.bean

import com.zionhuang.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
