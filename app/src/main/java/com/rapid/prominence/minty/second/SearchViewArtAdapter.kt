package com.rapid.prominence.minty.second

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.roundOptions
import com.zionhuang.innertube.models.ArtistItem

class SearchViewArtAdapter(var songs: List<ArtistItem>, private val onSongClick: (ArtistItem) -> Unit) :
    RecyclerView.Adapter<SearchViewArtAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
    }
    fun addData(songs: List<ArtistItem>){
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_artist, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        song.thumbnail?.let {
            Glide.with(holder.itemView)
                .load(it)
                .apply(roundOptions)
                .thumbnail(
                    Glide.with(holder.itemView)
                        .load(posBitmap(position))
                        .apply(roundOptions)
                )
                .into(holder.ivgs)
        }

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount(): Int = songs.size
}