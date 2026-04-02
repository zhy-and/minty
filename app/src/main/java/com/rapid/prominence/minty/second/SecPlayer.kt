package com.rapid.prominence.minty.second

import com.rapid.prominence.minty.second.transformer.AdvancedBannerTransformer
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.rapid.prominence.minty.application.App.Companion.isNextPlay
import com.rapid.prominence.minty.CustomViewsInfo
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.collectInter
import com.rapid.prominence.minty.cons.playViewBanner
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintyEiPlaysBinding
import com.rapid.prominence.minty.ei.BindingAdapter
import com.rapid.prominence.minty.extensions.collectLatest
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.extensions.mediaItems
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.toggleRepeatMode
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.view.favorite.MintySecFavPage
import com.tencent.mmkv.MMKV
import com.zhpan.bannerview.annotation.APageStyle
import com.zhpan.bannerview.constants.PageStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class SecPlayer : MintyBaseActivity() {
    private val adMessage: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                MESSAGE_MSG_CODE -> {
                    loadSpeNative()
                }
            }
        }
    }


    private fun loadSpeNative() {
        val normalBan: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            playViewBanner(), this)
        if (normalBan == null) {

            if (isFinishing || isDestroyed) {
                LogUtils.e("playViewBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(playViewBanner())) {
                LogUtils.e("playViewBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(playViewBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("playViewBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("playViewBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(playViewBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        adMessage.removeMessages(MESSAGE_MSG_CODE)
                        adMessage.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(playViewsInter(),this)
        binding = MintyEiPlaysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            firebaseLog("event_bp")
            if (secPlayConnect == null || secPlayConnect?.mediaMetadata == null || secPlayConnect?.mediaMetadata?.value == null){
                binding.allImg.visibility = View.VISIBLE
            }else{
                binding.bannerView.visibility = View.VISIBLE
            }
            lifecycleScope.launch {
                // 更新收藏状态
                secPlayConnect?.currentSong?.collectLatest(scope){
                    isCollect = it?.song?.liked ?: false
//                    Log.e("status ----",""+it?.song?.liked)
                    refreshLikeButton(isCollect)
                }
            }

            binding.titles.apply {
                lists.visibility = View.VISIBLE
                closeButton.setImageResource(R.mipmap.backs)
                nowPlayingText.visibility = View.GONE
                inputSearch.visibility = View.VISIBLE
                closeButton.setOnClickListener {
                    finishCurrent()
                }
                inputSearch.setOnClickListener {
                    firebaseLog("player_to_search")

                    startSearchPage()
                }
                searchEditText.setOnClickListener {
                    firebaseLog("player_to_search")

                    startSearchPage()
                }
                lists.setOnClickListener {
                    firebaseLog("player_to_queue")

                    startQueuePage()
                }
            }
            binding.bannerView?.apply {
                setOffScreenPageLimit(3)
                registerLifecycleObserver(lifecycle)
                setAdapter(BindingAdapter(SizeUtils.dp2px(20f)))
                setIndicatorVisibility(View.GONE)
                setAutoPlay(false)
                setOnPageClickListener({ _: View, position: Int -> itemClick(position) }, true)
                setupBanner(
                    PageStyle.MULTI_PAGE_SCALE,
                    SizeUtils.dp2px(40f),
                )
                // 使用一个标志来避免在初始化时触发页面选择回调
                var isInitialPageSelection = true
                setPageTransformer(AdvancedBannerTransformer())

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(i: Int, v: Float, i1: Int) {
                    }

                    override fun onPageSelected(i: Int) {
                        if (isInitialPageSelection) {
                            // 忽略初始化时的第一次回调
                            isInitialPageSelection = false
                            return
                        }
                        viewStatusCurrentSongPosition(i)
                    }

                    override fun onPageScrollStateChanged(i: Int) {
                    }
                })
            }
            if (secPlayConnect == null ||  isFinishing || isDestroyed) return
            lifecycleScope.launch {
                secPlayConnect?.queueWindows?.collect {
                    if (it.isNullOrEmpty()){
                        return@collect
                    }
                    if (it.size == queueList.size && it.size == musicPicts.size){
                        return@collect
                    }
                    queueList.clear()
                    musicPicts.clear()
                    LogUtils.e("refresh","${it.size}")
                    it.forEachIndexed { index, window ->
                        queueList.add(window)
                        musicPicts.add(CustomViewsInfo(window.mediaItem.metadata?.thumbnailUrl,window.mediaItem.metadata?.title))
                    }

                    binding.bannerView.refreshData(musicPicts) // 设置图片源
                    // 修复：在BannerView数据加载完成后，定位到当前正在播放的歌曲位置
                    val currentIndex = secPlayConnect?.currentWindowIndex?.value ?: 0
                    if (currentIndex in 0 until musicPicts.size) {
                        binding.bannerView.post {
                            binding.bannerView.setCurrentItem(currentIndex, false)
                        }
                    }


                }
            }
            refreshLoadingStatus(secPlayConnect?.playbackState?.value ?: Player.STATE_IDLE)
            observePlayerState()
            setupListeners()
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    private var queueList = mutableListOf<Timeline.Window>()
    override fun onDestroy() {
        super.onDestroy()
        seekMessage?.let {
            handler.removeCallbacks(it)
        }
        adMessage?.removeCallbacksAndMessages(null)
    }
    private lateinit var binding: MintyEiPlaysBinding

    override fun onResume() {
        super.onResume()
        if (secPlayConnect != null && secPlayConnect?.player?.isPlaying == true) {
            startProgressUpdate()
        }
        loadSpeNative()
    }
    private fun randomPlay(){
        try {

            var flag = 0
            secPlayConnect?.player?.mediaItems?.apply {
                forEachIndexed() {  index,its ->
                    if (its?.mediaMetadata?.title == secPlayConnect?.player?.currentMediaItem?.mediaMetadata?.title){
                        flag = index
                    }
                }
                var index =  Random.nextInt(this.size)
                if (index == flag){
                    index =  Random.nextInt(this.size)
                }
                secPlayConnect?.player?.seekToDefaultPosition(index)
                secPlayConnect?.player?.playWhenReady = true

            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }
    private var musicPicts = mutableListOf<CustomViewsInfo>()
    var isCollect = false
    var data : String?= null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            lifecycleScope.launch {
                secPlayConnect?.currentSong?.collectLatest(scope) {
                        value ->
                    refreshLikeButton(                       value?.song?.liked
                            == true)
                }
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    private val handler = Handler(Looper.getMainLooper())
    private var seekMessage: Runnable? = null
    private fun itemClick(position: Int) {

    }
    private fun setupBanner(
        @APageStyle pageStyle: Int,
        leftRevealWidth: Int,
        rightRevealWidth: Int
    ) {
        var vp = binding.bannerView?: return
        vp.setPageMargin(SizeUtils.dp2px(18f))
            .setScrollDuration(800)
            .setRevealWidth(leftRevealWidth, rightRevealWidth)
            .setPageStyle(pageStyle, 0.85f)
            .create()
    }
    private fun setupBanner(@APageStyle pageStyle: Int, revealWidth: Int) {
        setupBanner(pageStyle, revealWidth, revealWidth)
    }
    // 更新当前歌曲位置
    private fun viewStatusCurrentSongPosition(position: Int) {
//        LogUtils.e(""+position+"viewStatusCurrentSongPosition")
        val currentIndex = secPlayConnect?.currentWindowIndex?.value ?: -1
        if (position == currentIndex) {
            // 位置未改变，不需要操作
            return
        }

        // 只有当位置真正改变时才切换歌曲
        secPlayConnect?.player?.seekToDefaultPosition(position)
        secPlayConnect?.player?.playWhenReady = true

    }
    private fun setupListeners() {

        var database = InternalDatabase.newInstance(this@SecPlayer)
        binding.addFav?.setOnClickListener {
            try {
                firebaseLog("player_collect_btn")

                if (secPlayConnect == null || binding.title.text.contains(getString(R.string.app_name))){
                    startInterActivity(Intent(this, MintySecFavPage::class.java), collectInter(),false)
                    return@setOnClickListener
                }
                database.transaction {
                    lifecycleScope.launch {
                        secPlayConnect?.currentSong?.first().let {
                            it?.song?.apply {
                                withContext(Dispatchers.IO){
                                    update(toggleLike())
                                }
                                refreshLikeButton(!it.song.liked)
                            }
                        }
                    }

                }
            }catch (e : Exception){
                e.printStackTrace()
            }




        }

        binding.seekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && secPlayConnect != null) {
                    refreshPositionText(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekMessage?.let {
                    handler.removeCallbacks(it)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (secPlayConnect != null) {
                    secPlayConnect?.player?.seekTo(seekBar.progress.toLong())
                    startProgressUpdate()
                }
            }
        })

        binding.playPauseButton?.setOnClickListener { v: View? ->
            if (secPlayConnect == null || binding.title.text.contains(getString(R.string.app_name))){
                startQueuePage()
                firebaseLog("player_empty_click_play_btn")

                return@setOnClickListener
            }
            firebaseLog("player_play_btn")

            if (secPlayConnect != null) {
                if (secPlayConnect?.player?.playbackState == Player.STATE_ENDED) {
                    secPlayConnect?.player?.seekTo(0)
                    secPlayConnect?.player?.playWhenReady = true
                } else {
                    secPlayConnect?.player?.playWhenReady =
                        !secPlayConnect?.player?.playWhenReady!!
                }
            }
        }

        binding.previousButton?.setOnClickListener { v: View? ->
            try {
                if (secPlayConnect == null || binding.title.text.contains(getString(R.string.app_name))){
                    startQueuePage()
                    return@setOnClickListener
                }
                if (secPlayConnect?.player?.repeatMode == REPEAT_MODE_OFF && !isNextPlay){
                    randomPlay()
                    return@setOnClickListener
                }
                if (secPlayConnect != null) {
                    secPlayConnect?.seekToPrevious()
                }
                firebaseLog("player_previous_btn")
            }catch (e : Exception){
                e.printStackTrace()
            }


        }

        binding.nextButton?.setOnClickListener { v: View? ->
            try {
                if (secPlayConnect == null || binding.title.text.contains(getString(R.string.app_name))){
                    startQueuePage()
                    return@setOnClickListener
                }
                if (secPlayConnect?.player?.repeatMode == REPEAT_MODE_OFF && !isNextPlay){
                    randomPlay()
                    return@setOnClickListener
                }
                if (secPlayConnect != null) {
                    secPlayConnect?.seekToNext()
                }
                firebaseLog("player_next_btn")
            }catch (e : Exception){
                e.printStackTrace()
            }


        }

        binding.repeatButton?.setOnClickListener { v: View? ->
            if (secPlayConnect == null || binding.title.text.contains(getString(R.string.app_name))){
                startQueuePage()
                return@setOnClickListener
            }
            if (secPlayConnect != null) {
                secPlayConnect?.player?.toggleRepeatMode()
            }
            firebaseLog("player_repeat_btn")

        }


    }
    private var scope = CoroutineScope(Dispatchers.Main) + Job()

    private fun observePlayerState() {
        if (secPlayConnect == null) return

        // 监听播放状态变化
        secPlayConnect?.player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                refreshPlayPauseButton()
                refreshProgress()
                refreshLoadingStatus(playbackState)
            }

          
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshPlayPauseButton()
                if (isPlaying && secPlayConnect?.isPlaying?.value == true) {
                    startProgressUpdate()
                } else {
                    seekMessage?.let {
                        handler.removeCallbacks(it)
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                refreshRepeatButton(repeatMode)
            }


            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                try{
//                    LogUtils.e(" -------- change ------")
                    val currentIndex =  secPlayConnect?.currentWindowIndex?.value ?: -1
                    if (currentIndex >= 0 && currentIndex < musicPicts.size){
                        viewStatusBannerToCurrentSong(currentIndex)
                    }
                }catch (e : Exception){
                    e.printStackTrace()
                }

            }
            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                binding.positionText.text = "0:00"
                binding.seekBar.progress = 0
//                    resetAnimation()
//                    togglePlayback()
                refreshMediaInfo()
            }
        })

        // 初始化UI状态
        refreshPlayPauseButton()
        refreshRepeatButton(secPlayConnect?.player?.repeatMode!!)
        refreshMediaInfo()
        refreshProgress()
        startProgressUpdate()
    }

    private fun refreshLoadingStatus(playbackState: Int) {
        val isBufferingOrReady = playbackState == Player.STATE_BUFFERING
        if (isBufferingOrReady) {
            try {
                if (binding.anims.isInvisible) {
                    binding.anims.visibility = View.VISIBLE
                    binding.anims.playAnimation()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (playbackState == Player.STATE_READY) {
            if (binding.anims.isVisible) {
                binding.anims.visibility = View.INVISIBLE
                binding.anims.cancelAnimation()
            }
        }
    }

    private fun refreshPlayPauseButton() {
        if (secPlayConnect == null   || isFinishing || isDestroyed) return
        val isPlaying = secPlayConnect?.player?.isPlaying ?: return
        binding.playPauseButton?.setImageResource(if (isPlaying) R.mipmap.local_pause else R.mipmap.lo_play)
    }

    private fun refreshRepeatButton(repeatMode: Int) {
        if ( isFinishing || isDestroyed) return
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> {
//                playerConnection?.player?.shuffleModeEnabled = true
                binding. repeatButton?.setImageResource(R.mipmap.random)
            }

            Player.REPEAT_MODE_ONE -> {
//                playerConnection?.player?.shuffleModeEnabled = false
                binding. repeatButton?.setImageResource(R.mipmap.single)
            }

            Player.REPEAT_MODE_ALL -> {
//                playerConnection?.player?.shuffleModeEnabled = false
                binding. repeatButton?.setImageResource(R.mipmap.list_loop)
            }
        }
    }

    // 新增方法：根据当前播放歌曲索引更新 XBanner 的位置
    private fun viewStatusBannerToCurrentSong(position: Int) {
        if (binding.bannerView.currentItem == position){
            return
        }
        binding.bannerView.setCurrentItem(position, true)
    }
    private fun refreshMediaInfo() {
        try {
            if (secPlayConnect == null || isFinishing || isDestroyed) return

            secPlayConnect?.mediaMetadata?.value?.let {
                binding.title?.text = it.title
                binding.artist?.text = it?.artists?.joinToString(", ") { it.name }


            }
        }catch (e : Exception){
            e.printStackTrace()
        }


    }

    private fun refreshLikeButton(isLiked: Boolean) {
        if (secPlayConnect == null   || isFinishing || isDestroyed) return
        binding.addFav?.setImageResource(if (isLiked) R.mipmap.py_collect else R.mipmap.uncollect)
    }

    private fun refreshProgress() {
        try {
            if (secPlayConnect == null || isFinishing || isDestroyed) return

            val duration = secPlayConnect?.player?.duration ?: return
            val position = secPlayConnect?.player?.currentPosition ?: return
            if (duration > 0) {
                binding. seekBar?.max = duration.toInt()
                binding.seekBar?.progress = position.toInt()
                refreshDurationText(duration)
                refreshPositionText(position)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    private fun startProgressUpdate() {
        if (secPlayConnect == null   || isFinishing || isDestroyed) return
        seekMessage?.let {
            handler.removeCallbacks(it)
        }
        seekMessage = object : Runnable {
            override fun run() {
                refreshProgress()
                handler.postDelayed(this, 1000)
            }
        }
        seekMessage?.let {
            handler.post(it)
        }
    }

    private fun refreshPositionText(position: Long) {
        binding.positionText?.text = formatDuration(position)
    }

    private fun refreshDurationText(duration: Long) {
        binding.durationText?.text = formatDuration(duration)
    }

    private fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        seekMessage?.let {
            handler.removeCallbacks(it)
        }
        if (adMessage?.hasMessages(MESSAGE_MSG_CODE) == true){
            adMessage?.removeMessages(MESSAGE_MSG_CODE)
        }
    }


}