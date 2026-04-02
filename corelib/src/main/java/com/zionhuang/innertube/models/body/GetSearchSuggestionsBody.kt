package com.zionhuang.innertube.models.body

import com.zionhuang.innertube.models.body.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
