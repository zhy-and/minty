package com.rapid.prominence.minty.ei
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.CustomViewsInfo
import com.rapid.prominence.minty.ei.main.EiMainPage.Companion.playerConnection
import com.rapid.prominence.minty.ei.scan.EiScanTools
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.databinding.MintyEiPlaysBinding
import com.rapid.prominence.minty.ei.kts.mmkvs.initCreateData
import com.rapid.prominence.minty.ei.kts.mmkvs.loadSongs
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.ei.kts.toMediaMetadata
import com.rapid.prominence.minty.ei.pages.EiMintyQueue
import com.rapid.prominence.minty.extensions.mediaItems
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.toggleRepeatMode
import com.rapid.prominence.minty.posBitmap
import com.tencent.mmkv.MMKV
import com.zhpan.bannerview.annotation.APageStyle
import com.zhpan.bannerview.constants.PageStyle
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class EiPlayer : MintyBaseActivity() {
  

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyEiPlaysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                binding.allImg.visibility = View.VISIBLE
            }else{
                binding.bannerView.visibility = View.VISIBLE
            }
            initView()
            initListener()
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    var isCollect = false
    var data : String?= null


    private val handler = Handler(Looper.getMainLooper())
    private var viewStatusProgressRunnable: Runnable? = null


    override fun onDestroy() {
        super.onDestroy()
        viewStatusProgressRunnable?.let {
            handler.removeCallbacks(it)
        }

    }
    private lateinit var binding: MintyEiPlaysBinding


    private fun loadAuth() {
        try {
            XXPermissions.with(this).permission(Permission.READ_MEDIA_AUDIO)
                .request { permissions, allGranted ->
                    if (allGranted) {
                        MMKV.defaultMMKV().putBoolean("isAuth",true)
                        val dialog =  showDialogs(this,R.layout.dialog_ei_pop)
                        binding?.root?.postDelayed({
                            if (dialog.isShowing && !isFinishing && !isDestroyed){
                                dialog.dismiss()
                            }
                        },1500)
                        val songs = EiScanTools.getLocalSongs(this) as ArrayList<Song>
                        if (songs.isNullOrEmpty()){
                            return@request
                        }
                        initCreateData(songs)
                        val data =  loadSongs(Song())
                        if (data.isNotEmpty()){
                            authGetLocalData(data)
                        }
                        finish()
                    }
                }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun authGetLocalData(localMusic: ArrayList<Song>) {
        val mediaItems = localMusic.map { music ->
            MediaItem.Builder()
                .setMediaId(music.id.toString())
                .setUri(Uri.parse(music.uri).buildUpon().appendPath(music.uriId.toString()).build())
                .setCustomCacheKey(music.id.toString())
                .setTag(music.toMediaMetadata())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(music.title)
                        .setSubtitle(music.artist)
                        .setArtist(music.artist)
//                        .setArtworkUri(music.albumArtUri.uri)
//                        .setAlbumTitle(music.album)
                        .setMediaType(MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()

        }
        if (playerConnection?.queueWindows != null && playerConnection?.queueWindows?.value?.isEmpty() == true){
            playerConnection?.addToQueue(mediaItems)
        }

    }
    private fun initListener() {


        binding.addFav?.setOnClickListener { v: View? ->
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                loadAuth()
                return@setOnClickListener
            }
//               playerConnection?.toggleLike()
//               viewStatusLikeButton(!isCollect)
            viewStatusCollectImages()

        }

        binding.seekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && playerConnection != null) {
                    viewStatusPositionText(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                viewStatusProgressRunnable?.let {
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
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                loadAuth()
                return@setOnClickListener
            }
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
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                loadAuth()
                return@setOnClickListener
            }
            try {
                if (playerConnection?.player?.repeatMode == REPEAT_MODE_OFF){
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
//                        val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
//                        viewStatusBannerToCurrentSong(currentIndex)

                    }
                    return@setOnClickListener
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

            if (playerConnection != null) {
                playerConnection?.seekToPrevious()
//                val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
//                viewStatusBannerToCurrentSong(currentIndex)
            }


        }

        binding.nextButton?.setOnClickListener { v: View? ->
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                loadAuth()
                return@setOnClickListener
            }
            try {
                if (playerConnection?.player?.repeatMode == REPEAT_MODE_OFF){
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
//                        val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
//                        viewStatusBannerToCurrentSong(currentIndex)
                    }
                    return@setOnClickListener
                }
            }catch (e : Exception){
                e.printStackTrace()
            }


            if (playerConnection != null) {
                playerConnection?.seekToNext()
//                val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
//                viewStatusBannerToCurrentSong(currentIndex)
            }
        }

        binding.repeatButton?.setOnClickListener { v: View? ->
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                loadAuth()
                return@setOnClickListener
            }
            if (playerConnection != null) {
                playerConnection?.player?.toggleRepeatMode()
            }
        }


    }

    private fun initView() {
        try {
            binding.titles.lists.setOnClickListener {
                if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                    loadAuth()
                    return@setOnClickListener
                }
                startInterActivity(Intent(this, EiMintyQueue::class.java), queueInter(),false)
            }
            binding.titles.inputSearch.visibility = View.GONE
            binding.titles.lists.visibility = View.VISIBLE
            binding.titles.closeButton.setImageResource(R.mipmap.backs)
            binding.titles.closeButton.setOnClickListener {
                finish()
            }
            binding.bannerView?.apply {
                setOffScreenPageLimit(3)
                registerLifecycleObserver(lifecycle)
                setAdapter(ViewBindingSampleAdapter(SizeUtils.dp2px(20f)))
                setIndicatorVisibility(View.GONE)
                setAutoPlay(false)
                setOnPageClickListener({ _: View, position: Int -> itemClick(position) }, true)
                setupBanner(
                    PageStyle.MULTI_PAGE_SCALE,
                    SizeUtils.dp2px(40f),
                )
                // 使用一个标志来避免在初始化时触发页面选择回调
                var isInitialPageSelection = true
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
                        musicPicts.add(CustomViewsInfo(posBitmap(index).toString(),window.mediaItem.metadata?.title))
                    }

                    binding.bannerView.refreshData(musicPicts) // 设置图片源
                    // 修复：在BannerView数据加载完成后，定位到当前正在播放的歌曲位置
                    val currentIndex = playerConnection?.currentWindowIndex?.value ?: 0
                    if (currentIndex in 0 until musicPicts.size) {
                        binding.bannerView.post {
                            binding.bannerView.setCurrentItem(currentIndex, false)
                        }
                    }
//                lifecycleScope.launch {
//                    playerConnection?.player?.currentMediaItem?.let {
//                        LogUtils.e("media","${it.metadata?.thumbnailUrl}  ${it.metadata?.title}")
//
//                        musicPicts.forEachIndexed { index, customViewsInfo ->
//                            LogUtils.e("custom","${customViewsInfo.xBannerUrl}  ${customViewsInfo.xBannerTitle}")
//                            if (customViewsInfo.xBannerUrl ==  it.metadata?.thumbnailUrl && customViewsInfo.xBannerTitle == it.metadata?.title
//                                    ){
//                                LogUtils.e("select","${customViewsInfo.xBannerUrl}  ${customViewsInfo.xBannerTitle}")
////                                viewStatusCurrentSongPosition(index)
//                                binding.bannerView.postDelayed({
//                                    binding.bannerView.setCurrentItem(index, false)
//                                },500)
//                            }
//                        }
//                    }
//                }


//                viewStatusBannerImage(it[0].mediaItem!!)

                }
            }
            checkCollect()
            // 监听播放状态变化
            playerConnection?.player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    viewStatusProgress()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
