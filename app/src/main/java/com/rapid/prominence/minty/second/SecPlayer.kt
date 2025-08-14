package com.rapid.prominence.minty.second

import com.rapid.prominence.minty.second.transformer.AdvancedBannerTransformer
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.rapid.prominence.minty.application.App.Companion.isNextPlay
import com.rapid.prominence.minty.CustomViewsInfo
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.collectInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintyEiPlaysBinding
import com.rapid.prominence.minty.ei.BindingAdapter
import com.rapid.prominence.minty.extensions.collectLatest
import com.rapid.prominence.minty.extensions.logClickEvents
import com.rapid.prominence.minty.extensions.mediaItems
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.toggleRepeatMode
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.playerConnection
import com.rapid.prominence.minty.second.view.favorite.MintySecFavPage
import com.zhpan.bannerview.annotation.APageStyle
import com.zhpan.bannerview.constants.PageStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class SecPlayer : MintyBaseActivity() {
    override fun onDestroy() {
        super.onDestroy()
        updateProgressRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    private lateinit var binding: MintyEiPlaysBinding

    override fun onResume() {
        super.onResume()
        if (playerConnection != null && playerConnection?.player?.isPlaying == true) {
            startProgressUpdate()
        }
    }
    private fun randomPlay(){
        try {

            var flag = 0
            playerConnection?.player?.mediaItems?.apply {
                forEachIndexed() {  index,its ->
                    if (its?.mediaMetadata?.title == playerConnection?.player?.currentMediaItem?.mediaMetadata?.title){
                        flag = index
                    }
                }
                var index =  Random.nextInt(this.size)
                if (index == flag){
                    index =  Random.nextInt(this.size)
                }
                playerConnection?.player?.seekToDefaultPosition(index)
                playerConnection?.player?.playWhenReady = true

            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyEiPlaysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            if (playerConnection == null || playerConnection?.mediaMetadata == null || playerConnection?.mediaMetadata?.value == null){
                binding.allImg.visibility = View.VISIBLE
            }else{
                binding.bannerView.visibility = View.VISIBLE
            }
            lifecycleScope.launch {
                // 更新收藏状态
                playerConnection?.currentSong?.collectLatest(scope){
                    isCollect = it?.song?.liked ?: false
//                    Log.e("status ----",""+it?.song?.liked)
                    updateLikeButton(isCollect)
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
                    logClickEvents("player_to_search")

                    startSearchPage()
                }
                searchEditText.setOnClickListener {
                    logClickEvents("player_to_search")

                    startSearchPage()
                }
                lists.setOnClickListener {
                    logClickEvents("player_to_queue")

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
            if (playerConnection == null ||  isFinishing || isDestroyed) return
            lifecycleScope.launch {
                playerConnection?.queueWindows?.collect {
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
                    val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
                    if (currentIndex in 0 until musicPicts.size) {
                        binding.bannerView.post {
                            binding.bannerView.setCurrentItem(currentIndex, false)
                        }
                    }


                }
            }
            observePlayerState()
            setupListeners()
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    private var queueList = mutableListOf<Timeline.Window>()
    private var musicPicts = mutableListOf<CustomViewsInfo>()
    var isCollect = false
    var data : String?= null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            lifecycleScope.launch {
                playerConnection?.currentSong?.collectLatest(scope) {
                        value ->
                    updateLikeButton(                       value?.song?.liked
                            == true)
                }
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    private val handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
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
        LogUtils.e(""+position+"viewStatusCurrentSongPosition")
//        binding.xbanner.setBannerCurrentItem(currentSongPosition, false)
        val currentIndex = playerConnection?.currentWindowIndex?.value ?: -1
        if (position == currentIndex) {
            // 位置未改变，不需要操作
            return
        }

        // 只有当位置真正改变时才切换歌曲
        playerConnection?.player?.seekToDefaultPosition(position)
        playerConnection?.player?.playWhenReady = true
//        if (position == playerConnection?.currentWindowIndex?.value) {
//            if (playerConnection?.isPlaying?.value == false){
//                playerConnection?.player?.play()
//            }
//        } else {
//            playerConnection?.player?.seekToDefaultPosition(songs[position].lastPeriodIndex)
//            playerConnection?.player?.playWhenReady = true
//        }
    }
    private fun setupListeners() {

        var database = InternalDatabase.newInstance(this@SecPlayer)
        binding.addFav?.setOnClickListener {
            try {
                logClickEvents("player_collect_btn")

                if (playerConnection == null || binding.title.text.contains(getString(R.string.app_name))){
                    startInterActivity(Intent(this, MintySecFavPage::class.java), collectInter(),false)
                    return@setOnClickListener
                }
                database.transaction {
                    lifecycleScope.launch {
                        playerConnection?.currentSong?.first().let {
                            it?.song?.apply {
                                withContext(Dispatchers.IO){
                                    update(toggleLike())
                                }
                                updateLikeButton(!it.song.liked)
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
                if (fromUser && playerConnection != null) {
                    updatePositionText(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                updateProgressRunnable?.let {
                    handler.removeCallbacks(it)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (playerConnection != null) {
                    playerConnection?.player?.seekTo(seekBar.progress.toLong())
                    startProgressUpdate()
                }
            }
        })

        binding.playPauseButton?.setOnClickListener { v: View? ->
            if (playerConnection == null || binding.title.text.contains(getString(R.string.app_name))){
                startQueuePage()
                logClickEvents("player_empty_click_play_btn")

                return@setOnClickListener
            }
            logClickEvents("player_play_btn")

            if (playerConnection != null) {
                if (playerConnection?.player?.playbackState == Player.STATE_ENDED) {
                    playerConnection?.player?.seekTo(0)
                    playerConnection?.player?.playWhenReady = true
                } else {
                    playerConnection?.player?.playWhenReady =
                        !playerConnection?.player?.playWhenReady!!
                }
            }
        }

        binding.previousButton?.setOnClickListener { v: View? ->
            try {
                if (playerConnection == null || binding.title.text.contains(getString(R.string.app_name))){
                    startQueuePage()
                    return@setOnClickListener
                }
                if (playerConnection?.player?.repeatMode == REPEAT_MODE_OFF && !isNextPlay){
                    randomPlay()
                    return@setOnClickListener
                }
                if (playerConnection != null) {
                    playerConnection?.seekToPrevious()
                }
                logClickEvents("player_previous_btn")
            }catch (e : Exception){
                e.printStackTrace()
            }


        }

        binding.nextButton?.setOnClickListener { v: View? ->
            try {
                if (playerConnection == null || binding.title.text.contains(getString(R.string.app_name))){
                    startQueuePage()
                    return@setOnClickListener
                }
                if (playerConnection?.player?.repeatMode == REPEAT_MODE_OFF && !isNextPlay){
                    randomPlay()
                    return@setOnClickListener
                }
                if (playerConnection != null) {
                    playerConnection?.seekToNext()
                }
                logClickEvents("player_next_btn")
            }catch (e : Exception){
                e.printStackTrace()
            }


        }

        binding.repeatButton?.setOnClickListener { v: View? ->
            if (playerConnection == null || binding.title.text.contains(getString(R.string.app_name))){
                startQueuePage()
                return@setOnClickListener
            }
            if (playerConnection != null) {
                playerConnection?.player?.toggleRepeatMode()
            }
            logClickEvents("player_repeat_btn")

        }


    }
    private var scope = CoroutineScope(Dispatchers.Main) + Job()

    private fun observePlayerState() {
        if (playerConnection == null) return

        // 监听播放状态变化
        playerConnection?.player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
//                    Log.e("onPlaybackStateChanged","---"+playbackState)
                updatePlayPauseButton()
                updateProgress()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                    val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING
                    if (isBufferingOrReady) {
                        try {
                            if (binding.anims.isInvisible){
                                binding.anims.visibility = View.VISIBLE
                                binding.anims.playAnimation()
                            }

                        }catch (e : Exception){
                            e.printStackTrace()
                        }
                    }else if(player.playbackState == Player.STATE_READY){
                        if (binding.anims.isVisible){
                            binding.anims.visibility = View.INVISIBLE
                            binding.anims.cancelAnimation()
                        }
                    }

//
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
//                    togglePlayback()

                updatePlayPauseButton()
                if (isPlaying && playerConnection?.isPlaying?.value == true) {
                    startProgressUpdate()
                } else {
                    updateProgressRunnable?.let {
                        handler.removeCallbacks(it)
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateRepeatButton(repeatMode)
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
//                    if (isLoading){
//                        try {
//                            binding.anims.visibility = View.VISIBLE
//                            binding.anims.playAnimation()
//                        }catch (e : Exception){
//                            e.printStackTrace()
//                        }
////
//                    }else{
//                        binding.anims.visibility = View.INVISIBLE
//                        binding.anims.cancelAnimation()
//                    }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                try{
                    LogUtils.e(" -------- change ------")
                    val currentIndex =  playerConnection?.currentWindowIndex?.value ?: -1
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
                updateMediaInfo()
            }
        })

        // 初始化UI状态
        updatePlayPauseButton()
        updateRepeatButton(playerConnection?.player?.repeatMode!!)
        updateMediaInfo()
        updateProgress()
        startProgressUpdate()
    }

    private fun updatePlayPauseButton() {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        val isPlaying = playerConnection?.player?.isPlaying ?: return
        binding.playPauseButton?.setImageResource(if (isPlaying) R.mipmap.local_pause else R.mipmap.lo_play)
    }

    private fun updateRepeatButton(repeatMode: Int) {
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
    private fun updateMediaInfo() {
        try {
            if (playerConnection == null || isFinishing || isDestroyed) return

            playerConnection?.mediaMetadata?.value?.let {
                binding.title?.text = it.title
                binding.artist?.text = it?.artists?.joinToString(", ") { it.name }


            }
        }catch (e : Exception){
            e.printStackTrace()
        }


    }

    private fun updateLikeButton(isLiked: Boolean) {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        binding.addFav?.setImageResource(if (isLiked) R.mipmap.py_collect else R.mipmap.uncollect)
    }

    private fun updateProgress() {
        try {
            if (playerConnection == null || isFinishing || isDestroyed) return

            val duration = playerConnection?.player?.duration ?: return
            val position = playerConnection?.player?.currentPosition ?: return
            if (duration > 0) {
                binding. seekBar?.max = duration.toInt()
                binding.seekBar?.progress = position.toInt()
                updateDurationText(duration)
                updatePositionText(position)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    private fun startProgressUpdate() {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        updateProgressRunnable?.let {
            handler.removeCallbacks(it)
        }
        updateProgressRunnable = object : Runnable {
            override fun run() {
                updateProgress()
                handler.postDelayed(this, 1000)
            }
        }
        updateProgressRunnable?.let {
            handler.post(it)
        }
    }

    private fun updatePositionText(position: Long) {
        binding.positionText?.text = formatDuration(position)
    }

    private fun updateDurationText(duration: Long) {
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
        updateProgressRunnable?.let {
            handler.removeCallbacks(it)
        }

    }






}