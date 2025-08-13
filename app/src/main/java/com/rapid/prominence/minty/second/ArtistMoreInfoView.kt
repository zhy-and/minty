package com.rapid.prominence.minty.second

import androidx.lifecycle.lifecycleScope


import com.zionhuang.innertube.models.SongItem

import kotlinx.coroutines.launch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.ItemsPage
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.databinding.MintySecReleaseDetailBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.playerConnection
import com.zionhuang.innertube.models.*
import com.zionhuang.innertube.pages.ArtistSection
import com.rapid.prominence.minty.second.databean.ArtistItemsViewModel
import com.rapid.prominence.minty.second.search.SongAdapter
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.rapid.prominence.minty.utils.joinByBullet
import com.rapid.prominence.minty.utils.makeTimeString

import kotlinx.coroutines.CoroutineScope

class ArtistMoreInfoView : MintyBaseActivity() {
    private var tempPage: ItemsPage?= null
    private  var songAdapter: SongAdapter?= null
    private  var adapter: ItemsAdapter?= null
    var isLoadingNextPage = false;

    private lateinit var binding: MintySecReleaseDetailBinding
    private lateinit var newReleaseAlbumsAdapter: SectionsAdapter
    override fun onResume() {
        super.onResume()
        if (playerConnection != null && playerConnection?.player?.currentMetadata != null){
            playerConnection?.mediaMetadata?.let {
                updatePlayerUI(it.value)
            }
        }
    }

    private fun updatePlayerUI(metadata: MediaMetadata?) {
        addUIPlayer(binding.bottomPlayer.artist,binding.bottomPlayer.title,binding.bottomPlayer.thumbnail,metadata)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintySecReleaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.tops.root).init()
        binding.tops.apply {
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
        binding.bottomPlayer.container.setOnClickListener {
            startPlayerPage()
        }
        binding.specAnm2.post {
            binding.specAnm2.visibility = View.VISIBLE
            binding.specAnm2.startShimmerAnimation()
        }
        val broId =   intent?.getStringExtra("browseId")
        val params =   intent?.getStringExtra("params")
        broId ?: return
        params ?: return
        val albumViewModel = ArtistItemsViewModel(this,broId,params)
//        newReleaseAlbumsAdapter =  SectionsAdapter(emptyList())
//
//        binding.homePageSectionsRecyclerView.layoutManager = LinearLayoutManager(this)
//        binding.homePageSectionsRecyclerView.adapter = newReleaseAlbumsAdapter
        lifecycleScope.launch {
//            withContext(Dispatchers.IO){
//              database.quickPicks()
//                    .first().shuffled().take(20).forEach {
//                        Log.e("quickPicks-----",it.toString())
//                  }
//
//                val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
//
//                database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
//                    .first().shuffled().take(10).forEach {
//                        Log.e("keep -----",it.toString())
//                    }




//            albumViewModel?.title.observe(this) { title ->
//                titleTextView.text = title
//            }

            albumViewModel?.itemsPage?.collect { itemsPage ->
                tempPage = itemsPage
                binding.titles.text = itemsPage?.items?.firstOrNull()?.title
//                Log.e("artist item ----",itemsPage.toString())
                if (isFinishing || isDestroyed){
                    return@collect
                }
                binding.specAnm2.visibility = View.GONE
                binding.specAnm2.stopShimmerAnimation()
                if (itemsPage == null) {
                    // Show shimmer loading
                } else {
                    if (itemsPage.items.firstOrNull() is SongItem) {
                        songAdapter =  SongAdapter { songItem ->
                            playerConnection?.playQueue(radio(songItem.toMediaMetadata()))
                            startPlayerPage()
                        }
                        songAdapter?.addData(itemsPage.items.filterIsInstance<SongItem>() as ArrayList<SongItem>)
                        binding.moodAndGenresRecyclerView.layoutManager = LinearLayoutManager(this@ArtistMoreInfoView)
                        binding.moodAndGenresRecyclerView.adapter = songAdapter

                    } else {
                        binding.moodAndGenresRecyclerView.layoutManager = GridLayoutManager(this@ArtistMoreInfoView,2)
                        binding.moodAndGenresRecyclerView?.addItemDecoration(
                            SecGridItemDecoration(
                                SizeUtils.dp2px( 20f),2)
                        )
                        adapter = ItemsAdapter(itemsPage.items)
                        binding.moodAndGenresRecyclerView.adapter = adapter

                    }
                    binding.moodAndGenresRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)

                        }

                        override fun onScrollStateChanged(
                            recyclerView: RecyclerView,
                            newState: Int
                        ) {
                            super.onScrollStateChanged(recyclerView, newState)
//                            if (newState == RecyclerView.SCROLL_STATE_IDLE && isBottom(binding.moodAndGenresRecyclerView) && !isLoadingNextPage) {
//                                lifecycleScope.launch {
//                                    withContext(Dispatchers.IO){
////                                            val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@withContext
////                                            viewStateMap[key] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
////                                            viewStateMap[key]?.items?.filterIsInstance<AlbumItem>()?.let {
////                                                withContext(Dispatchers.Main){
////                                                    albumUniqueAdapter?.addData(it)
////                                                }
////                                                isLoadingAlbumNextPage = false
//
//                                                val oldItemsPage = tempPage ?: return@withContext
//                                                val continuation = oldItemsPage.continuation ?: return@withContext
//                                                     isLoadingNextPage = true
//
//                                                YouTube.artistItemsContinuation(continuation)
//                                                    .onSuccess { artistItemsContinuationPage ->
//                                                        isLoadingNextPage = false
//                                                          tempPage =   ItemsPage(
//                                                                items = (oldItemsPage.items + artistItemsContinuationPage.items)
//                                                                    .distinctBy { it.id }
//                                                                    .filterExplicit(dataStore.get(
//                                                                        HideExplicitKey, false)),
//                                                                continuation = artistItemsContinuationPage.continuation
//                                                            )
//                                                        Log.e("more",tempPage.toString())
//
//                                                        withContext(Dispatchers.Main){
//                                                            if(tempPage?.items?.firstOrNull() is SongItem){
//                                                                songAdapter?.addData(tempPage?.items as ArrayList<SongItem>)
//                                                            }else{
//                                                                adapter?.addData(tempPage?.items as ArrayList<YTItem>)
//                                                            }
//                                                        }
//                                                    }.onFailure {
//                                                        isLoadingNextPage = false
//                                                        reportException(it)
//                                                    }
//
//                                        }
//                                    }
//
//                                }
                        }
                    })
                }
            }



