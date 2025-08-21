package com.rapid.prominence.minty.second


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.artOptions1
import com.rapid.prominence.minty.artOptions2
import com.rapid.prominence.minty.artOptions3
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.cons.projectDetInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.database.entities.SongEntity
import com.rapid.prominence.minty.databinding.MintySecArtPageBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.logClickEvents
import com.rapid.prominence.minty.extensions.toMediaItem
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.ArtistViewModel
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.service.queues.YouTubeQueue.Companion.radio
import com.rapid.prominence.minty.utils.joinByBullet
import com.zionhuang.innertube.models.*
import com.zionhuang.innertube.pages.ArtistSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MintySecArtistDetailPage : MintyBaseActivity() {

    private var queue: WatchEndpoint? = null
    private lateinit var binding: MintySecArtPageBinding
    private lateinit var newReleaseAlbumsAdapter: SectionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintySecArtPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        try{
            ViewCompat.setOnApplyWindowInsetsListener(binding.mintyIncContainerPlay.container){view , insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(params.leftMargin,
                    params.topMargin,
                    params.rightMargin,
                    systemBars.bottom)
                view.layoutParams = params
//            binding.mintyIncContainerPlay.container.setPadding(binding.mintyIncContainerPlay.container.paddingLeft,binding.mintyIncContainerPlay.container.paddingTop,binding.mintyIncContainerPlay.container.paddingRight,systemBars.bottom+binding.mintyIncContainerPlay.container.paddingBottom)
                insets
            }
//        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.tops.root).init()
            val albumViewModel = intent?.getStringExtra("artistId")?.let {
                ArtistViewModel(this, InternalDatabase.newInstance(this), it

                )
            }
//        ImmersionBar.with(this).fullScreen(false).init()
            binding.tops.apply {
                lists.visibility = View.VISIBLE
                lists.setOnClickListener {
                    startQueuePage()
                }
                searchEditText.setOnClickListener {
                    startSearchPage()
                }
                inputSearch.setOnClickListener {
                    startSearchPage()
                }
                closeButton.setOnClickListener {
                    finishCurrent()
                }
            }
            newReleaseAlbumsAdapter =  SectionsAdapter(emptyList())
            binding.specAnm.post {
                binding.specAnm.visibility = View.VISIBLE
                binding.specAnm.startShimmerAnimation()
            }
            binding.playBtns.setOnClickListener {
                logClickEvents("artist_det_play_btn")

                queue?.let {
                    startPlayerPage()
                    secPlayConnect?.playQueue(YouTubeQueue(it))
                }
            }
            binding.mintyIncContainerPlay.container.setOnClickListener {
                startPlayerPage()
            }
            ImmersionBar.with(this).navigationBarColor(R.color.new_color).init()
            binding.homePageSectionsRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.homePageSectionsRecyclerView.isNestedScrollingEnabled = false
            binding.homePageSectionsRecyclerView.adapter = newReleaseAlbumsAdapter
            lifecycleScope.launch {

                val options: RequestOptions = RequestOptions()
                    .placeholder(R.mipmap.bg1)
                    .error(R.mipmap.bg1)

                albumViewModel?.artistPage?.collect {
                    if (isFinishing || isDestroyed) {
                        return@collect
                    }
                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    binding.titles.text = it?.artist?.title

                    Glide.with(this@MintySecArtistDetailPage)
//                .load(posBitmap(queryType))
                        .load(it?.artist?.thumbnail)
                        .apply(options)
                        .thumbnail(
                            Glide.with(this@MintySecArtistDetailPage)
                                .load(randomBitmap())
                                .apply(options)
                        )
                        .into(binding.thumbnailss)


                    it?.sections?.let { ots ->
                        newReleaseAlbumsAdapter?.addData(ots)
                    }
                    it?.artist?.shuffleEndpoint?.let {
                        queue = it
                    }
                }


//
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
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
            // 设置艺术家页面部分

        inner class SectionsAdapter(private var sections: List<ArtistSection>) : RecyclerView.Adapter<SectionsAdapter.ViewHolder>() {

            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val sectionTitleTextView: TextView = itemView.findViewById(R.id.main_title_name)
                val constraint: View = itemView.findViewById(R.id.constraint)
                val sectionItemsRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_sec)
                val mores: View = itemView.findViewById(R.id.click_more)
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
            try {
                val section = sections[position]
                holder.sectionTitleTextView.text = section.title
                section.moreEndpoint?.let{
                    holder.mores.visibility = View.VISIBLE
                }
                holder.constraint.setOnClickListener {
                    section.moreEndpoint?.let {
                        startInterActivity(Intent(this@MintySecArtistDetailPage, ArtistMoreInfoView::class.java).apply {
                            putExtra("browseId",it.browseId)
                            putExtra("params",it.params)
                            putExtra("title",section.title)
                        }, projectDetInter(),false)
                        logClickEvents("art_det_to_item_detail")

                    }
                }
                val isVertical = (section.items.firstOrNull() as? SongItem)?.album != null

                holder.sectionItemsRecyclerView.layoutManager = if (isVertical) {
                    LinearLayoutManager(this@MintySecArtistDetailPage, LinearLayoutManager.VERTICAL, false)
                } else {
                    LinearLayoutManager(this@MintySecArtistDetailPage, LinearLayoutManager.HORIZONTAL, false)
                }
                holder.sectionItemsRecyclerView.isNestedScrollingEnabled = false

                holder.sectionItemsRecyclerView.adapter = SectionItemsAdapter(section.items,isVertical)
            }catch (e : Exception){
                e.printStackTrace()
            }

            }

            override fun getItemCount(): Int = sections.size
        }

        inner class SectionItemsAdapter(private val items: List<YTItem>, var isVertical: Boolean) : RecyclerView.Adapter<SectionItemsAdapter.ViewHolder>() {

            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val title: TextView? = itemView.findViewById(R.id.title)
                val title2: TextView? = itemView.findViewById(R.id.title2)
                val artist: TextView? = itemView.findViewById(R.id.artist)
                val ivgs: ImageView? = itemView.findViewById(R.id.ivgs)
                val thumbnail: ImageView? = itemView.findViewById(R.id.thumbnail)
                val thumbnail2: ImageView? = itemView.findViewById(R.id.thumbnail2)
                val thumbnail3: View? = itemView.findViewById(R.id.thumbnail3)
                val circleView: ImageView? = itemView.findViewById(R.id.thumbnail_circle)
                val artPlay: ImageView? = itemView.findViewById(R.id.play_icon)
                val videoPlay: ImageView? = itemView.findViewById(R.id.play_icon2)

            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val layoutId = if (isVertical) R.layout.item_sec_artist_one_so_layout else R.layout.item_sec_artist_release_al_layout
                val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = items[position]

                when (item) {
                    is SongItem -> {
                        if (item.album == null){
                            holder.thumbnail2?.let {
                                holder.thumbnail2.visibility = View.VISIBLE
                                holder.thumbnail?.visibility = View.GONE
                                holder.videoPlay?.let {
                                    its ->
                                    if(its.isGone){
                                        its.visibility = View.VISIBLE
                                    }
                                }
                                holder.artPlay?.let {
                                        arts ->
                                    if(arts.isVisible){
                                        arts.visibility = View.GONE
                                    }
                                }
                                Glide.with(holder.itemView)
                                    .asBitmap()
                                    .load(item.thumbnail)
                                    .apply(artOptions3)
                                    .into(it)
                            }
                        }else{
                            holder.thumbnail?.let {
                                Glide.with(holder.itemView)
                                    .asBitmap()
                                    .load(item.thumbnail)
                                    .apply(artOptions1)
                                    .into(it)
                            }
                        }
                        holder?.title?.text = item.title
                        holder?.artist?.text = joinByBullet(item.artists?.joinToString { it.name })
                    }
                    is AlbumItem -> {
                        holder.thumbnail?.let {
                            Glide.with(holder.itemView)
                                .asBitmap()
                                .load(item.thumbnail)
                                .apply(artOptions2)

                                .into(it)
                        }
                        holder?.title?.text = item.title
                        if (!TextUtils.isEmpty(joinByBullet(item.artists?.joinToString { it.name }))){
                            holder?.artist?.text = joinByBullet(item.artists?.joinToString { it.name })
                            holder.artist?.visibility = View.VISIBLE
                        }else{
                            holder.artist?.visibility = View.GONE
                        }
                        holder.artPlay?.let {
                                arts ->
                            if(arts.isGone){
                                arts.visibility = View.VISIBLE
                            }
                        }
                    }
                    is ArtistItem -> {
                        holder.thumbnail3?.let {
                            holder.thumbnail?.visibility = View.GONE
                            holder.thumbnail3?.visibility = View.VISIBLE
                            holder.thumbnail2?.visibility = View.GONE
                            holder.circleView?.let {
                                Glide.with(holder.itemView)
                                    .asBitmap()
                                    .load(item.thumbnail)
                                    .into(it)
                            }

                        }
                        holder?.title?.text = item.title
                        holder.title?.gravity = Gravity.CENTER
                        holder.artist?.visibility = View.GONE
                    }
                    is PlaylistItem -> {

                        holder.thumbnail?.let {
                            Glide.with(holder.itemView)
                                .asBitmap()
                                .load(item.thumbnail)
                                .apply(artOptions2)
                                .into(it)
                        }
                        holder?.title?.text = item.title
                        holder.artist?.visibility = View.GONE
                    }
                    else -> {
//                        Log.e("other",item.toString())
                    }
                }
                holder.itemView.setOnClickListener {
                    when (item) {
                        is SongItem -> {
                            secPlayConnect?.playQueue(radio(item.toMediaMetadata()))
                           startPlayerPage()
                        }
                        is AlbumItem ->   {
                            startInterActivity(Intent(this@MintySecArtistDetailPage, MintySecAlbumDetailPage::class.java).apply {
                                putExtra("albumId",item.id)
                            }, albumDetInter(),false)
                        }
                        is ArtistItem -> {
                            startInterActivity(Intent(this@MintySecArtistDetailPage, MintySecArtistDetailPage::class.java).apply {
                                putExtra("artistId",item.id)
                            }, artistDetInter(),false)
                        }
                        is PlaylistItem -> {
                            startInterActivity(Intent(this@MintySecArtistDetailPage, MintySecPyDetailView::class.java).apply {
                                putExtra("playlistId",item.id)
                            }, playlistDetInter(),false)
                        }
                    }
                }


                holder.ivgs?.setOnClickListener{
                    val data = item as? SongItem ?: return@setOnClickListener
                    try {
                        showDialogs(holder.itemView.context, R.layout.minty_dialog_popup,true, Gravity.BOTTOM).apply {
                            val text =   findViewById<TextView>(R.id.titles4)
                            var database : MusicDatabase?= null
                            App.instance?.let {
                                database = InternalDatabase.newInstance(it)
                                database?.transaction {
                                    lifecycleScope.launch {
                                        song(item.id).first().let {
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
                                secPlayConnect?.playQueue(radio(data.toMediaMetadata()))
                                startPlayerPage()
                            }
                            findViewById<View>(R.id.play_next)?.setOnClickListener {
                                if (isShowing){
                                    dismiss()
                                }
                                secPlayConnect?.playNext(data.toMediaItem())
                            }
                            findViewById<View>(R.id.add_queue)?.setOnClickListener {
                                if (isShowing){
                                    dismiss()
                                }
                                secPlayConnect?.addToQueue(data.toMediaItem())
                            }
                            findViewById<View>(R.id.add_fav)?.setOnClickListener {
                                if (isShowing){
                                    dismiss()
                                }
                                database?.transaction {
                                    lifecycleScope.launch {
                                        song(item.id).first().let {
                                                its ->
//                                            Log.e("test",it.toString())
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

            }

            override fun getItemCount(): Int = items.size
        }
    override fun onResume() {
        super.onResume()
        if (secPlayConnect != null && secPlayConnect?.player?.currentMetadata != null){
            secPlayConnect?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }
    }






    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)
    }
}


