package com.rapid.prominence.minty.second

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.databinding.MintySecAlbumPageBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.logClickEvents
import com.rapid.prominence.minty.roundOptions2
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.OnlinePlaylistViewModel
import com.rapid.prominence.minty.service.queues.LocalAlbumRadio
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.zionhuang.innertube.models.WatchEndpoint
import kotlinx.coroutines.launch
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

    }

    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
