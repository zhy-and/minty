package com.rapid.prominence.minty.bean

import com.rapid.prominence.minty.database.entities.LocalItem
import com.zionhuang.innertube.models.YTItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
