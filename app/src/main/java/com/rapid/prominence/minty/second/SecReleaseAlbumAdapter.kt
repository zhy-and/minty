package com.rapid.prominence.minty.second

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.databinding.MintyItemReleaseAlbumPageBinding
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.utils.joinByBullet
import com.zionhuang.innertube.models.AlbumItem

class SecReleaseAlbumAdapter(var homeActivity: MintyBaseActivity) : RecyclerView.Adapter<SecReleaseAlbumAdapter.NewReleaseAlbumViewHolder>() {

    private var albums = listOf<AlbumItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewReleaseAlbumViewHolder {
        val binding = MintyItemReleaseAlbumPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewReleaseAlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewReleaseAlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)
    }

    override fun getItemCount(): Int = albums.size

    fun submitList(newAlbums: List<AlbumItem>) {
        albums = newAlbums
        notifyDataSetChanged()
    }

    inner class NewReleaseAlbumViewHolder(private val binding: MintyItemReleaseAlbumPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: AlbumItem) {
           binding?.artist?.text = joinByBullet(album.artists?.joinToString { it.name }, album.year?.toString())
           binding?.title?.text = album.title
            binding.container?.setOnClickListener {
                if (homeActivity?.isFinishing == true || homeActivity?.isDestroyed == true){
                    return@setOnClickListener
                }
                homeActivity?.startInterActivity(Intent(homeActivity, MintySecAlbumDetailPage::class.java).apply {
                    putExtra("albumId",album.id)
                }, albumDetInter(),false)
                firebaseLog("home_release_to_album_detail")

            }


           album?.thumbnail?.let {
                   it ->
               Glide.with(itemView)
                   .load(it)
                   .apply(options)
                   .thumbnail(
                       Glide.with(itemView)
                           .load(randomBitmap())
                           .apply(options)
                   )
                   .into(binding.thumbnail)
               }
           }


        }
}
