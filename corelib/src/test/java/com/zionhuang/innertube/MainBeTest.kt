
package com.zionhuang.innertube

import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_ALBUM
import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_ARTIST
import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_SONG
import com.zionhuang.innertube.MainBe.SearchFilter.Companion.FILTER_VIDEO
import com.zionhuang.innertube.models.WatchEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * 用于测试 YouTube 功能的测试类。
 */
@Ignore("未知原因导致 GitHub Action 始终运行此测试时出错")
class MainBeTest {
    private val youTube = MainBe

    /**
     * 测试 'player' 接口，确保播放状态为 "OK"。
     */
    @Test
    fun `Check 'player' endpoint`() = runBlocking {
        VIDEO_IDS.forEach { videoId ->
            val playerResponse = youTube.player(videoId).getOrThrow()
            assertTrue(playerResponse.playabilityStatus.status == "OK")
        }
    }

    /**
     * 测试视频流是否可播放，通过检查响应状态来验证。
     */
    @Test
    fun `Check playable stream`() = runBlocking {
        VIDEO_IDS.forEach { videoId ->
            val playerResponse = youTube.player(videoId).getOrThrow()
            val format = playerResponse.streamingData!!.adaptiveFormats[0]
            val url = format.url!!
            println(url)
            val response = HttpClient(OkHttp).get(url) {
                headers {
                    append("Range", "bytes=0-10")
                }
            }
            assertTrue(response.status.isSuccess())
        }
    }

    /**
     * 测试 'search' 接口，确保搜索结果符合预期。
     */
    @Test
    fun `Check 'search' endpoint`() = runBlocking {
        // 搜索关键词 "musi" 的顶级结果
        val searchAllTypeResult = youTube.searchSummary("musi").getOrThrow()
        assertTrue(searchAllTypeResult.summaries.size > 1)
        for (filter in listOf(
            FILTER_SONG,
            FILTER_VIDEO,
            FILTER_ALBUM,
            FILTER_ARTIST,
            FILTER_FEATURED_PLAYLIST,
            FILTER_COMMUNITY_PLAYLIST
        )) {
            val searchResult = youTube.search(SEARCH_QUERY, filter).getOrThrow()
            assertTrue(searchResult.items.isNotEmpty())
        }
    }

    /**
     * 测试搜索分页功能，确保可以正确加载更多结果。
     */
    @Test
    fun `Check search continuation`() = runBlocking {
        var count = 5
        var searchResult = youTube.search(SEARCH_QUERY, FILTER_SONG).getOrThrow()
        while (searchResult.continuation != null && count > 0) {
            searchResult.items.forEach {
                println(it.title)
            }
            searchResult = youTube.searchContinuation(searchResult.continuation!!).getOrThrow()
            count -= 1
        }
        searchResult.items.forEach {
            println(it.title)
        }
    }

    /**
     * 测试 'get_search_suggestion' 接口，确保返回的搜索建议不为空。
     */
    @Test
    fun `Check 'get_search_suggestion' endpoint`() = runBlocking {
        val suggestions = youTube.searchSuggestions(SEARCH_QUERY).getOrThrow()
        assertTrue(suggestions.queries.isNotEmpty())
    }

    /**
     * 测试 'browse' 接口，确保艺术家、专辑和播放列表的数据不为空。
     */
    @Test
    fun `Check 'browse' endpoint`() = runBlocking {
        var artist = youTube.artist("UCI6B8NkZKqlFWoiC_xE-hzA").getOrThrow()
        assertTrue(artist.sections.isNotEmpty())
        artist = youTube.artist("UCy2RKLxIOMOfGld_yBYEBLw").getOrThrow() // 包含有声书的艺术家
        assertTrue(artist.sections.isNotEmpty())
        val album = youTube.album("MPREb_oNAdr9eUOfS").getOrThrow()
        assertTrue(album.songs.isNotEmpty())
        val playlist = youTube.playlist("RDCLAK5uy_mHAEb33pqvgdtuxsemicZNu-5w6rLRweo").getOrThrow()
        assertTrue(playlist.songs.isNotEmpty())
    }

