package com.zionhuang.innertube.models.body

import com.zionhuang.innertube.models.body.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
