package com.zionhuang.innertube.models.body

import com.zionhuang.innertube.models.body.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
