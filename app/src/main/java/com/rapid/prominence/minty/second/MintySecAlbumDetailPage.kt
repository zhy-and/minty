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
import com.rapid.prominence.minty.cons.albumDetBanner
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.entities.AlbumWithSongs
import com.rapid.prominence.minty.databinding.MintySecAlbumPageBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.roundOptions2
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.AlbumViewModel
import com.rapid.prominence.minty.service.queues.LocalAlbumRadio
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import timber.log.Timber

class MintySecAlbumDetailPage : MintyBaseActivity() {
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
            albumDetBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("albumDetBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("albumDetBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(albumDetBanner())) {
                LogUtils.e("albumDetBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(albumDetBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("albumDetBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("albumDetBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(albumDetBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private var albumSongs: AlbumWithSongs? = null
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

    private  var secReleaseAlbumAdapter: SecReleaseAlbumAdapter?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(albumDetInter(),this)

        binding = MintySecAlbumPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.topCon.root).init()

        val albumViewModel = intent?.getStringExtra("albumId")?.let {
            AlbumViewModel(
                InternalDatabase.newInstance(this), it

            )
        }
        binding.topCon.apply {
            lists.visibility = View.VISIBLE
            lists.setOnClickListener {
                startQueuePage()
            }

            inputSearch.setOnClickListener {
                startSearchPage()
            }
            searchEditText.setOnClickListener {
                startSearchPage()
            }
            closeButton.setOnClickListener {
                finishCurrent()
            }
        }
        secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@MintySecAlbumDetailPage)

        binding.playBtns.setOnClickListener {
            firebaseLog("album_det_play_btn")
            albumSongs?.let {
                secPlayConnect?.playQueue(LocalAlbumRadio(it))
               startPlayerPage()
            }
        }
        binding.recRecycleview.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false)
        binding.recRecycleview.adapter = secReleaseAlbumAdapter
        binding.container.setOnClickListener {
            firebaseLog("album_det_play_btn")
            albumSongs?.let {
                secPlayConnect?.playQueue(LocalAlbumRadio(it))
               startPlayerPage()
            }
        }
        binding.loadings.post {
            binding.loadings.visibility = View.VISIBLE
            binding.loadings.startShimmerAnimation()
        }
        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        lifecycleScope.launch {
//              database.quickPicks()
//                    .first().shuffled().take(20).forEach {
//                        Log.e("quickPicks-----",it.toString())
//                  }
//
//                val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
//
//                database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
//                    .first().shuffled().take(10).forEach {
//                        Log.e("keep -----",it.toString())
//                    }

                albumViewModel?.songLists?.collect {
                    Timber.e(it.toString())
                    if (it == null) {
                        return@collect
                    }
                    if (isFinishing || isDestroyed) {
                        return@collect
                    }
                    binding.container.visibility = View.VISIBLE
                    binding.loadings.visibility = View.GONE
                    binding.loadings.stopShimmerAnimation()
                    binding.titles.text = it.firstOrNull()?.album?.name
                    binding.artists.text =  it.firstOrNull()?.artists?.joinToString(", ") { it.name }
                            Glide.with(this@MintySecAlbumDetailPage)
                                .load(it.firstOrNull()?.thumbnail)
                                .apply(roundOptions2)
                                .thumbnail(
                                    Glide.with(this@MintySecAlbumDetailPage)
                                        .load(R.mipmap.default_det)
                                        .apply(roundOptions2)
                                )
                                .into(binding.thumbnailo)

                    // Set data to adapters
                        it?.apply {
                            newReleaseAlbumsAdapter = SecDetailSongsAdapter(true,albumSongs,this@MintySecAlbumDetailPage)
                            binding.listRecycleview.layoutManager = LinearLayoutManager(this@MintySecAlbumDetailPage)
                            binding.listRecycleview.adapter = newReleaseAlbumsAdapter
                            newReleaseAlbumsAdapter?.submitList(this)
                        }

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
            albumViewModel?.albumWithSongs?.collect {
                if (it == null){
                    return@collect
                }
                albumSongs = it
            }
        }
        lifecycleScope.launch {
            try {
                albumViewModel?.otherVersions?.collect{
                    if (it.isNullOrEmpty() || isDestroyed || isFinishing){
                        return@collect
                    }
                    binding.newReleaseTitle.visibility = View.VISIBLE
                    it?.apply {
                        secReleaseAlbumAdapter?.submitList(this)
                    }
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
        lifecycleScope.launch {

            secPlayConnect?.mediaMetadata?.collect{
                bottomSettingChange(it)
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


    }
}
