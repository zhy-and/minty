package com.rapid.prominence.minty.second.search

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.StringUtils.getString
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.SongEntity
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.toMediaItem
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.SecPlayer
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.zionhuang.innertube.models.SongItem
import io.supercharge.shimmerlayout.ShimmerLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongAdapter(var songs: ArrayList<SongItem> = arrayListOf(), private val onSongClick: (SongItem) -> Unit) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.title2)
        val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
        val menu: ImageView = itemView.findViewById(R.id.ivgs)
        val spec_anm: ShimmerLayout? = itemView.findViewById(R.id.spec_anm)
    }
    fun addData(songs: ArrayList<SongItem>){
        this.songs = songs
        notifyDataSetChanged()
    }
    fun removeLoadItem() {
        if (songs.isEmpty() || songs[songs.size - 1].duration!= 999999999) return

        val lastIdx = songs.size - 1
        songs.removeAt(lastIdx)
        notifyItemChanged(lastIdx)
    }

    fun addLoadItem() {
        if (songs.size > 0 && songs[songs.size - 1].duration == 999999999) {
            return
        }
        val footItem = SongItem("","", emptyList(), null,999999999,"",false,null)
        songs.add(footItem)
        notifyItemInserted(songs.size - 1)
    }

//    override fun getItemViewType(position: Int): Int {
//        return position
//    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
    val view = if (viewType == 999999999) {
        LayoutInflater.from(parent.context).inflate(R.layout.minty_sec_it_los, parent, false)
    } else {
          LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_song, parent, false)
    }
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val item = songs[position]

        holder.menu.visibility = View.VISIBLE
        holder.menu.setOnClickListener {
            try {
                showDialogs(holder.itemView.context, R.layout.minty_dialog_popup,true,Gravity.BOTTOM).apply {
                    val text =   findViewById<TextView>(R.id.titles4)
                    var database : MusicDatabase?= null
                    App.instance?.let {
                        database = InternalDatabase.newInstance(it)
                        database?.transaction {
                            lifecycleScope.launch {
                                songSnapshot(item.id).let {
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
                        secPlayConnect?.playQueue(radio(item.toMediaMetadata()))
                        ActivityUtils.startActivity(Intent(holder.itemView.context, SecPlayer::class.java))
                    }
                    findViewById<View>(R.id.play_next)?.setOnClickListener {
                        if (isShowing){
                            dismiss()
                        }
                        secPlayConnect?.playNext(item.toMediaItem())
                    }
                    findViewById<View>(R.id.add_queue)?.setOnClickListener {
                        if (isShowing){
                            dismiss()
                        }
                        secPlayConnect?.addToQueue(item.toMediaItem())
                    }
                    findViewById<View>(R.id.add_fav)?.setOnClickListener {
                        if (isShowing){
                            dismiss()
                        }
                        database?.transaction {
                            lifecycleScope.launch {
                                songSnapshot(item.id).let {
                                        its ->
//                                    Log.e("test",it.toString())
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
        if (item.duration != 999999999){
            holder.title.text = item.title
            holder.artist.text = item.artists.joinToString(", ") { it.name }
            item.thumbnail?.let {
                Glide.with(holder.itemView)
                    .load(it)
                    .encodeQuality(30)
                    .apply(options)
                    .thumbnail(
                        Glide.with(holder.itemView)
                            .load(posBitmap(position))
                            .apply(options)
                    )
                    .into(holder.ivgs)
            }

            holder.itemView.setOnClickListener { onSongClick(item) }
        }else{
            holder?.spec_anm?.let {
                it.startShimmerAnimation()
            }
            holder?.menu?.visibility = View.GONE
        }

    }
    override fun getItemViewType(position: Int): Int = songs[position].duration?.toInt() ?: 0
    override fun getItemCount(): Int = songs.size


}
