package com.rapid.prominence.minty.notification.localpush

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * FRC 双分段外壳：`{ version, organic, paid }`。
 * 选段后把子对象原样交给既有单段解析器。
 */
object DualSegmentJson {
    const val KEY_ORGANIC = "organic"
    const val KEY_PAID = "paid"

    fun wrapIdentical(segmentJson: String, version: Int = 1): String {
        val segment = JsonParser.parseString(segmentJson)
        require(segment.isJsonObject) { "segmentJson must be a JSON object" }
        val root = JsonObject()
        root.addProperty("version", version)
        root.add(KEY_ORGANIC, segment)
        root.add(KEY_PAID, segment.deepCopy())
        return root.toString()
    }

    /**
     * @param preferPaid true = 买量优先取 paid（缺段回落 organic）；false = 只取 organic（不回落 paid）
     * @return 单段 JSON；无法解析时 null。兼容旧版「无外壳的单段 JSON」。
     */
    fun extract(raw: String, preferPaid: Boolean): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val root = JsonParser.parseString(trimmed)
            if (!root.isJsonObject) return@runCatching null
            val obj = root.asJsonObject
            val hasDual = obj.has(KEY_ORGANIC) || obj.has(KEY_PAID)
            if (!hasDual) {
                return@runCatching trimmed
            }
            val chosen = if (preferPaid) {
                obj.get(KEY_PAID)?.takeIf { it.isJsonObject }
                    ?: obj.get(KEY_ORGANIC)?.takeIf { it.isJsonObject }
            } else {
                obj.get(KEY_ORGANIC)?.takeIf { it.isJsonObject }
            }
            chosen?.asJsonObject?.toString()
        }.getOrNull()
    }
}
