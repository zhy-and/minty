package com.rapid.prominence.minty.second

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.homeReleaseDetailBanner
import com.rapid.prominence.minty.cons.releaseDetailInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintySecReleaseDetailBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.zionhuang.innertube.models.AlbumItem
import com.rapid.prominence.minty.second.databean.NewReleaseViewModel
import com.rapid.prominence.minty.utils.joinByBullet
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import timber.log.Timber

class MintyReDetailPage : MintyBaseActivity() {
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
            homeReleaseDetailBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("homeReleaseDetailBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("homeReleaseDetailBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(homeReleaseDetailBanner())) {
                LogUtils.e("homeReleaseDetailBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(homeReleaseDetailBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("homeReleaseDetailBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("homeReleaseDetailBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(homeReleaseDetailBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private lateinit var binding: MintySecReleaseDetailBinding
    private lateinit var newReleaseAlbumsAdapter: ItemsAdapter
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(releaseDetailInter(),this)

        binding = MintySecReleaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.tops.root).init()
        val albumViewModel =
            NewReleaseViewModel(this, InternalDatabase.newInstance(this)

            )
        binding.tops.apply {
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
        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        binding.loadings.post {
            binding.loadings.visibility = View.VISIBLE
            binding.loadings.startShimmerAnimation()
        }
        newReleaseAlbumsAdapter = ItemsAdapter(emptyList())
        binding.listRecycleview.layoutManager = GridLayoutManager(this, 2)
        binding.listRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 1f),2))
        binding.listRecycleview.adapter = newReleaseAlbumsAdapter
        lifecycleScope.launch {

                albumViewModel?.newReleaseAlbums?.collect {
                    Timber.e(it.toString())
                    if (isFinishing || isDestroyed){
                        return@collect
                    }
                    binding.loadings.visibility = View.GONE
                    binding.loadings.stopShimmerAnimation()
                    it?.let {
                        its ->
                        newReleaseAlbumsAdapter?.addData(its)
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
    inner class ItemsAdapter( var items: List<AlbumItem>) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.title)
            val artist: TextView = itemView.findViewById(R.id.artist)
            val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_album, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
          holder.itemTitleTextView.text = item.title
          holder.artist.text =  joinByBullet(item.artists?.joinToString { it.name })

            Glide.with(this@MintyReDetailPage)
                .load(item?.thumbnail)
                .apply(options)
                .thumbnail(
                    Glide.with(this@MintyReDetailPage)
                        .load(randomBitmap())
                        .apply(options)
                )
                .into(holder.ivgs)

            holder.itemView.setOnClickListener {
                startInterActivity(Intent(this@MintyReDetailPage, MintySecAlbumDetailPage::class.java).apply {
                    putExtra("albumId",item.id)
                }, albumDetInter(),false)
            }

        }

        override fun getItemCount(): Int = items.size
        fun addData(its: List<AlbumItem>) {
            items = its
            notifyDataSetChanged()
        }
    }




}