//            // 观察 ViewModel 数据
//            albumViewModel?.librarySongs.observe(this) { artistPage ->
//                if (artistPage != null) {
//                    // 更新 UI
//                    supportActionBar?.title = artistPage.artist.title
//                    // 其他 UI 更新逻辑...
//                }
//            }
//
//            albumViewModel?.librarySongs.observe(this) { songs ->
//                adapter.submitList(songs)
//                progressBar.isVisible = false
//            }
//
//
//            albumViewModel?.songLists?.collect {
//                    Timber.e(it.toString())
//                    if (it == null) {
//                        return@collect
//                    }
//                    // Set data to adapters
//                    withContext(Dispatchers.Main){
//                        it?.apply {
//                            newReleaseAlbumsAdapter?.submitList(this)
//                        }
//                    }
//
//                }

//            }

        }
        lifecycleScope.launch {

            playerConnection?.mediaMetadata?.collect{
                updatePlayerUI(it)
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












    // 设置艺术家页面部分




    inner class SectionsAdapter(private var sections: List<ArtistSection>) : RecyclerView.Adapter<SectionsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val sectionTitleTextView: TextView = itemView.findViewById(R.id.main_title_name)
            val sectionItemsRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view)
        }
        fun addData(songs: List<ArtistSection>){
            this.sections = songs
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.minty_sec_item_sec, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val section = sections[position]
            holder.sectionTitleTextView.text = section.title
            holder.sectionTitleTextView.setOnClickListener {
                section.moreEndpoint?.let {
//                        navController.navigate("artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}")
                }
            }

            holder.sectionItemsRecyclerView.layoutManager = if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                LinearLayoutManager(this@ArtistMoreInfoView, LinearLayoutManager.VERTICAL, false)
            } else {
                LinearLayoutManager(this@ArtistMoreInfoView, LinearLayoutManager.HORIZONTAL, false)
            }
            holder.sectionItemsRecyclerView.adapter = SectionItemsAdapter(section.items)
        }

        override fun getItemCount(): Int = sections.size
    }

    inner class SectionItemsAdapter(private val items: List<YTItem>) : RecyclerView.Adapter<SectionItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.item_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.minty_sec_item_sec_det, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.itemTitleTextView.text = when (item) {
                is SongItem -> item.title
                is AlbumItem -> item.title
                is ArtistItem -> item.title
                is PlaylistItem -> item.title
                else -> ""
            }
            holder.itemView.setOnClickListener {
                when (item) {
                    is SongItem ->{
                        playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                        startPlayerPage()
                    }
                    is AlbumItem ->   {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecAlbumDetailPage::class.java).apply {
                            putExtra("albumId",item.id)
                        }, albumDetInter(),false)
                    }
                    is ArtistItem -> {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecArtistDetailPage::class.java).apply {
                            putExtra("artistId",item.id)
                        }, artistDetInter(),false)
                    }
                    is PlaylistItem -> {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecPyDetailView::class.java).apply {
                            putExtra("playlistId",item.id)
                        }, playlistDetInter(),false)
                    }
                }
            }
