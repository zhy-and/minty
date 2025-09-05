package com.rapid.prominence.minty.second

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Timeline
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.cons.queueListBanner
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintyQueueBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.getQueueWindows
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.HomeViewModel
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import timber.log.Timber

class SecPlayDlView : MintyBaseActivity() {


    override fun onResume() {
        super.onResume()
        if (secPlayConnect != null && secPlayConnect?.player?.currentMetadata != null){
            secPlayConnect?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }
        loadSpeNative()
    }

    private var songAdapter: ItemsAdapter?= null
    private lateinit var binding: MintyQueueBinding
    private  var secReleaseAlbumAdapter: SecReleaseAlbumAdapter?= null
    private  var homeStyleAdapter: HomeStyleAdapter?= null

    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(queueInter(),this)
        binding = MintyQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        songAdapter = ItemsAdapter()
        binding.titles.apply {
            lists.visibility = View.VISIBLE
            lists.setImageResource(R.mipmap.white_search)
            nowPlayingText.text = "Queue"
            inputSearch.visibility = View.GONE
            closeButton.setOnClickListener {
                finishCurrent()
            }
            lists.setOnClickListener {
                startSearchPage()
            }

        }

        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        binding.listRecycleview.layoutManager = LinearLayoutManager(this@SecPlayDlView)
        binding.listRecycleview.adapter = songAdapter
        lifecycleScope.launch {
            if (secPlayConnect?.queueWindows?.value.isNullOrEmpty()){
                initEmptyLayout()
                return@launch
            }
            secPlayConnect?.queueWindows?.value?.let {
                if (it.isNullOrEmpty()){
                    initEmptyLayout()
                    return@launch
                }

                try {
                    val arrayList = ArrayList<Timeline.Window>()
                    it.forEach { one ->
                        arrayList.add(one)
                    }
                    if (arrayList.isNullOrEmpty()){
                        initEmptyLayout()
                        return@let
                    }
                    binding.listLayout.visibility = View.VISIBLE
                    binding.emptyLayout.visibility = View.GONE
                    songAdapter?.addData(arrayList)
                }catch (e : Exception){
                    e.printStackTrace()
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
            queueListBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("queueListBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("queueListBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(queueListBanner())) {
                LogUtils.e("queueListBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(queueListBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("queueListBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("queueListBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
                if(!EventsAppendTool.posIsCanShow(queueListBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        handler.removeMessages(MESSAGE_MSG_CODE)
                        handler.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }
    private fun initEmptyLayout() {
        binding.emptyLayout.visibility = View.VISIBLE
        binding.listLayout.visibility = View.GONE
        HomeViewModel(
            this@SecPlayDlView,
            InternalDatabase.newInstance(this@SecPlayDlView)
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
    //                                    binding.specAnm.visibility = View.GONE
    //                                    binding.specAnm.stopShimmerAnimation()

                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@SecPlayDlView)
                    homeStyleAdapter = HomeStyleAdapter(this@SecPlayDlView)
                    binding.newReleaseAlbumsRecyclerView.layoutManager = LinearLayoutManager(
                        this@SecPlayDlView,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.newReleaseAlbumsRecyclerView.adapter = secReleaseAlbumAdapter

                    binding.homeListRecycleview.layoutManager =
                        GridLayoutManager(this@SecPlayDlView, 2)
                    binding.homeListRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 10f),2))

                    binding.homeListRecycleview.adapter = homeStyleAdapter
                    binding.homeListRecycleview.isNestedScrollingEnabled = false
                    withContext(Dispatchers.Main) {
                        it?.newReleaseAlbums?.apply {
                            secReleaseAlbumAdapter?.submitList(this)
                        }
                        it?.moodAndGenres?.apply {
                            homeStyleAdapter?.submitList(this)
                        }
                    }
                    Timber.e(it.toString())
    //                        }
                }
            }
        }
    }

    inner class ItemsAdapter : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
        var items: ArrayList<Timeline.Window> = ArrayList()

        fun  addData(data: ArrayList<Timeline.Window>){
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
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
            item.mediaItem.metadata?.let {
                holder.itemTitleTextView.text = it.title
                holder.subtitle.text = item.mediaItem.metadata?.artists?.joinToString { it.name }
                Glide.with(holder.itemView)
                    .load(it.thumbnailUrl)
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
                    showDialogs(this@SecPlayDlView, R.layout.minty_dialog_sure_exit).apply {
                        findViewById<View>(R.id.cancel_button)?.setOnClickListener {
                            if (isShowing && !isDestroyed && !isFinishing){
                                dismiss()
                            }
                        }
                        findViewById<TextView>(R.id.titles)?.text = getString(R.string.is_delete_queue)
                        findViewById<View>(R.id.ok_btn)?.setOnClickListener {
                            if (isShowing && !isDestroyed && !isFinishing){
                                dismiss()
                            }
                            if (items?.size == 1){
                                ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER,0,0).show("Only one song left, please add more!")
                                return@setOnClickListener
                            }
//                        playerConnection?.player?.mediaItems?.forEachIndexed { index, mediaItem ->
//                            if (mediaItem.mediaMetadata.title == item.mediaItem.metadata?.title) {
//                                Log.e("-----f","fff"+item.firstPeriodIndex)
//                                playerConnection?.player?.removeMediaItem(index)
//                                items.remove(item)
//                                notifyDataSetChanged()
//                            }
//                        }
//                        items.sortedBy {
//                            -it.firstPeriodIndex
//                        }.forEach {
//                            its ->
                            secPlayConnect?.player?.removeMediaItem(item.firstPeriodIndex)
//                        }
                            try {
                                val arrayList = ArrayList<Timeline.Window>()
                                secPlayConnect?.player?.getQueueWindows()?.forEach { one ->
                                    arrayList.add(one)
                                }
                                addData(arrayList)
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


                if (position == secPlayConnect?.currentWindowIndex?.value) {
                    secPlayConnect?.player?.togglePlayPause()
                } else {


                    secPlayConnect?.player?.seekToDefaultPosition(items[position].firstPeriodIndex)
                    secPlayConnect?.player?.playWhenReady = true
                }
               startPlayerPage()
            }

        }

        override fun getItemCount(): Int = items.size

    }




}
