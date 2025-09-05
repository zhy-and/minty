package com.rapid.prominence.minty.second

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.databinding.MintyItemSecHomeSecBinding
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.pages.HomePage

class SecMainLayoutAdapter(var mintySecMainPage: MintySecMainPage) : RecyclerView.Adapter<SecMainLayoutAdapter.HomePageSectionViewHolder>() {

    private var sections = listOf<HomePage.Section>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePageSectionViewHolder {
        val binding = MintyItemSecHomeSecBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomePageSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomePageSectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section)
    }

    override fun getItemCount(): Int = sections.size

    fun submitList(newSections: List<HomePage.Section>) {
        sections = newSections
        notifyDataSetChanged()
    }

    inner class HomePageSectionViewHolder(private val binding: MintyItemSecHomeSecBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(section: HomePage.Section) {
            binding.title.text = section.title
//            Log.e("label----",section.label.toString())
//            section.thumbnail?.let { thumbnailUrl ->
//                Glide.with(binding.root.context).asBitmap().load(thumbnailUrl).into(binding.thumbnail)
//            }

            binding.itemsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            val adapter = SecHomeDetailAdapter(
                onItemClickListener = { item ->
                    when (item) {
                        is SongItem -> {
//                            Log.e("-------type","song")
                            secPlayConnect?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                            if (mintySecMainPage?.isFinishing == true || mintySecMainPage?.isDestroyed == true){
                                return@SecHomeDetailAdapter
                            }
                            firebaseLog("home_to_player")
                            mintySecMainPage?.startInterActivity(Intent(mintySecMainPage, SecPlayer::class.java), playViewsInter(),false)
//                            playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        }
//                            playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        is AlbumItem ->{
//                            Log.e("-------type","album")
                            if (mintySecMainPage?.isFinishing == true || mintySecMainPage?.isDestroyed == true){
                                return@SecHomeDetailAdapter
                            }
                            firebaseLog("home_to_album_det")

                            mintySecMainPage?.startInterActivity(Intent(mintySecMainPage, MintySecAlbumDetailPage::class.java).apply {
                                putExtra("albumId",item.id)
                            }, albumDetInter(),false)
                        }
//                            navController.navigate("album/${item.id}")
                        is ArtistItem -> {
//                            Log.e("-------type","artist")
                            if (mintySecMainPage?.isFinishing == true || mintySecMainPage?.isDestroyed == true){
                                return@SecHomeDetailAdapter
                            }
                            firebaseLog("home_to_artist_det")

                            mintySecMainPage?.startInterActivity(Intent(mintySecMainPage, MintySecArtistDetailPage::class.java).apply {
                                putExtra("artistId",item.id)
                            }, artistDetInter(),false)

                        }
//                            navController.navigate("artist/${item.id}")
                        is PlaylistItem -> {
//                            Log.e("-------type","PlaylistItem")

                            if (mintySecMainPage?.isFinishing == true || mintySecMainPage?.isDestroyed == true){
                                return@SecHomeDetailAdapter
                            }
                            firebaseLog("home_to_playlist_det")

                            mintySecMainPage?.startInterActivity(Intent(mintySecMainPage, MintySecPyDetailView::class.java).apply {
                                putExtra("playlistId",item.id)
                            }, playlistDetInter(),false)

                        }
//                            navController.navigate("online_playlist/${item.id}")
                    }
                },
            )
            binding.itemsRecyclerView.adapter = adapter
            adapter.setList(section.items)
        }
    }
}
