package com.rapid.prominence.minty.service

/** Keeps a song on the same signed URL until it expires or playback fails. */
internal class StreamAddresses {
    private data class Address(val url: String, val expiresAt: Long)
    private val entries = HashMap<String, Address>()

    @Synchronized
    fun of(mediaId: String, now: Long): String? =
        entries[mediaId]?.takeIf { it.expiresAt > now }?.url

    @Synchronized
    fun keep(mediaId: String, url: String, expiresAt: Long) {
        entries[mediaId] = Address(url, expiresAt)
    }

    @Synchronized
    fun playerFailedOn(mediaId: String?): Boolean =
        mediaId != null && entries.remove(mediaId) != null
}
