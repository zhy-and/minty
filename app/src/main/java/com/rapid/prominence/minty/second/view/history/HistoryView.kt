package com.rapid.prominence.minty.second.view.history

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd

import com.rapid.prominence.minty.second.databean.HistoryViewModel
import com.rapid.prominence.minty.second.databean.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.historyBanner
import com.rapid.prominence.minty.cons.historyInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.EventWithSong

import com.rapid.prominence.minty.databinding.MintyQueueBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.second.SecGridItemDecoration
import com.rapid.prominence.minty.second.HomeStyleAdapter
import com.rapid.prominence.minty.second.SecReleaseAlbumAdapter
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.utils.joinByBullet
import com.tencent.mmkv.MMKV
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool

class HistoryView : MintyBaseActivity() {
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
            historyBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("historyBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("historyBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(historyBanner())) {
                LogUtils.e("historyBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(historyBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("historyBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("historyBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(historyBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private var data: MusicDatabase?= null
    private lateinit var binding: MintyQueueBinding
    private  var historyAdapter: ItemsAdapter?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(historyInter(),this)

        binding = MintyQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
         data = InternalDatabase.newInstance(this@HistoryView)
        binding.titles.apply {
            inputSearch.visibility = View.GONE
            lists.visibility = View.VISIBLE
            lists.setImageResource(R.mipmap.white_search)
            nowPlayingText.text = "History"
            closeButton.setOnClickListener {
                finishCurrent()
            }
            lists.setOnClickListener {
                startSearchPage(true)
            }
        }
        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage(true)
        }
        binding.tvs.text = getString(R.string.no_song_history)
        lifecycleScope.launch {
            val viewModel =
                HistoryViewModel(data!!
                )
            viewModel.events.collect {
                if ( it.values.isNullOrEmpty()){
                    initEmptyLayout()
                    return@collect
                }
                val list = ArrayList<EventWithSong>()
                it.values?.forEach { its ->
                    list.addAll(its)
                }
                if (binding.emptyLayout.isVisible){
                    binding.emptyLayout.visibility = View.GONE
                }
                historyAdapter = ItemsAdapter(list)
                binding.listRecycleview.layoutManager = LinearLayoutManager(this@HistoryView)
                binding.listRecycleview.adapter = historyAdapter
            }
        }
        }

    private  var secReleaseAlbumAdapter: SecReleaseAlbumAdapter?= null
    private  var homeStyleAdapter: HomeStyleAdapter?= null
    private fun initEmptyLayout() {
        val datas = data ?: return
        binding.emptyLayout.visibility = View.VISIBLE
        HomeViewModel(
            this@HistoryView,
            datas
        ).apply {
            binding.specAnm.post {
                binding.specAnm.visibility = View.VISIBLE
                binding.specAnm.startShimmerAnimation()
            }
            lifecycleScope.launch {
                explorePage?.collect {
                    if (isFinishing || isDestroyed) {
                        return@collect
                    }
                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    if (secReleaseAlbumAdapter == null){
                        secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@HistoryView)
                        binding.newReleaseAlbumsRecyclerView.layoutManager = LinearLayoutManager(
                            this@HistoryView,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        binding.newReleaseAlbumsRecyclerView.adapter = secReleaseAlbumAdapter
                    }
                    if (homeStyleAdapter == null){
                        homeStyleAdapter = HomeStyleAdapter(this@HistoryView)
                        binding.homeListRecycleview.layoutManager =
                            GridLayoutManager(this@HistoryView, 2)

                        binding.homeListRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 10f),2))

                        binding.homeListRecycleview.adapter = homeStyleAdapter
                    }
                    withContext(Dispatchers.Main) {
                        it?.newReleaseAlbums?.apply {
                            secReleaseAlbumAdapter?.submitList(this)
                        }
                        it?.moodAndGenres?.apply {
                            homeStyleAdapter?.submitList(this)
                        }
                    }
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
    inner class ItemsAdapter(var items: ArrayList<EventWithSong>) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.title)
            val subtitle: TextView = itemView.findViewById(R.id.title2)
            val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
            val delete: ImageView = itemView.findViewById(R.id.ivgs)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.delete.setImageResource(R.mipmap.dete)
            val item = items[position]
            item?.song?.apply {
                holder.itemTitleTextView.text = title
//                holder.subtitle.text = joinByBullet(
//                    artists.joinToString { it.name },
//                    makeTimeString(song.duration * 1000L)
//                )
                                holder.subtitle.text = joinByBullet(
                    artists.joinToString { it.name }
                )
                Glide.with(holder.itemView)
                    .load(thumbnailUrl)
                    .apply(options)
                    .thumbnail(
                        Glide.with(holder.itemView)
                            .load(posBitmap(position))
                            .apply(options)
                    )
                    .into(holder.ivgs)
            }
            holder.delete.setOnClickListener {
                try {
                    showDialogs(this@HistoryView, R.layout.minty_dialog_sure_exit).apply {
                        findViewById<View>(R.id.cancel_button)?.setOnClickListener {
                            if (isShowing && !isDestroyed && !isFinishing){
                                dismiss()
                            }
                        }
                        findViewById<TextView>(R.id.titles)?.text = getString(R.string.is_delete_history)
                        findViewById<View>(R.id.ok_btn)?.setOnClickListener {
                            try {
                                if (isShowing && !isDestroyed && !isFinishing){
                                    dismiss()
                                }
                                items.remove(item)
                                notifyDataSetChanged()
                                Thread{
                                    data?.delete(item.event)
                                }.start()
                            }catch (e : Exception){
                                e.printStackTrace()
                            }

                        }
                    }

                }catch (e : Exception){
                    e.printStackTrace()
                }

            }

            holder.itemView.setOnClickListener {
                if (item.song.id == secPlayConnect?.mediaMetadata?.value?.id) {
                    secPlayConnect?.player?.togglePlayPause()
                } else {
                    secPlayConnect?.playQueue(
                        YouTubeQueue.radio(item.song.toMediaMetadata())
                    )
                }
                startPlayerPage(true)
            }

        }

        override fun getItemCount(): Int = items.size

    }







}
