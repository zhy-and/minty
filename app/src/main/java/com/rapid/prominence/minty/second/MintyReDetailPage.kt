package com.rapid.prominence.minty.second

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintySecReleaseDetailBinding
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.randomBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.zionhuang.innertube.models.AlbumItem
import com.rapid.prominence.minty.second.databean.NewReleaseViewModel
import com.rapid.prominence.minty.utils.joinByBullet
import kotlinx.coroutines.launch
import timber.log.Timber

class MintyReDetailPage : MintyBaseActivity() {

    private lateinit var binding: MintySecReleaseDetailBinding
    private lateinit var newReleaseAlbumsAdapter: ItemsAdapter
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintySecReleaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).transparentStatusBar().titleBar(binding.tops.root).init()
        val albumViewModel =
            NewReleaseViewModel(this, InternalDatabase.newInstance(this)

            )
        binding.tops.apply {
            lists.visibility = View.VISIBLE
            searchEditText.setOnClickListener {
                startSearchPage()
            }
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
        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        binding.loadings.post {
            binding.loadings.visibility = View.VISIBLE
            binding.loadings.startShimmerAnimation()
        }
        newReleaseAlbumsAdapter = ItemsAdapter(emptyList())
        binding.listRecycleview.layoutManager = GridLayoutManager(this, 2)
        binding.listRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 1f),2))
        binding.listRecycleview.adapter = newReleaseAlbumsAdapter
        lifecycleScope.launch {

                albumViewModel?.newReleaseAlbums?.collect {
                    Timber.e(it.toString())
                    if (isFinishing || isDestroyed){
                        return@collect
                    }
                    binding.loadings.visibility = View.GONE
                    binding.loadings.stopShimmerAnimation()
                    it?.let {
                        its ->
                        newReleaseAlbumsAdapter?.addData(its)
                    }

                }

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

    }
    inner class ItemsAdapter( var items: List<AlbumItem>) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemTitleTextView: TextView = itemView.findViewById(R.id.title)
            val artist: TextView = itemView.findViewById(R.id.artist)
            val ivgs: ImageView = itemView.findViewById(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_album, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
          holder.itemTitleTextView.text = item.title
          holder.artist.text =  joinByBullet(item.artists?.joinToString { it.name })

            Glide.with(this@MintyReDetailPage)
                .load(item?.thumbnail)
                .apply(options)
                .thumbnail(
                    Glide.with(this@MintyReDetailPage)
                        .load(randomBitmap())
                        .apply(options)
                )
                .into(holder.ivgs)

            holder.itemView.setOnClickListener {
                startInterActivity(Intent(this@MintyReDetailPage, MintySecAlbumDetailPage::class.java).apply {
                    putExtra("albumId",item.id)
                }, albumDetInter(),false)
            }

        }

        override fun getItemCount(): Int = items.size
        fun addData(its: List<AlbumItem>) {
            items = its
            notifyDataSetChanged()
        }
    }




}
