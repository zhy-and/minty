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
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.ei.main.EiMainPage.Companion.playerConnection
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.databinding.MintyQueueBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.mediaItems
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.ei.EiPlayer
import com.rapid.prominence.minty.roundOptions

import kotlinx.coroutines.launch

class EiMintyQueue : MintyBaseActivity() {
    override fun onResume() {
        super.onResume()
        if (playerConnection != null && playerConnection?.player?.currentMetadata != null){
            playerConnection?.mediaMetadata?.let {
                updatePlayerUI(it.value)
            }
        }
    }
    private lateinit var binding: MintyQueueBinding
    private lateinit var newReleaseAlbumsAdapter: ItemsAdapter
    private fun updatePlayerUI(metadata: MediaMetadata?) {
        if (!TextUtils.isEmpty(metadata?.title)){
            binding.bottomPlayer.title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            binding.bottomPlayer.artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        Glide.with(this@EiMintyQueue)
            .load(metadata?.thumbnailUrl)
            .apply(roundOptions)
            .thumbnail(
                Glide.with(this)
                    .load(posBitmap(0))
                    .apply(roundOptions)
            )
            .into(binding.bottomPlayer.thumbnail)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.titles.apply {
            nowPlayingText.text = "Queue"
            lists.visibility = View.GONE
            closeButton.setOnClickListener {
                finish()
            }
        }
        newReleaseAlbumsAdapter = ItemsAdapter()

        binding.bottomPlayer.container.setOnClickListener {
            startActivity(Intent(this, EiPlayer::class.java))
        }
        binding.moodAndGenresRecyclerView.layoutManager = LinearLayoutManager(this@EiMintyQueue)
        binding.moodAndGenresRecyclerView.adapter = newReleaseAlbumsAdapter
        lifecycleScope.launch {
            playerConnection?.queueWindows?.value?.let {
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
            playerConnection?.isPlaying?.collect{
                if (it){
                    binding.bottomPlayer.playPauseButton.setImageResource(R.mipmap.pause)
                }else{
                    binding.bottomPlayer.playPauseButton.setImageResource(R.mipmap.home_plays)
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
                playerConnection?.player?.mediaItems?.forEachIndexed { index, mediaItem ->
                    if (mediaItem.mediaMetadata.title == item.mediaItem.metadata?.title) {
                        playerConnection?.player?.removeMediaItem(index)
                    }
                    }
                items.remove(item)
                notifyDataSetChanged()
            }

            holder.itemView.setOnClickListener {
                if (position == playerConnection?.currentWindowIndex?.value) {
                    playerConnection?.player?.togglePlayPause()
                } else {
                    playerConnection?.player?.seekToDefaultPosition(item.firstPeriodIndex)
                    playerConnection?.player?.playWhenReady = true
                }
                startActivity(Intent(this@EiMintyQueue, EiPlayer::class.java))
            }

        }

        override fun getItemCount(): Int = items.size

    }

}
