package com.rapid.prominence.minty.second

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.launch

import android.widget.*
import androidx.core.view.isVisible
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.homeMoodDetInter
import com.rapid.prominence.minty.cons.homeMoodDetailBanner
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.SongEntity
import com.rapid.prominence.minty.databinding.MintySecMoodDetailBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.toMediaItem
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.zionhuang.innertube.models.*
import com.zionhuang.innertube.pages.BrowseResult
import com.rapid.prominence.minty.second.databean.YouTubeBrowseViewModel
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool

class MintySecMdDetailPage : MintyBaseActivity() {
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
            homeMoodDetailBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("homeMoodDetailBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("homeMoodDetailBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(homeMoodDetailBanner())) {
                LogUtils.e("homeMoodDetailBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(homeMoodDetailBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("homeMoodDetailBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("homeMoodDetailBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(homeMoodDetailBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private lateinit var binding: MintySecMoodDetailBinding
    private lateinit var newReleaseAlbumsAdapter: YouTubeBrowseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(homeMoodDetInter(),this)


        binding = MintySecMoodDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.tops.root).init()
        try {
            val broId = intent?.getStringExtra("browseId")
            val params = intent?.getStringExtra("params")
            broId ?: return
            params ?: return
            val albumViewModel =
                YouTubeBrowseViewModel(this, broId, params)
            binding.loadings.post {
                binding.loadings.visibility = View.VISIBLE
                binding.loadings.startShimmerAnimation()
            }
            newReleaseAlbumsAdapter = YouTubeBrowseAdapter(emptyList())
            binding.listRecycleview.layoutManager = LinearLayoutManager(this)
            binding.listRecycleview.adapter = newReleaseAlbumsAdapter
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
            lifecycleScope.launch {
                albumViewModel.result.collect { browseResult ->
                    if (isFinishing || isDestroyed) {
                        return@collect
                    }
                    binding.loadings.visibility = View.GONE
                    binding.loadings.stopShimmerAnimation()

                    if (browseResult == null) {
                        // Show shimmer loading
                    } else {
                        newReleaseAlbumsAdapter?.addData(browseResult.items)
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

        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    companion object {
        const val TYPE_TITLE = 0
        const val TYPE_ITEM = 1
    }

    inner class YouTubeBrowseAdapter(var sections: List<BrowseResult.Item>) :
        RecyclerView.Adapter<YouTubeBrowseAdapter.ItemViewHolder>() {


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): YouTubeBrowseAdapter.ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.minty_item_mood_dt_1, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val title = sections[position].title
            holder.itemTitleTextView.text = title

            sections[position]?.items?.apply {
               holder.list.layoutManager = LinearLayoutManager(this@MintySecMdDetailPage)
                holder.list.adapter = ItemsAdapter(this)
            }

        }

        override fun getItemCount(): Int = sections.size
        fun addData(browseResult: List<BrowseResult.Item>) {
            this.sections = browseResult
            notifyDataSetChanged()
        }


        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.title_text_view)
            val list: RecyclerView = itemView.findViewById(R.id.mood_item_list)
        }


    }

    inner class ItemsAdapter(private val items: List<YTItem>) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.title)
            val artist: TextView = itemView.findViewById(R.id.title2)
            val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
            val ivgs: ImageView = itemView.findViewById(R.id.ivgs)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivgs.visibility = View.GONE
            holder.title.text = when (item) {
                is SongItem -> item.title
                is AlbumItem -> item.title
                is ArtistItem -> item.title
                is PlaylistItem -> item.title
                else -> ""
            }

            item.thumbnail?.let {
                Glide.with(holder.itemView)
                    .load(it)
                    .apply(options)
                    .thumbnail(
                        Glide.with(holder.itemView)
                            .load(posBitmap(position))
                            .apply(options)
                    )
                    .into(holder.thumbnail)
            }
            holder.itemView.setOnClickListener {
                when (item) {
                    is SongItem -> {
                        holder.ivgs.visibility = View.VISIBLE
                        secPlayConnect?.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        startInterActivity(Intent(this@MintySecMdDetailPage, SecPlayer::class.java), playViewsInter(),false)
                    }
                    is AlbumItem ->   {
                        startInterActivity(Intent(this@MintySecMdDetailPage, MintySecAlbumDetailPage::class.java).apply {
                            putExtra("albumId",item.id)
                        }, albumDetInter(),false)
                    }
                    is ArtistItem -> {
                        startInterActivity(Intent(this@MintySecMdDetailPage, MintySecArtistDetailPage::class.java).apply {
                            putExtra("artistId",item.id)
                        }, artistDetInter(),false)
                    }
                    is PlaylistItem -> {
                        startInterActivity(Intent(this@MintySecMdDetailPage, MintySecPyDetailView::class.java).apply {
                            putExtra("playlistId",item.id)
                        }, playlistDetInter(),false)
                    }
                }
            }
            holder.ivgs.setOnClickListener{
                val data = item as? SongItem ?: return@setOnClickListener
                try {
                    showDialogs(holder.itemView.context,R.layout.minty_dialog_popup,true, Gravity.BOTTOM).apply {
                        val text =   findViewById<TextView>(R.id.titles4)
                        var database : MusicDatabase?= null
                        App.instance?.let {
                            database = InternalDatabase.newInstance(it)
                            database?.transaction {
                                lifecycleScope.launch {
                                    song(item.id).first().let {
                                            its ->
                                        if (its == null){

                                            return@let
                                        }
                                        its?.apply {
                                            if (song.liked){
                                                text?.text = getString(R.string.dialog_remove_fav)
                                            }
                                        }
                                    }
                                }

                            }
                        }
                        findViewById<View>(R.id.play_now)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            secPlayConnect?.playQueue(radio(data.toMediaMetadata()))
                            startPlayerPage()
                        }
                        findViewById<View>(R.id.play_next)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            secPlayConnect?.playNext(data.toMediaItem())
                        }
                        findViewById<View>(R.id.add_queue)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            secPlayConnect?.addToQueue(data.toMediaItem())
                        }
                        findViewById<View>(R.id.add_fav)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            database?.transaction {
                                lifecycleScope.launch {
                                    song(item.id).first().let {
                                            its ->
//                                        Log.e("test",it.toString())
                                        if (its == null){
                                            withContext(Dispatchers.IO){
                                                insert(item.toMediaMetadata(), SongEntity::toggleLike)
                                            }
                                            return@let
                                        }
                                        its?.apply {
                                            withContext(Dispatchers.IO){
                                                update(song.toggleLike())
                                            }
                                        }
                                    }
                                }

                            }
                            if (text?.text.toString().contains("Remove")){
                                ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER,0,0).show("Removed from favourite")
                            }else{
                                ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER,0,0).show("Added to favourite")
                            }


                        }
                    }

                }catch (e : Exception){
                    e.printStackTrace()
                }
            }

        }

        override fun getItemCount(): Int = items.size
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




}
