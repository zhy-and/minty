package com.rapid.prominence.minty.notification.localpush

import android.content.Context

object StorageFreeDropState {
    fun evaluateAndCommit(context: Context): Status {
        context.applicationContext
        return Status(
            currentFreePercent = 0f,
            baselineFreePercent = null,
            dropPoints = 0f,
            shouldShow = false,
            reason = "unsupported_in_enmusic",
        )
    }

    data class Status(
        val currentFreePercent: Float,
        val baselineFreePercent: Float?,
        val dropPoints: Float,
        val shouldShow: Boolean,
        val reason: String,
    ) {
        fun describe(): String {
            return "current=$currentFreePercent baseline=${baselineFreePercent ?: "none"} " +
                "drop=${dropPoints}pt shouldShow=$shouldShow reason=$reason"
        }
    }
}
