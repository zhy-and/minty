package com.rapid.prominence.minty.second

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zionhuang.innertube.models.SongItem
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.AlbumWithSongs
import com.rapid.prominence.minty.database.entities.SongEntity
import com.rapid.prominence.minty.databinding.ItemSecOneSongBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.toMediaItem
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.service.queues.LocalAlbumRadio
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.zionhuang.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecDetailSongsAdapter(
    var isGone: Boolean = false,
    var index: AlbumWithSongs? = null,
    var homeActivity : MintyBaseActivity
) : RecyclerView.Adapter<SecDetailSongsAdapter.NewReleaseAlbumViewHolder>() {

    private var albums = listOf<SongItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewReleaseAlbumViewHolder {
        val binding = ItemSecOneSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewReleaseAlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewReleaseAlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album,position)
    }

    override fun getItemCount(): Int = albums.size

    fun submitList(newAlbums: List<SongItem>) {
        albums = newAlbums
        notifyDataSetChanged()
    }

    inner class NewReleaseAlbumViewHolder(private val binding: ItemSecOneSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: SongItem, position: Int) {
            if (isGone){
                binding.thumbnail.visibility  = View.GONE
            }else{
                if (binding.thumbnail.isGone){
                    binding.thumbnail.visibility  = View.VISIBLE
                }
                Glide.with(itemView)
                    .load(album?.thumbnail)
                    .apply(options)
                    .thumbnail(
                        Glide.with(itemView)
                            .load(randomBitmap())
                            .apply(options)
                    )
                    .into(binding.thumbnail)
            }
           binding?.title?.text = album.title
           binding?.title2?.text = album.artists.firstOrNull()?.name

            binding?.ivgs?.setOnClickListener {
                try {
                    showDialogs(binding.ivgs.context, R.layout.minty_dialog_popup,true, Gravity.BOTTOM).apply {
                        val text =   findViewById<TextView>(R.id.titles4)
                        var database : MusicDatabase?= null
                        App.instance?.let {
                            database = InternalDatabase.newInstance(it)
                            database?.transaction {
                                lifecycleScope.launch {
                                    song(album.id).first().let {
                                            its ->
                                        if (its == null){

                                            return@let
                                        }
                                        its?.apply {
                                            if (song.liked){
                                                text?.text = "Remove from favourite"
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
                            secPlayConnect?.playQueue(radio(album.toMediaMetadata()))
                            if (homeActivity?.isFinishing == true || homeActivity?.isDestroyed == true){
                                return@setOnClickListener
                            }
                            homeActivity?.startInterActivity(Intent(homeActivity, SecPlayer::class.java), playViewsInter(),false)
                        }
                        findViewById<View>(R.id.play_next)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            secPlayConnect?.playNext(album.toMediaItem())
                        }
                        findViewById<View>(R.id.add_queue)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            secPlayConnect?.addToQueue(album.toMediaItem())
                        }
                        findViewById<View>(R.id.add_fav)?.setOnClickListener {
                            if (isShowing){
                                dismiss()
                            }
                            database?.transaction {
                                lifecycleScope.launch {
                                    song(album.id).first().let {
                                            its ->
//                                        Log.e("test",it.toString())
                                        if (its == null){
                                            withContext(Dispatchers.IO){
                                                insert(album.toMediaMetadata(), SongEntity::toggleLike)
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
                                ToastUtils.showShort("Removed from favourite")
                            }else{
                                ToastUtils.showShort("Added to favourite")
                            }

                        }
                    }
                }catch (e : Exception){
                    e.printStackTrace()
                }
            }
            binding?.all?.setOnClickListener {
                if (album.id == secPlayConnect?.mediaMetadata?.value?.id) {
                    secPlayConnect?.player?.togglePlayPause()
                } else {
                    if (index == null){
                        secPlayConnect?.playQueue(
                            YouTubeQueue(album.endpoint?: WatchEndpoint(videoId = album.id),album.toMediaMetadata())
                        )
                    }else{
                        secPlayConnect?.playQueue(
                            LocalAlbumRadio(index!!,position)
                        )
                    }

                }
                if (homeActivity?.isFinishing == true || homeActivity?.isDestroyed == true){
                    return@setOnClickListener
                }
                homeActivity?.startInterActivity(Intent(homeActivity, SecPlayer::class.java), playViewsInter(),false)
            }

        }}
}
