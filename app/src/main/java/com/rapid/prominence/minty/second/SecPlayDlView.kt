package com.rapid.prominence.minty.second

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Timeline
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.databinding.MintyQueueBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.getQueueWindows
import com.rapid.prominence.minty.extensions.metadata
import com.rapid.prominence.minty.extensions.togglePlayPause
import com.rapid.prominence.minty.options
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.secPlayConnect
import com.rapid.prominence.minty.second.databean.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SecPlayDlView : MintyBaseActivity() {


    override fun onResume() {
        super.onResume()
        if (secPlayConnect != null && secPlayConnect?.player?.currentMetadata != null){
            secPlayConnect?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }

    }

    private var songAdapter: ItemsAdapter?= null
    private lateinit var binding: MintyQueueBinding
    private  var secReleaseAlbumAdapter: SecReleaseAlbumAdapter?= null
    private  var homeStyleAdapter: HomeStyleAdapter?= null

    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        songAdapter = ItemsAdapter()
        binding.titles.apply {
            lists.visibility = View.VISIBLE
            lists.setImageResource(R.mipmap.white_search)
            nowPlayingText.text = "Queue"
            inputSearch.visibility = View.GONE
            closeButton.setOnClickListener {
                finishCurrent()
            }
            lists.setOnClickListener {
                startSearchPage()
            }

        }

        binding.mintyIncContainerPlay.container.setOnClickListener {
            startPlayerPage()
        }
        binding.listRecycleview.layoutManager = LinearLayoutManager(this@SecPlayDlView)
        binding.listRecycleview.adapter = songAdapter
        lifecycleScope.launch {
            if (secPlayConnect?.queueWindows?.value.isNullOrEmpty()){
                initEmptyLayout()
                return@launch
            }
            secPlayConnect?.queueWindows?.value?.let {
                if (it.isNullOrEmpty()){
                    initEmptyLayout()
                    return@launch
                }

                try {
                    val arrayList = ArrayList<Timeline.Window>()
                    it.forEach { one ->
                        arrayList.add(one)
                    }
                    if (arrayList.isNullOrEmpty()){
                        initEmptyLayout()
                        return@let
                    }
                    binding.listLayout.visibility = View.VISIBLE
                    binding.emptyLayout.visibility = View.GONE
                    songAdapter?.addData(arrayList)
                }catch (e : Exception){
                    e.printStackTrace()
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

    private fun initEmptyLayout() {
        binding.emptyLayout.visibility = View.VISIBLE
        binding.listLayout.visibility = View.GONE
        HomeViewModel(
            this@SecPlayDlView,
            InternalDatabase.newInstance(this@SecPlayDlView)
        ).apply {
            binding.specAnm.post {
                binding.specAnm.visibility = View.VISIBLE
                binding.specAnm.startShimmerAnimation()
            }
            lifecycleScope.launch {
                explorePage?.collect {
                    if (isFinishing || isDestroyed) {
                        return@collect
                    }
    //                                    binding.specAnm.visibility = View.GONE
    //                                    binding.specAnm.stopShimmerAnimation()

                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@SecPlayDlView)
                    homeStyleAdapter = HomeStyleAdapter(this@SecPlayDlView)
                    binding.newReleaseAlbumsRecyclerView.layoutManager = LinearLayoutManager(
                        this@SecPlayDlView,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.newReleaseAlbumsRecyclerView.adapter = secReleaseAlbumAdapter

                    binding.homeListRecycleview.layoutManager =
                        GridLayoutManager(this@SecPlayDlView, 2)
                    binding.homeListRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 10f),2))

                    binding.homeListRecycleview.adapter = homeStyleAdapter
                    binding.homeListRecycleview.isNestedScrollingEnabled = false
                    withContext(Dispatchers.Main) {
                        it?.newReleaseAlbums?.apply {
                            secReleaseAlbumAdapter?.submitList(this)
                        }
                        it?.moodAndGenres?.apply {
                            homeStyleAdapter?.submitList(this)
                        }
                    }
                    Timber.e(it.toString())
    //                        }
                }
            }
        }
    }

    inner class ItemsAdapter : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sec_one_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.delete.setImageResource(R.mipmap.dete)
            val item = items[position]
            item.mediaItem.metadata?.let {
                holder.itemTitleTextView.text = it.title
                holder.subtitle.text = item.mediaItem.metadata?.artists?.joinToString { it.name }
                Glide.with(holder.itemView)
                    .load(it.thumbnailUrl)
                    .apply(options)
                    .thumbnail(
                        Glide.with(holder.itemView)
                            .load(posBitmap(position))
                            .apply(options)
                    )
                    .into(holder.ivgs)

            }
            holder.delete.setOnClickListener {
                try {
                    showDialogs(this@SecPlayDlView, R.layout.minty_dialog_sure_exit).apply {
                        findViewById<View>(R.id.cancel_button)?.setOnClickListener {
                            if (isShowing && !isDestroyed && !isFinishing){
                                dismiss()
                            }
                        }
                        findViewById<TextView>(R.id.titles)?.text = getString(R.string.is_delete_queue)
                        findViewById<View>(R.id.ok_btn)?.setOnClickListener {
                            if (isShowing && !isDestroyed && !isFinishing){
                                dismiss()
                            }
                            if (items?.size == 1){
                                ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER,0,0).show("Only one song left, please add more!")
                                return@setOnClickListener
                            }
//                        playerConnection?.player?.mediaItems?.forEachIndexed { index, mediaItem ->
//                            if (mediaItem.mediaMetadata.title == item.mediaItem.metadata?.title) {
//                                Log.e("-----f","fff"+item.firstPeriodIndex)
//                                playerConnection?.player?.removeMediaItem(index)
//                                items.remove(item)
//                                notifyDataSetChanged()
//                            }
//                        }
//                        items.sortedBy {
//                            -it.firstPeriodIndex
//                        }.forEach {
//                            its ->
                            secPlayConnect?.player?.removeMediaItem(item.firstPeriodIndex)
//                        }
                            try {
                                val arrayList = ArrayList<Timeline.Window>()
                                secPlayConnect?.player?.getQueueWindows()?.forEach { one ->
                                    arrayList.add(one)
                                }
                                addData(arrayList)
                            }catch (e : Exception){
                                e.printStackTrace()
                            }

                        }
                    }

                }catch (e : Exception){
                    e.printStackTrace()
                }

            }

            holder.itemView.setOnClickListener {


                if (position == secPlayConnect?.currentWindowIndex?.value) {
                    secPlayConnect?.player?.togglePlayPause()
                } else {


                    secPlayConnect?.player?.seekToDefaultPosition(items[position].firstPeriodIndex)
                    secPlayConnect?.player?.playWhenReady = true
                }
               startPlayerPage()
            }

        }

        override fun getItemCount(): Int = items.size

    }




}
