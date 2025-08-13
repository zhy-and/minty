package com.rapid.prominence.minty.bean

import com.zionhuang.innertube.models.YTItem

data class HomePageSection(
    val title: String,
    val label: String? = null,
    val thumbnail: String? = null,
    val endpoint: YTItem? = null,
    val items: List<YTItem>
)
