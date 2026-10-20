/*
 * Copyright (C) 2025 OuterTune Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.rapid.prominence.minty.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.rapid.prominence.minty.cons.AudioQuality
import com.zionhuang.innertube.MintyMusicBe
import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeClient.Companion.IOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.VISIONOS
import com.zionhuang.innertube.models.response.PlayerResponse
import okhttp3.OkHttpClient

object YTPlayerUtils {

    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(MintyMusicBe.proxy)
        .build()

    /**
     * Try the existing VR client first, then VISIONOS before the token-limited IOS client.
     */
    private val MAIN_CLIENT: YouTubeClient = YouTubeClient(
        clientName = "ANDROID_VR",
        clientVersion = "1.61.48",
        clientId = "28",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Oculus Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
        loginSupported = false,
        useSignatureTimestamp = false
    )
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        // Could not parse deobfuscation function
//        WEB_REMIX,
//        ANDROID,
//        TVHTML5,
//        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        VISIONOS,
        IOS,
    )


    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Log.d(TAG, "Playback info requested: $videoId")

        val isLoggedIn = MintyMusicBe.cookie != null
        Log.d(TAG, "[$videoId] isLoggedIn: $isLoggedIn")

        val mainPlayerResponse =
            MintyMusicBe.player(videoId, playlistId, MAIN_CLIENT)
                .getOrThrow()

        StreamCheck.visitorDataToAdopt(
            MintyMusicBe.visitorData,
            mainPlayerResponse.responseContext.visitorData,
        )?.let { MintyMusicBe.visitorData = it }

        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null

        var streamPlayerResponse: PlayerResponse? = null
        var lastHttpStatus: Int? = null
        val clients = listOf(MAIN_CLIENT) + STREAM_FALLBACK_CLIENTS
        clientLoop@ for ((clientIndex, client) in clients.withIndex()) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            if (client.loginRequired && !isLoggedIn) continue
            var retriedWithFreshVisitor = false
            while (true) {
                streamPlayerResponse = if (clientIndex == 0 && !retriedWithFreshVisitor) {
                    mainPlayerResponse
                } else {
                    MintyMusicBe.player(videoId, playlistId, client).getOrNull()
                }
                Log.d(TAG, "[$videoId] stream client: ${client.clientName}, " +
                    "playabilityStatus: ${streamPlayerResponse?.playabilityStatus?.status} - " +
                    streamPlayerResponse?.playabilityStatus?.reason)

                val response = streamPlayerResponse
                if (response?.playabilityStatus?.status == "OK") {
                    val candidateFormat = findFormat(response, audioQuality, connectivityManager)
                    val candidateUrl = candidateFormat?.let { findUrlOrNull(it, videoId) }
                    val expires = response.streamingData?.expiresInSeconds
                    if (candidateFormat != null && candidateUrl != null && expires != null) {
                        val status = streamStatus(candidateUrl)
                        lastHttpStatus = status
                        if (StreamCheck.accept(status, clientIndex == clients.lastIndex)) {
                            format = candidateFormat
                            streamUrl = candidateUrl
                            streamExpiresInSeconds = expires
                            Log.i(TAG, "[$videoId] [${client.clientName}] found working stream (HEAD $status)")
                            break@clientLoop
                        }
                        Log.w(TAG, "[$videoId] [${client.clientName}] stream HEAD $status")
                    }
                }

                if (client == VISIONOS && !retriedWithFreshVisitor) {
                    retriedWithFreshVisitor = true
                    val freshVisitor = MintyMusicBe.player(
                        videoId, client = MAIN_CLIENT, asNewVisitor = true,
                    ).getOrNull()?.responseContext?.visitorData
                        ?.takeIf { StreamCheck.looksLikeVisitorData(it) && it != MintyMusicBe.visitorData }
                    if (freshVisitor != null) {
                        MintyMusicBe.visitorData = freshVisitor
                        Log.i(TAG, "[$videoId] retrying VISIONOS with fresh visitorData")
                        continue
                    }
                }
                break
            }
        }

        if (streamUrl == null && lastHttpStatus != null) {
            throw PlaybackException(
                "YouTube refused the stream (HTTP $lastHttpStatus)",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("Missing stream expire time")
        }
        if (format == null) {
            throw Exception("Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("Could not find stream url")
        }

        Log.d(TAG, "[$videoId] stream URL resolved")

        PlaybackData(
            audioConfig ?: streamPlayerResponse.playerConfig?.audioConfig,
            videoDetails ?: streamPlayerResponse.videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        MintyMusicBe.player(videoId, playlistId, client = VISIONOS)

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? =
        playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun streamStatus(url: String): Int? {
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            return httpClient.newCall(requestBuilder.build()).execute().use { it.code }
        } catch (e: Exception) {
            reportException(e)
        }
        return null
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

}
