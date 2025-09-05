package com.rapid.prominence.minty.second.search


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.ItemsPage
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.HideExplicitKey
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.searchResultBanner
import com.rapid.prominence.minty.cons.searchInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.SearchHistory
import com.rapid.prominence.minty.databinding.MintySecSearchResBinding
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.second.SearchViewAlbumAdapter
import com.rapid.prominence.minty.second.MintySecAlbumDetailPage
import com.rapid.prominence.minty.second.SearchViewArtAdapter
import com.rapid.prominence.minty.second.MintySecArtistDetailPage
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.SpecialVerManager
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.tencent.mmkv.MMKV
import com.zionhuang.innertube.MintyMusicBe
import com.zionhuang.innertube.MintyMusicBe.SearchFilter.Companion.FILTER_ALBUM
import com.zionhuang.innertube.MintyMusicBe.SearchFilter.Companion.FILTER_ARTIST
import com.zionhuang.innertube.MintyMusicBe.SearchFilter.Companion.FILTER_SONG
import com.zionhuang.innertube.MintyMusicBe.SearchFilter.Companion.FILTER_VIDEO
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.filterExplicit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool


class MusicSearchActivity : MintyBaseActivity() {
    private var lastVisibleItem: Int = 0
    private var lastVideoVisibleItem: Int = 0
    private var data: MusicDatabase?= null
    private  var songAdapter: SongAdapter?= null
    private  var searchViewAlbumAdapter: SearchViewAlbumAdapter?= null
    private  var searchViewArtAdapter: SearchViewArtAdapter?= null
    private  var songUniqueAdapter: SongAdapter?= null
    private  var albumUniqueAdapter: SearchViewAlbumAdapter?= null
    private  var artistUniqueAdapter: SearchViewArtAdapter?= null
    private  var videoUniqueAdapter: SearchVideoSongAdapter?= null
    var isLoadingNextPage = false
    var isLoadingAlbumNextPage = false
    var isLoadingArtistNextPage = false
    var isLoadingPlayNextPage = false
    private  var playAdapter: SearchVideoSongAdapter?= null
    private val youTube = MintyMusicBe
    var key = FILTER_SONG.value
    var currentType = 0
    var currentSearchContent = ""
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private fun showAnimations(isShow: Boolean) {
        binding.specAnm.apply {
            if (isShow){
                visibility = View.VISIBLE
                startShimmerAnimation()
            }else{
                visibility = View.GONE
                stopShimmerAnimation()
            }
        }
    }
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                MESSAGE_MSG_CODE -> {
                    loadSpeNative()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (handler?.hasMessages(MESSAGE_MSG_CODE) == true){
            handler?.removeMessages(MESSAGE_MSG_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        loadSpeNative()
    }

    private fun loadSpeNative() {
        val normalBan: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            searchResultBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("searchResultBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("searchResultBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(searchResultBanner())) {
                LogUtils.e("searchResultBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(searchResultBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("searchResultBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("searchResultBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(searchResultBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private fun setVisible(one: View,song: View, album: View, artist: View,view : View) {
        one.visibility = View.VISIBLE
        song.visibility = View.GONE
        album.visibility = View.GONE
        artist.visibility = View.GONE
        view.visibility = View.GONE
        if (binding.emptyResult.isVisible){
            binding.emptyResult.visibility = View.GONE
        }
        when(currentType){
            0 ->{
                binding.all.setBackgroundResource(R.drawable.tag_se_search)
                binding.artist.setBackgroundResource(R.drawable.tag_un_search)
                binding.albums.setBackgroundResource(R.drawable.tag_un_search)
                binding.video.setBackgroundResource(R.drawable.tag_un_search)
                binding.song.setBackgroundResource(R.drawable.tag_un_search)
                binding.all.setTextColor("#000000".toColorInt())
                binding.artist.setTextColor("#829D77".toColorInt())
                binding.albums.setTextColor("#829D77".toColorInt())
                binding.video.setTextColor("#829D77".toColorInt())
                binding.song.setTextColor("#829D77".toColorInt())
            }
            1 ->{
                binding.song.setBackgroundResource(R.drawable.tag_se_search)
                binding.artist.setBackgroundResource(R.drawable.tag_un_search)
                binding.albums.setBackgroundResource(R.drawable.tag_un_search)
                binding.video.setBackgroundResource(R.drawable.tag_un_search)
                binding.all.setBackgroundResource(R.drawable.tag_un_search)
                binding.song.setTextColor("#000000".toColorInt())
                binding.artist.setTextColor("#829D77".toColorInt())
                binding.albums.setTextColor("#829D77".toColorInt())
                binding.video.setTextColor("#829D77".toColorInt())
                binding.all.setTextColor("#829D77".toColorInt())
            }
            2 ->{
                binding.albums.setBackgroundResource(R.drawable.tag_se_search)
                binding.artist.setBackgroundResource(R.drawable.tag_un_search)
                binding.all.setBackgroundResource(R.drawable.tag_un_search)
                binding.video.setBackgroundResource(R.drawable.tag_un_search)
                binding.song.setBackgroundResource(R.drawable.tag_un_search)
                binding.albums.setTextColor("#000000".toColorInt())
                binding.artist.setTextColor("#829D77".toColorInt())
                binding.all.setTextColor("#829D77".toColorInt())
                binding.video.setTextColor("#829D77".toColorInt())
                binding.song.setTextColor("#829D77".toColorInt())
            }
            3 ->{
                binding.artist.setBackgroundResource(R.drawable.tag_se_search)
                binding.all.setBackgroundResource(R.drawable.tag_un_search)
                binding.albums.setBackgroundResource(R.drawable.tag_un_search)
                binding.video.setBackgroundResource(R.drawable.tag_un_search)
                binding.song.setBackgroundResource(R.drawable.tag_un_search)
                binding.artist.setTextColor("#000000".toColorInt())
                binding.all.setTextColor("#829D77".toColorInt())
                binding.albums.setTextColor("#829D77".toColorInt())
                binding.video.setTextColor("#829D77".toColorInt())
                binding.song.setTextColor("#829D77".toColorInt())
            }
            4 ->{
                binding.video.setBackgroundResource(R.drawable.tag_se_search)
                binding.artist.setBackgroundResource(R.drawable.tag_un_search)
                binding.albums.setBackgroundResource(R.drawable.tag_un_search)
                binding.all.setBackgroundResource(R.drawable.tag_un_search)
                binding.song.setBackgroundResource(R.drawable.tag_un_search)
                binding.video.setTextColor("#000000".toColorInt())
                binding.albums.setTextColor("#829D77".toColorInt())
                binding.artist.setTextColor("#829D77".toColorInt())
                binding.all.setTextColor("#829D77".toColorInt())
                binding.song.setTextColor("#829D77".toColorInt())
            }
        }
    }
    companion object {
        fun isBottom(recyclerView: RecyclerView): Boolean {

            return (recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset() >= recyclerView.computeVerticalScrollRange()
                    && !recyclerView.canScrollVertically(1))
        }
    }

    private lateinit var binding: MintySecSearchResBinding
    fun emptyLayout(){
        binding.apply {
            albumsContainer.visibility = View.GONE
            emptyResult.visibility = View.VISIBLE
            emIv.setImageResource(R.mipmap.empty_sec)
            emTx.text = "No result"
            emoScan.visibility = View.GONE
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(searchInter(),this)

        enableEdgeToEdge()
        binding = MintySecSearchResBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            data =  InternalDatabase.newInstance(this@MusicSearchActivity)

        }
        ImmersionBar.with(this).navigationBarColor(R.color.new_color).init()

        binding.tops.closeButton.setOnClickListener {
            finishCurrent()
        }
        val content =  intent?.getStringExtra("search_txt") ?: ""
        if (!TextUtils.isEmpty(content)){
            binding.tops.searchEditText.setText(content)
            binding.tops.search.post {
                firebaseLog("search_page_search")
                binding.tops.search.performClick()
            }
        }
        binding.songMore.setOnClickListener {
            binding.song.performClick()
        }
        binding.songTitle.setOnClickListener {
            binding.song.performClick()
        }
        binding.artistMore.setOnClickListener {
            binding.artist.performClick()
        }
        binding.artistTitle.setOnClickListener {
            binding.artist.performClick()
        }
        binding.albumMore.setOnClickListener {
            binding.albums.performClick()
        }
        binding.albumTitle.setOnClickListener {
            binding.albums.performClick()
        }
        binding.playListMore.setOnClickListener {
            binding.video.performClick()
        }
        binding.playlistTitle.setOnClickListener {
            binding.video.performClick()
        }
        binding.tops.searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {

                // 处理确认事件
                binding.tops.search.performClick()
                firebaseLog("search_page_search")

                // 隐藏软键盘
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                true // 消费事件
            } else {
                false
            }
        }
        binding.list.apply {
            layoutManager =  SpecialVerManager(this@MusicSearchActivity,2)
            songAdapter = SongAdapter() { songItem ->
                secPlayConnect?.playQueue(radio(songItem.toMediaMetadata()))
                startPlayerPage()

            }
            adapter = songAdapter
        }

        binding.albumList.layoutManager =  SpecialVerManager(this,2)
        searchViewAlbumAdapter = SearchViewAlbumAdapter(emptyList()) { songItem ->
            startInterActivity(Intent(this@MusicSearchActivity, MintySecAlbumDetailPage::class.java).apply {
                putExtra("albumId",songItem.id)
            }, albumDetInter(),false)
        }


        binding.albumList.adapter = searchViewAlbumAdapter


        binding.playList.layoutManager = SpecialVerManager(this,2)
        playAdapter =  SearchVideoSongAdapter() { songItem ->
            secPlayConnect?.playQueue(radio(songItem.toMediaMetadata()))
            startPlayerPage()

        }
        binding.playList.adapter = playAdapter


        binding.artistList.layoutManager = SpecialVerManager(this,2)
        searchViewArtAdapter = SearchViewArtAdapter(emptyList()) { songItem ->
            startInterActivity(Intent(this@MusicSearchActivity, MintySecArtistDetailPage::class.java).apply {
                putExtra("artistId",songItem.id)
            }, artistDetInter(),false)
        }
        binding.artistList.adapter = searchViewArtAdapter

        binding.all.setOnClickListener {
            currentType = 0
            setVisible(binding.allContainer,binding.songContainer, binding.artistContainer, binding.albumsContainer,binding.videoContainer)
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                return@setOnClickListener
            }
            firebaseLog("search_page_search")
            binding.tops.search.performClick()
        }

        binding.uniqueSongList.apply {
            songUniqueAdapter = SongAdapter() { songItem ->
                secPlayConnect?.playQueue(radio(songItem.toMediaMetadata()))
                startPlayerPage()

            }

            layoutManager = LinearLayoutManager(this@MusicSearchActivity)
            adapter = songUniqueAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager
                    if (layoutManager is LinearLayoutManager) {
                        lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    }
//
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    try{
                        if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 >= songUniqueAdapter?.songs?.size!! && !isLoadingNextPage) { //当前状态为停止滑动
                            binding.uniqueSongList.post {
                                songUniqueAdapter?.addLoadItem()
                            }
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO){
                                    val viewState = viewStateMap[key] ?: return@withContext
                                    val continuation = viewState.continuation
                                    if (continuation != null) {
                                        isLoadingNextPage = true
                                        val searchResult = MintyMusicBe.searchContinuation(continuation).getOrNull() ?: return@withContext
                                        viewStateMap[key] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
                                        viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
                                            withContext(Dispatchers.Main){
                                                songUniqueAdapter?.removeLoadItem()
                                                songUniqueAdapter?.addData(it as ArrayList<SongItem>)
                                            }
                                            isLoadingNextPage = false
//                                                                            binding.progress.visibility = View.GONE
                                        }
//                                        Log.e("viewStateMap2",viewStateMap[key].toString())
                                    }else{
                                        withContext(Dispatchers.Main){
                                            songUniqueAdapter?.removeLoadItem()
                                        }
                                    }
                                }

                            }
                        }

                    }catch(e : Exception){
                        e.printStackTrace()
                    }
//                    if (newState == RecyclerView.SCROLL_STATE_IDLE && isBottom(binding.uniqueSongList) && !isLoadingNextPage) { //当前状态为停止滑动
                }

            })

        }

        binding.uniqueAlbumList.apply {
            albumUniqueAdapter = SearchViewAlbumAdapter(emptyList()) { songItem ->
                startInterActivity(Intent(this@MusicSearchActivity, MintySecAlbumDetailPage::class.java).apply {
                    putExtra("albumId",songItem.id)
                }, albumDetInter(),false)
            }

            layoutManager = LinearLayoutManager(this@MusicSearchActivity)
            adapter = albumUniqueAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && isBottom(binding.uniqueAlbumList) && !isLoadingAlbumNextPage) { //当前状态为停止滑动
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO){
                                val viewState = viewStateMap[key] ?: return@withContext
                                val continuation = viewState.continuation
                                if (continuation != null) {
                                    isLoadingAlbumNextPage = true
                                    val searchResult = MintyMusicBe.searchContinuation(continuation).getOrNull() ?: return@withContext
                                    viewStateMap[key] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
                                    viewStateMap[key]?.items?.filterIsInstance<AlbumItem>()?.let {
                                        withContext(Dispatchers.Main){
                                            albumUniqueAdapter?.addData(it)
                                        }
                                        isLoadingAlbumNextPage = false
                                    }
//                                    Log.e("album2",viewStateMap[key].toString())
                                }
                            }

                        }
                    }
                }
