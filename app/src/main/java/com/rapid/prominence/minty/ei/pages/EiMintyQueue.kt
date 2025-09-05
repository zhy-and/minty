package com.rapid.prominence.minty.ei.pages

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Timeline
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.ei.main.EiMainPage.Companion.eiPlays
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.cons.queueListBanner
import com.rapid.prominence.minty.databinding.MintyQueueBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.mediaItems
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.ei.EiPlayer
import com.rapid.prominence.minty.roundOptions

import kotlinx.coroutines.launch
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool

class EiMintyQueue : MintyBaseActivity() {
    override fun onResume() {
        super.onResume()
        if (eiPlays != null && eiPlays?.player?.currentMetadata != null){
            eiPlays?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }
        loadSpeNative()
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
            }
        }
    }
    private lateinit var binding: MintyQueueBinding
    private lateinit var newReleaseAlbumsAdapter: ItemsAdapter
    private fun bottomSettingChange(metadata: MediaMetadata?) {
        if (!TextUtils.isEmpty(metadata?.title)){
            binding.mintyIncContainerPlay.title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            binding.mintyIncContainerPlay.artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        Glide.with(this@EiMintyQueue)
            .load(metadata?.thumbnailUrl)
            .apply(roundOptions)
            .thumbnail(
                Glide.with(this)
                    .load(posBitmap(0))
                    .apply(roundOptions)
            )
            .into(binding.mintyIncContainerPlay.thumbnail)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(queueInter(),this)
        binding = MintyQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.titles.apply {
            inputSearch.visibility = View.GONE
            nowPlayingText.text = "Queue"
            lists.visibility = View.GONE
            closeButton.setOnClickListener {
                finish()
            }
        }
        newReleaseAlbumsAdapter = ItemsAdapter()

        binding.mintyIncContainerPlay.container.setOnClickListener {
            startActivity(Intent(this, EiPlayer::class.java))
        }
        binding.listRecycleview.layoutManager = LinearLayoutManager(this@EiMintyQueue)
        binding.listRecycleview.adapter = newReleaseAlbumsAdapter
        lifecycleScope.launch {
            eiPlays?.queueWindows?.value?.let {
                if (it.isNullOrEmpty()){
                    return@launch
                }
//                Log.e("------",""+it.size)
                try {
                    val arrayList = ArrayList<Timeline.Window>()
                    it.forEach { one ->
                        arrayList.add(one)
                    }
                    newReleaseAlbumsAdapter?.addData(arrayList)
                }catch (e : Exception){
                    e.printStackTrace()
                }

            }

        }
        lifecycleScope.launch {
            eiPlays?.isPlaying?.collect{
                if (it){
                    binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.pause)
                }else{
                    binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.home_plays)
                }
            }
        }


    }
    inner class ItemsAdapter() : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.queue_ei_item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            item.mediaItem.metadata?.let {
                holder.itemTitleTextView.text = it.title
                holder.subtitle.text = item.mediaItem.metadata?.artists?.joinToString { it.name }
                Glide.with(holder.itemView.context).asBitmap().load(posBitmap(position))
                    .apply(RequestOptions().transform(RoundedCorners(SizeUtils.dp2px(6f))))
                    .into(holder.ivgs)
            }
            holder.delete.setOnClickListener {
                if (items?.size == 1){
                    ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER,0,0).show("Only one song left, please add more!")
                    return@setOnClickListener
                }
                eiPlays?.player?.mediaItems?.forEachIndexed { index, mediaItem ->
                    if (mediaItem.mediaMetadata.title == item.mediaItem.metadata?.title) {
                        eiPlays?.player?.removeMediaItem(index)
                    }
                    }
                items.remove(item)
                notifyDataSetChanged()
            }

            holder.itemView.setOnClickListener {
                if (position == eiPlays?.currentWindowIndex?.value) {
                    eiPlays?.player?.togglePlayPause()
                } else {
                    eiPlays?.player?.seekToDefaultPosition(item.firstPeriodIndex)
                    eiPlays?.player?.playWhenReady = true
                }
                startActivity(Intent(this@EiMintyQueue, EiPlayer::class.java))
            }

        }

        override fun getItemCount(): Int = items.size

    }

}
