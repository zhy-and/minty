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
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.entities.AlbumWithSongs
import com.rapid.prominence.minty.databinding.MintySecAlbumPageBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.logClickEvents
import com.rapid.prominence.minty.roundOptions2
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.AlbumViewModel
import com.rapid.prominence.minty.service.queues.LocalAlbumRadio
import kotlinx.coroutines.launch
import timber.log.Timber

class MintySecAlbumDetailPage : MintyBaseActivity() {

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
    }





    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            closeButton.setOnClickListener {
                finishCurrent()
            }
        }
        binding.playBtns.setOnClickListener {
            logClickEvents("album_det_play_btn")
            albumSongs?.let {
                secPlayConnect?.playQueue(LocalAlbumRadio(it))
               startPlayerPage()
            }
        }

        binding.container.setOnClickListener {
            logClickEvents("album_det_play_btn")
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
            albumViewModel?.albumWithSongs?.collect {
                if (it == null){
                    return@collect
                }
                albumSongs = it
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
