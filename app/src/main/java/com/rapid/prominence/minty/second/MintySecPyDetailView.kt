package com.rapid.prominence.minty.second

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.playListDetBanner
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.databinding.MintySecAlbumPageBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.roundOptions2
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.OnlinePlaylistViewModel
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.tencent.mmkv.MMKV
import com.zionhuang.innertube.models.WatchEndpoint
import kotlinx.coroutines.launch
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import timber.log.Timber

class MintySecPyDetailView : MintyBaseActivity() {

    private var playList: WatchEndpoint?= null
    private lateinit var binding: MintySecAlbumPageBinding
    private lateinit var newReleaseAlbumsAdapter: SecDetailSongsAdapter
    override fun onResume() {
        super.onResume()
        if (secPlayConnect != null && secPlayConnect?.player?.currentMetadata != null){
            secPlayConnect?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }
        loadSpeNative()
    }

    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)
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


    private fun loadSpeNative() {
        val normalBan: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            playListDetBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("playListDetBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("playListDetBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(playListDetBanner())) {
                LogUtils.e("playListDetBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(playListDetBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("playListDetBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("playListDetBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(playListDetBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(playlistDetInter(),this)

        binding = MintySecAlbumPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.topCon.root).init()
//        binding.playBtns.visibility = View.GONE
        val albumViewModel = intent?.getStringExtra("playlistId")?.let {
            OnlinePlaylistViewModel(it)
        }
        binding.loadings.post {
            binding.loadings.visibility = View.VISIBLE
            binding.loadings.startShimmerAnimation()
        }
        binding.topCon.apply {
            lists.visibility = View.VISIBLE
            searchEditText.setOnClickListener {
                startSearchPage()
            }
            lists.setOnClickListener {
                startQueuePage()
            }
            inputSearch.setOnClickListener {
                startSearchPage()
            }
            closeButton.setOnClickListener {
                finishCurrent()
            }
        }
        binding.playBtns.setOnClickListener {
            playList?.let {
                secPlayConnect?.playQueue(YouTubeQueue(it))
                startPlayerPage()
            }
        }
        binding.container.setOnClickListener {
            playList?.let {
                secPlayConnect?.playQueue(YouTubeQueue(it))
                startPlayerPage()
            }
        }
        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        lifecycleScope.launch {

//            withContext(Dispatchers.IO){
                albumViewModel?.playlist?.collect {
                    Timber.e(it.toString())
                    if (it == null) {
                        return@collect
                    }

                    binding.loadings.visibility = View.GONE
                    binding.loadings.stopShimmerAnimation()
                    // Set data to adapters
//                    withContext(Dispatchers.Main){
                        it?.apply {
                            it.shuffleEndpoint?.let { a->
                                playList = a
//                                binding.playBtns.visibility = View.VISIBLE
                            }
                            binding.titles.text = it.title
                            binding.artists.text = it.songCountText+" "
                                Glide.with(this@MintySecPyDetailView)
                                    .load(it.thumbnail)
                                    .apply(roundOptions2)
                                    .thumbnail(
                                        Glide.with(this@MintySecPyDetailView)
                                            .load(R.mipmap.default_det)
                                            .apply(roundOptions2)
                                    )
                                    .into(binding.thumbnailo)

                            albumViewModel?.playlistSongs?.collect {
                                Timber.e(it.toString())
                                if (it == null) {
                                    return@collect
                                }
                                // Set data to adapters
//                    withContext(Dispatchers.Main){
                                it?.apply {

                                    newReleaseAlbumsAdapter.submitList(it)
                                }
//                    }

                            }

                        }
//                    }

                }

//            }





}
        lifecycleScope.launch {

            secPlayConnect?.mediaMetadata?.collect{
                bottomSettingChange(it)
            }

        }
        lifecycleScope.launch {
            try{
                albumViewModel?.isError?.collect{
                    if (it) {
                        LogUtils.e("error")
                        if (isFinishing || isDestroyed){
                            return@collect
                        }
                        if (binding?.loadings?.isVisible == true){
                            binding?.loadings?.visibility = View.GONE
                            binding?.loadings?.stopShimmerAnimation()
                        }
                    }
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
        lifecycleScope.launch {
            secPlayConnect?.isPlaying?.collect{
                if (it){
                    binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.pause)
                }else{
                    binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.home_plays)
                }
            }
        }

//            }
        newReleaseAlbumsAdapter = SecDetailSongsAdapter(homeActivity = this@MintySecPyDetailView)

        binding.listRecycleview.layoutManager = LinearLayoutManager(this@MintySecPyDetailView)
        binding.listRecycleview.adapter = newReleaseAlbumsAdapter
        }
    
    
    
}
