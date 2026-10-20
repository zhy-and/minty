package com.rapid.prominence.minty.utils

internal object StreamCheck {
    fun accept(status: Int?, isLast: Boolean): Boolean = when (status) {
        null -> isLast
        in 200..299 -> true
        else -> false
    }

    fun looksLikeVisitorData(value: String?): Boolean =
        value?.startsWith("Cgt") == true || value?.startsWith("Cgs") == true

    fun visitorDataToAdopt(current: String?, offered: String?): String? =
        offered?.takeIf { !looksLikeVisitorData(current) && looksLikeVisitorData(it) }
}