//
            })

            binding.uniqueArtistList.apply {
                artistUniqueAdapter = SearchViewArtAdapter(emptyList()) { songItem ->
                    startInterActivity(Intent(this@MusicSearchActivity, MintySecArtistDetailPage::class.java).apply {
                        putExtra("artistId",songItem.id)
                    }, artistDetInter(),false)
                }

                layoutManager = LinearLayoutManager(this@MusicSearchActivity)
                adapter = artistUniqueAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE && isBottom(binding.uniqueArtistList) && !isLoadingArtistNextPage) { //当前状态为停止滑动

                            lifecycleScope.launch {
                                withContext(Dispatchers.IO){
                                    val viewState = viewStateMap[key] ?: return@withContext
                                    val continuation = viewState.continuation
                                    if (continuation != null) {
                                        isLoadingArtistNextPage = true
                                        val searchResult = MintyMusicBe.searchContinuation(continuation).getOrNull() ?: return@withContext
                                        viewStateMap[key] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
                                        viewStateMap[key]?.items?.filterIsInstance<ArtistItem>()?.let {
                                            withContext(Dispatchers.Main){
                                                artistUniqueAdapter?.addData(it)
                                            }
                                            isLoadingArtistNextPage = false
                                        }
//                                                Log.e("artist2",viewStateMap[key].toString())
                                    }
                                }

                            }
                        }
                    }

                })

            }
            binding.uniqueVideoList.apply {
                videoUniqueAdapter =  SearchVideoSongAdapter() { songItem ->
                    secPlayConnect?.playQueue(radio(songItem.toMediaMetadata()))
                    startPlayerPage()

                }

                layoutManager = LinearLayoutManager(this@MusicSearchActivity)
                adapter = videoUniqueAdapter

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        val layoutManager = recyclerView.layoutManager
                        if (layoutManager is LinearLayoutManager) {
                            lastVideoVisibleItem = layoutManager.findLastVisibleItemPosition()
                        }
//
                    }

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        try{
                            if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVideoVisibleItem + 1 >= videoUniqueAdapter?.songs?.size!! && !isLoadingPlayNextPage) { //当前状态为停止滑动
                                binding.uniqueVideoList.post {
                                    videoUniqueAdapter?.addLoadItem()
                                }
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO){
                                        val viewState = viewStateMap[key] ?: return@withContext
                                        val continuation = viewState.continuation
                                        if (continuation != null) {
                                            isLoadingPlayNextPage = true
                                            val searchResult = MintyMusicBe.searchContinuation(continuation).getOrNull() ?: return@withContext
                                            viewStateMap[key] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
                                            viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
                                                withContext(Dispatchers.Main){
                                                    videoUniqueAdapter?.removeLoadItem()
                                                    videoUniqueAdapter?.addData(it as ArrayList<SongItem>)
                                                }
                                                isLoadingPlayNextPage = false
//                                                                            binding.progress.visibility = View.GONE
                                            }
//                                        Log.e("viewStateMap2",viewStateMap[key].toString())
                                        }else{
                                            withContext(Dispatchers.Main){
                                                videoUniqueAdapter?.removeLoadItem()
                                            }
                                        }
                                    }

                                }
                            }

                        }catch (e : Exception){
                            e.printStackTrace()
                        }
