package com.rapid.prominence.minty.second

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.zionhuang.innertube.models.PlaylistItem

class SecPlayLbAdapter(var songs: List<PlaylistItem>, private val onSongClick: (PlaylistItem) -> Unit) :
    RecyclerView.Adapter<SecPlayLbAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.title2)
        val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
        val menu: ImageView = itemView.findViewById(R.id.ivgs)
    }
    fun addData(songs: List<PlaylistItem>){
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_playlist, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.author?.name
        song.thumbnail?.let {
            Glide.with(holder.itemView)
                .load(it)
                .apply(options)
                .thumbnail(
                    Glide.with(holder.itemView)
                        .load(posBitmap(position))
                        .apply(options)
                )
                .into(holder.ivgs)
        }

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount(): Int = songs.size
}