    /**
     * 测试 'next' 接口，确保返回的播放列表项不为空。
     */
    @Test
    fun `Check 'next' endpoint`() = runBlocking {
        var nextResult = youTube.next(WatchEndpoint(videoId = "qivRUhepWVA", playlistId = "RDEMQWAKLFUHzBCn9nEsPHDYAw")).getOrThrow()
        assertTrue(nextResult.items.isNotEmpty())
        nextResult = youTube.next(WatchEndpoint(videoId = "jF4KKOsoyDs", playlistId = "PLaHh1PiehjvqOXm1J7b2QGy2iAvN84Azb")).getOrThrow()
        assertTrue(nextResult.items.isNotEmpty())
    }

    /**
     * 测试 'next' 分页功能，确保可以正确加载更多结果。
     */
    @Test
    fun `Check 'next' continuation`() = runBlocking {
        val endpoint = WatchEndpoint(videoId = "afXwxtQLvZM")
        var count = 5
        var nextResult = youTube.next(endpoint).getOrThrow()
        while (nextResult.continuation != null && count > 0) {
            nextResult.items.forEach {
                println(it.title)
            }
            nextResult = youTube.next(nextResult.endpoint, nextResult.continuation).getOrThrow()
            count -= 1
        }
        nextResult.items.forEach {
            println(it.title)
        }
    }

    /**
     * 测试 'get_queue' 接口，确保返回的队列数据不为空。
     */
    @Test
    fun `Check 'get_queue' endpoint`() = runBlocking {
        var queue = youTube.queue(videoIds = VIDEO_IDS).getOrThrow()
        assertTrue(queue.isNotEmpty())
        queue = youTube.queue(playlistId = PLAYLIST_ID).getOrThrow()
        assertTrue(queue.isNotEmpty())
    }

    /**
     * 测试播放列表浏览功能，确保可以正确加载歌曲分页。
     */
    @Test
    fun `Browse playlist`() = runBlocking {
        // 此播放列表包含 2900 首歌曲
        val playlistId = "PLtAw-mgfCzRwduBTjBHknz5U4_ZM4n6qm"
        var count = 5
        val playlistPage = MainBe.playlist(playlistId).getOrThrow()
        var songs = playlistPage.songs
        var continuation = playlistPage.songsContinuation
        while (count > 0) {
            songs.forEach {
                println(it.id)
            }
            if (continuation == null) break
            val continuationPage = MainBe.playlistContinuation(continuation).getOrThrow()
            songs = continuationPage.songs
            continuation = continuationPage.continuation
            count--
        }
    }

    /**
     * 测试歌词接口，确保返回的歌词不为空。
     */
    @Test
    fun lyrics() = runBlocking {
        val nextResult = MainBe.next(WatchEndpoint(videoId = "NCC6lI0GGy0")).getOrThrow()
        val lyrics = MainBe.lyrics(nextResult.lyricsEndpoint!!).getOrThrow()
        assertTrue(lyrics != null)
    }

    /**
     * 测试相关视频接口，确保返回的相关视频列表不为空。
     */
    @Test
    fun related() = runBlocking {
        val relatedEndpoint = MainBe.next(WatchEndpoint(videoId = "Z6ji6kls_OA")).getOrThrow().relatedEndpoint!!
        val relatedPage = MainBe.related(relatedEndpoint).getOrThrow()
        assertTrue(relatedPage.songs.isNotEmpty())
    }

    /**
     * 测试字幕接口，确保返回的字幕内容不为空。
     */
    @Test
    fun transcript() = runBlocking {
        val lyrics = MainBe.transcript("7G0ovtPqHnI").getOrThrow()
        assertTrue(lyrics.isNotEmpty())
    }

    companion object {
        private val VIDEO_IDS = listOf(
            "4H-N260cPCg",
            "jF4KKOsoyDs",
            "x8VYWazR5mE" // 需要登录
        )

        private const val PLAYLIST_ID = "RDAMVM_WVXrDmm-P0"

        private const val SEARCH_QUERY = "YOASOBI"
    }
}