//                    if (newState == RecyclerView.SCROLL_STATE_IDLE && isBottom(binding.uniqueSongList) && !isLoadingNextPage) { //当前状态为停止滑动
                    }
                })

            }


        }


        binding.song.setOnClickListener {
            currentType = 1
            setVisible(binding.songContainer,binding.allContainer, binding.artistContainer, binding.albumsContainer,binding.videoContainer)
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                return@setOnClickListener
            }
            firebaseLog("search_page_search_songs")

            key = FILTER_SONG.value + binding.tops.searchEditText.text.toString()
            lifecycleScope.launch {

                if (viewStateMap[key] == null) {
                    showAnimations(true)

                    withContext(Dispatchers.IO){

                        MintyMusicBe.search(binding.tops.searchEditText.text.toString(), FILTER_SONG)
                            .onSuccess { result ->
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )?.let {
                                    if (it.items.isNullOrEmpty()){
                                        withContext(Dispatchers.Main){
                                            showAnimations(false)
                                            emptyLayout()
                                            songUniqueAdapter?.addData(it.items as ArrayList<SongItem>)
                                        }
                                        return@onSuccess
                                    }else{
                                        viewStateMap[key] = it
                                    }
                                }
//                            Log.e("viewStateMap1",  viewStateMap[key].toString())
                                viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
                                    withContext(Dispatchers.Main){
                                        showAnimations(false)
                                        songUniqueAdapter?.addData(it as ArrayList<SongItem>)
                                    }
                                }
                            }
                            .onFailure {
                                withContext(Dispatchers.Main){
                                    showAnimations(false)
                                    diconnectView()
                                }
//                            reportException(it)
                            }
                    }}else{
//                    viewStateMap[key]?.items?.filterIsInstance<SongItem>().isNullOrEmpty().apply {
//                        binding.emptyResult.visibility = View.VISIBLE
//                    }
//                    viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
//                        withContext(Dispatchers.Main){
//                            showAnimations(false)
//                            songUniqueAdapter?.addData(it as ArrayList<SongItem>)
//                        }
//                    }
                }

            }
        }
        binding.albums.setOnClickListener {
            currentType = 2
            setVisible(binding.albumsContainer,binding.allContainer, binding.artistContainer, binding.songContainer,binding.videoContainer)
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                return@setOnClickListener
            }
            firebaseLog("search_page_search_albums")

            key = FILTER_ALBUM.value + binding.tops.searchEditText.text.toString()
            lifecycleScope.launch {

                if (viewStateMap[key] == null) {
                    showAnimations(true)

                    withContext(Dispatchers.IO){

                        MintyMusicBe.search(binding.tops.searchEditText.text.toString(), FILTER_ALBUM)
                            .onSuccess { result ->
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )?.let {
                                    if (it.items.isNullOrEmpty()){
                                        withContext(Dispatchers.Main){
                                            showAnimations(false)
                                            emptyLayout()
                                            albumUniqueAdapter?.addData(it.items as ArrayList<AlbumItem>)
                                        }
                                        return@onSuccess
                                    }else{
                                        viewStateMap[key] = it
                                    }
                                }
//                                Log.e("album1",  viewStateMap[key].toString())
                                viewStateMap[key]?.items?.filterIsInstance<AlbumItem>()?.let {
                                    withContext(Dispatchers.Main){
                                        showAnimations(false)
                                        albumUniqueAdapter?.addData(it)

                                    }
                                }



                            }
                            .onFailure {
                                withContext(Dispatchers.Main){
                                    showAnimations(false)
                                    diconnectView()
                                }
//                                reportException(it)
                            }
                    }
                }else{
//                    viewStateMap[key]?.items?.filterIsInstance<AlbumItem>().isNullOrEmpty().apply {
//                        binding.emptyResult.visibility = View.VISIBLE
//                    }
                }

            }
        }
        binding.artist.setOnClickListener {
            currentType = 3
            setVisible(binding.artistContainer,binding.allContainer, binding.albumsContainer, binding.songContainer,binding.videoContainer)
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                return@setOnClickListener
            }
            firebaseLog("search_page_search_artists")

            key = FILTER_ARTIST.value + binding.tops.searchEditText.text.toString()
            lifecycleScope.launch {
                if (viewStateMap[key] == null) {
                    showAnimations(true)

                    withContext(Dispatchers.IO){

                        MintyMusicBe.search(binding.tops.searchEditText.text.toString(), FILTER_ARTIST)
                            .onSuccess { result ->
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )?.let {
                                    if (it.items.isNullOrEmpty()){
                                        withContext(Dispatchers.Main){
                                            showAnimations(false)
                                            emptyLayout()
                                            artistUniqueAdapter?.addData(it.items as ArrayList<ArtistItem>)
                                        }
                                        return@onSuccess
                                    }else{
                                        viewStateMap[key] = it
                                    }
                                }
//                                Log.e("artist1",  viewStateMap[key].toString())
                                viewStateMap[key]?.items?.filterIsInstance<ArtistItem>()?.let {
                                    withContext(Dispatchers.Main){
                                        showAnimations(false)
                                        artistUniqueAdapter?.addData(it)
                                    }
                                }



                            }
                            .onFailure {
                                withContext(Dispatchers.Main){
                                    showAnimations(false)
                                    diconnectView()
                                }
//                                reportException(it)
                            }
                    }}else{
//
                }

            }
        }


        binding.video.setOnClickListener {
            currentType = 4
            setVisible(binding.videoContainer,binding.allContainer, binding.albumsContainer, binding.songContainer,binding.artistContainer)
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                return@setOnClickListener
            }
            firebaseLog("search_page_search_videos")

            key = FILTER_VIDEO.value + binding.tops.searchEditText.text.toString()
            lifecycleScope.launch {
                if (viewStateMap[key] == null) {
                    showAnimations(true)

                    withContext(Dispatchers.IO){

                        MintyMusicBe.search(binding.tops.searchEditText.text.toString(), FILTER_VIDEO)
                            .onSuccess { result ->
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )?.let {
                                    if (it.items.isNullOrEmpty()){
                                        withContext(Dispatchers.Main){
                                            showAnimations(false)
                                            emptyLayout()
                                            videoUniqueAdapter?.addData(it.items as ArrayList<SongItem>)
                                        }
                                        return@onSuccess
                                    }else{
                                        viewStateMap[key] = it
                                    }
                                }
//                                Log.e("video1",  viewStateMap[key].toString())
                                viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
                                    withContext(Dispatchers.Main){
                                        showAnimations(false)
                                        videoUniqueAdapter?.addData(it as ArrayList<SongItem>)

                                    }
                                }



                            }
                            .onFailure {
                                withContext(Dispatchers.Main){
                                    showAnimations(false)
                                    diconnectView()
                                }
//                                reportException(it)
                            }
                    }}
                else{
//                    viewStateMap[key]?.items?.filterIsInstance<SongItem>().isNullOrEmpty().apply {
//                        binding.emptyResult.visibility = View.VISIBLE
//                    }
//                    viewStateMap[key]?.items?.filterIsInstance<SongItem>()?.let {
//                        withContext(Dispatchers.Main){
//                            showAnimations(false)
//                            videoUniqueAdapter?.addData(it as ArrayList<SongItem>)
//
//                        }
//                    }
                }

            }
        }

        binding.tops.search.setOnClickListener {
            var isFirst = false
            var content  = ""
            if (TextUtils.isEmpty(binding.tops.searchEditText.text.toString())){
                Toast.makeText(this, "Please input content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                content =  binding.tops.searchEditText.text.toString()
                if (content == currentSearchContent){
                    return@setOnClickListener
                }
            }
            firebaseLog("search_page_search")

            insertSearchHistory()

            showAnimations(true)
            viewStateMap.clear()
            //search
            firebaseLog("search_page_search_artists")

            if (currentType != 0){
                when (currentType) {
                    1 ->{
                        binding.song.performClick()
                        return@setOnClickListener
                    }
                    2 ->{
                        binding.albums.performClick()
                        return@setOnClickListener
                    }
                    3 ->{
                        binding.artist.performClick()
                        return@setOnClickListener
                    }
                    4 ->{
                        binding.video.performClick()
                        return@setOnClickListener
                    }
                }
            }

            // 搜索关键词 "musi" 的顶级结果
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    for (filter in listOf(
                        FILTER_SONG,
                        FILTER_ALBUM,
                        FILTER_ARTIST,
                        FILTER_VIDEO
                    )) {
                        val searchResult = youTube.search(binding.tops.searchEditText.text.trim().toString(), filter).getOrNull()
                        searchResult ?: return@withContext
                        if(searchResult.items.isNotEmpty()){
                            currentSearchContent = binding.tops.searchEditText.text.toString()

                            withContext(Dispatchers.Main){
                                if (currentType == 0 && !isFirst ){
                                    isFirst = true
                                    setVisible(binding.allContainer,binding.songContainer, binding.artistContainer, binding.albumsContainer,binding.videoContainer)
                                    isShow( binding.songTitle)
                                    isShow( binding.songMore)
                                    isShow( binding.albumTitle)
                                    isShow( binding.albumMore)

                                    isShow( binding.artistTitle)
                                    isShow( binding.artistMore)

                                    isShow( binding.playlistTitle)
                                    isShow( binding.playListMore)

                                }
                                if (filter == FILTER_SONG){
                                    if (searchResult.items.filterIsInstance<SongItem>().isEmpty()){
                                        isGone(binding.songTitle)
                                        isGone(binding.songMore)
                                        isGone(binding.list)
                                    }
                                    if(searchResult.items.filterIsInstance<SongItem>().size >= 2){
                                        songAdapter?.addData(searchResult.items.filterIsInstance<SongItem>().take(2) as ArrayList<SongItem>)
                                    }else{
                                        songAdapter?.addData(searchResult.items.filterIsInstance<SongItem>() as ArrayList<SongItem>)
                                    }

                                }else if(filter == FILTER_ALBUM){
                                    if (searchResult.items.filterIsInstance<AlbumItem>().isEmpty()){
                                        isGone(binding.albumTitle)
                                        isGone(binding.albumList)
                                        isGone(binding.albumMore)

                                    }
                                    if(searchResult.items.filterIsInstance<AlbumItem>().size >= 2){
                                        searchViewAlbumAdapter?.addData(searchResult.items.filterIsInstance<AlbumItem>().take(2) as ArrayList<AlbumItem>)
                                    }else{
                                        searchViewAlbumAdapter?.addData(searchResult.items.filterIsInstance<AlbumItem>() as ArrayList<AlbumItem>)
                                    }
                                }else if(filter == FILTER_VIDEO){
                                    if (searchResult.items.filterIsInstance<SongItem>().isEmpty()){
                                        isGone(binding.playlistTitle)
                                        isGone(binding.playList)
                                        isGone(binding.playListMore)
                                    }
                                    if(searchResult.items.filterIsInstance<SongItem>().size >= 2){
                                        playAdapter?.addData(searchResult.items.filterIsInstance<SongItem>().take(2) as ArrayList<SongItem>)
                                    }else{
                                        playAdapter?.addData(searchResult.items.filterIsInstance<SongItem>() as ArrayList<SongItem>)
                                    }
                                }else if(filter == FILTER_ARTIST){
                                    if (searchResult.items.filterIsInstance<ArtistItem>().isEmpty()){
                                        isGone(binding.artistTitle)
                                        isGone(binding.artistList)
                                        isGone(binding.artistMore)
                                    }
                                    if(searchResult.items.filterIsInstance<ArtistItem>().size >= 2){
                                        searchViewArtAdapter?.addData(searchResult.items.filterIsInstance<ArtistItem>().take(2) as ArrayList<ArtistItem>)
                                    }else{
                                        searchViewArtAdapter?.addData(searchResult.items.filterIsInstance<ArtistItem>() as ArrayList<ArtistItem>)
                                    }
                                } else {

                                }
                            }
                        }else{
                            withContext(Dispatchers.Main){

                                if (filter == FILTER_SONG){
                                    if (searchResult.items.filterIsInstance<SongItem>().isEmpty()){
//                                    isGone(binding.songTitle)
//                                    isGone(binding.list)
                                    }
                                    songAdapter?.addData(searchResult.items.filterIsInstance<SongItem>() as ArrayList<SongItem>)

                                }else if(filter == FILTER_ALBUM){
                                    if (searchResult.items.filterIsInstance<AlbumItem>().isEmpty()){
//                                    isGone(binding.albumTitle)
//                                    isGone(binding.albumList)
                                    }
                                    searchViewAlbumAdapter?.addData(searchResult.items.filterIsInstance<AlbumItem>())
                                }else if(filter == FILTER_VIDEO){
                                    if (searchResult.items.filterIsInstance<SongItem>().isEmpty()){
//                                    isGone(binding.playlistTitle)
//                                    isGone(binding.playList)
                                    }

                                    playAdapter?.addData(searchResult.items.filterIsInstance<SongItem>()  as ArrayList<SongItem>)
                                }else if(filter == FILTER_ARTIST){
                                    if (searchResult.items.filterIsInstance<ArtistItem>().isEmpty()){
//                                    isGone(binding.artistTitle)
//                                    isGone(binding.artistList)
                                    }
                                    searchViewArtAdapter?.addData(searchResult.items.filterIsInstance<ArtistItem>())
                                } else {

                                }

                                showAnimations(false)
                            }

                        }

                    }

                    withContext(Dispatchers.Main){

                        if (currentType == 0){
                            if (binding.allContainer.visibility == View.GONE){
                                binding.allContainer.visibility = View.VISIBLE
                            }
                        }

                        showAnimations(false)
                    }
                }
            }
        }
    }

    private fun diconnectView() {
        binding.apply {
            emptyResult.visibility = View.VISIBLE
            emIv.setImageResource(R.mipmap.no_net)
            emTx.text = "There may be some network problems.\n" +
                    "You can try clicking the button to refresh."
            emoScan.visibility = View.VISIBLE
            emoScan.setOnClickListener {
//                Log.e("refresh","refresh")
                firebaseLog("search_page_refresh_search")
                binding.tops.search.performClick()
            }
            allContainer.visibility = View.GONE
            songContainer.visibility = View.GONE
            artistContainer.visibility = View.GONE
            albumsContainer.visibility = View.GONE
            videoContainer.visibility = View.GONE
        }
    }

    private fun isShow(songTitle: View) {
        if (songTitle.isGone){
            songTitle.visibility = View.VISIBLE
        }
    }

    private fun isGone(songTitle: View) {
        if (songTitle.isVisible){
            songTitle.visibility = View.GONE
        }
    }

    private fun insertSearchHistory() {
        if (binding.tops.searchEditText.text.toString().isEmpty()){
            return
        }
        try {
            data?.query {
                insert(SearchHistory(query = binding.tops.searchEditText.text.toString()))
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }





}