//                    Log.e("---onIsPlayingChanged",""+isPlaying)
//                    togglePlayback()
                    viewStatusPlayPauseButton()
                    if (isPlaying) {
                        startProgressUpdate()
                    } else {
                        viewStatusProgressRunnable?.let {
                            handler.removeCallbacks(it)
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    LogUtils.e(" -------- change ------")
                   val currentIndex =  playerConnection?.currentWindowIndex?.value ?: -1
                    if (currentIndex >= 0 && currentIndex < musicPicts.size){
                        viewStatusBannerToCurrentSong(currentIndex)
                    }
                }
                override fun onRepeatModeChanged(repeatMode: Int) {
                    viewStatusRepeatButton(repeatMode)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                }

                override fun onMediaMetadataChanged(metadata: MediaMetadata) {
//                    Log.e("---onIsPlayingChanged","onMediaMetadataChanged")
                    checkCollect()
                    viewStatusMediaInfo()
//                    resetAnimation()
//                    togglePlayback()
                }
            })

            // 初始化UI状态
            viewStatusPlayPauseButton()
            viewStatusRepeatButton(playerConnection?.player?.repeatMode!!)
            viewStatusMediaInfo()
            viewStatusProgress()
            startProgressUpdate()
        }catch (e : Exception){
            e.printStackTrace()
        }

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
    private var queueList = mutableListOf<Timeline.Window>()
    private var musicPicts = mutableListOf<CustomViewsInfo>()
    private fun itemClick(position: Int) {

    }
    private fun setupBanner(@APageStyle pageStyle: Int, revealWidth: Int) {
        setupBanner(pageStyle, revealWidth, revealWidth)
    }
    // 新增方法：根据当前播放歌曲索引更新 XBanner 的位置
    private fun viewStatusBannerToCurrentSong(position: Int) {
            binding.bannerView.setCurrentItem(position, true)
    }
    private fun checkCollect() {
        try {
            if (isFinishing || isDestroyed){
                return
            }
            var isCan = false
            loadSongs(Song()).forEach {
                if (it.isCollect && it.title == playerConnection?.player?.currentMediaItem?.mediaMetadata?.title){
                    isCan = true
                    return@forEach
                }
            }
            currentCollect = isCan
            viewStatusLikeButton(isCan)
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    var currentCollect = false

    private fun viewStatusCollectImages() {
        if (isFinishing || isDestroyed){
            return
        }
        currentCollect = !currentCollect
        viewStatusLikeButton(currentCollect)
        val data =  loadSongs(Song())
        data.forEach {
            if (it.title == playerConnection?.player?.currentMediaItem?.mediaMetadata?.title){
                it.isCollect = currentCollect
                return@forEach
            }
        }
        initCreateData(data)
    }

    private fun viewStatusPlayPauseButton() {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        val isPlaying = playerConnection?.player?.isPlaying ?: return
        //        playPauseButton.setIma😎geResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        binding.playPauseButton?.setImageResource(if (isPlaying) R.mipmap.local_pause else R.mipmap.lo_play)
    }

    private fun viewStatusRepeatButton(repeatMode: Int) {
        if ( isFinishing || isDestroyed) return
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                binding. repeatButton?.setImageResource(R.mipmap.random)
            }

            Player.REPEAT_MODE_ONE -> {
                binding. repeatButton?.setImageResource(R.mipmap.single)
            }

            Player.REPEAT_MODE_ALL -> {
                binding. repeatButton?.setImageResource(R.mipmap.list_loop)
            }
        }
    }



    private fun viewStatusMediaInfo() {
        try {
            if (playerConnection == null || isFinishing || isDestroyed) return

            playerConnection?.mediaMetadata?.value?.let {
                binding. title?.text = it.title
                binding.titles.nowPlayingText?.text = it.title
                binding.artist?.text = it?.artists?.joinToString(", ") { it.name }
            }
        }catch (e : Exception){
            e.printStackTrace()
        }


    }

    private fun viewStatusLikeButton(isLiked: Boolean) {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        binding.addFav?.setImageResource(if (isLiked) R.mipmap.py_collect else R.mipmap.uncollect)
    }

    private fun viewStatusProgress() {
        try {
            if (playerConnection == null || isFinishing || isDestroyed) return

            val duration = playerConnection?.player?.duration ?: return
            val position = playerConnection?.player?.currentPosition ?: return
            if (duration > 0) {
                binding. seekBar?.max = duration.toInt()
                binding.seekBar?.progress = position.toInt()
                viewStatusDurationText(duration)
                viewStatusPositionText(position)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun startProgressUpdate() {
        try {
            if (playerConnection == null   || isFinishing || isDestroyed) return
            viewStatusProgressRunnable?.let {
                handler.removeCallbacks(it)
            }
            viewStatusProgressRunnable = object : Runnable {
                override fun run() {
                    viewStatusProgress()
                    handler.postDelayed(this, 1000)
                }
            }
            viewStatusProgressRunnable?.let {
                handler.post(it)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun viewStatusPositionText(position: Long) {
        if (playerConnection == null   || isFinishing || isDestroyed) return
        binding.positionText?.text = formatDuration(position)
    }

    private fun viewStatusDurationText(duration: Long) {
        if (playerConnection == null   || isFinishing || isDestroyed) return
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
        viewStatusProgressRunnable?.let {
            handler.removeCallbacks(it)
        }
    }


    override fun onResume() {
        super.onResume()
        if (playerConnection != null && playerConnection?.player?.isPlaying == true) {
            startProgressUpdate()

        }
    }




}