//                holder.itemView.setOnLongClickListener {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    menuState.show {
//                        when (item) {
//                            is SongItem -> YouTubeSongMenu(
//                                song = item,
//                                navController = navController,
//                                onDismiss = menuState::dismiss
//                            )
//                            is AlbumItem -> YouTubeAlbumMenu(
//                                albumItem = item,
//                                navController = navController,
//                                onDismiss = menuState::dismiss
//                            )
//                            is ArtistItem -> YouTubeArtistMenu(
//                                artist = item,
//                                onDismiss = menuState::dismiss
//                            )
//                            is PlaylistItem -> YouTubePlaylistMenu(
//                                playlist = item,
//                                coroutineScope = coroutineScope,
//                                onDismiss = menuState::dismiss
//                            )
//                        }
//                    }
//                    true
//                }
        }

        override fun getItemCount(): Int = items.size
    }

    private lateinit var coroutineScope: CoroutineScope

    private lateinit var titleTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var topAppBar: androidx.appcompat.widget.Toolbar
    private lateinit var backIconButton: ImageButton
    private lateinit var closeIconButton: ImageButton
    private lateinit var selectAllCheckBox: CheckBox
    private lateinit var moreIconButton: ImageButton

    private var inSelectMode = false
    private val selection = mutableListOf<String>()



    inner class ItemsAdapter(private var items: List<YTItem>) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.title)
            val artist: TextView = itemView.findViewById(R.id.artist)
            val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
        }
        fun addData(songs: ArrayList<YTItem>){
            this.items = songs
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_album, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {


            val item = items[position]
            when (item) {
                is SongItem -> {
                    holder.artist.text =  joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                }
                is AlbumItem ->   {
                    holder.artist.text =  joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())

                }
                is ArtistItem -> {
                }
                is PlaylistItem -> {
                    holder.artist.text =  joinByBullet(item.author?.name, item.songCountText)
                }
            }
            holder.itemTitleTextView.text = item.title

            Glide.with(this@ArtistMoreInfoView)
                .load(item?.thumbnail)
                .apply(options)
                .thumbnail(
                    Glide.with(this@ArtistMoreInfoView)
                        .load(randomBitmap())
                        .apply(options)
                )
                .into(holder.ivgs)

            holder.itemView.setOnClickListener {
                when (item) {
                    is SongItem -> {
                        playerConnection?.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        holder.artist.text =  joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                    }
                    is AlbumItem ->   {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecAlbumDetailPage::class.java).apply {
                            putExtra("albumId",item.id)
                        }, albumDetInter(),false)
                        holder.artist.text =  joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())

                    }
                    is ArtistItem -> {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecArtistDetailPage::class.java).apply {
                            putExtra("artistId",item.id)
                        }, artistDetInter(),false)
                    }
                    is PlaylistItem -> {
                        startInterActivity(Intent(this@ArtistMoreInfoView, MintySecPyDetailView::class.java).apply {
                            putExtra("playlistId",item.id)
                        }, playlistDetInter(),false)
                        holder.artist.text =  joinByBullet(item.author?.name, item.songCountText)

                    }
                }
            }

        }

        override fun getItemCount(): Int = items.size
    }

}


