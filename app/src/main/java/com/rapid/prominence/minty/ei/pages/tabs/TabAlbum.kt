package com.rapid.prominence.minty.ei.pages.tabs

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.ei.scan.EiScanTools
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.databinding.TabAlbumBinding
import com.rapid.prominence.minty.ei.scan.Album
import com.rapid.prominence.minty.ei.kts.mmkvs.initCreateData
import com.rapid.prominence.minty.ei.kts.mmkvs.loadSongs
import com.rapid.prominence.minty.ei.item.SpaceItemDecoration
import com.rapid.prominence.minty.ei.pages.EiAlbumDetail
import com.rapid.prominence.minty.posBitmap
import com.tencent.mmkv.MMKV



class TabAlbum : Fragment() {
    private  var _binding: TabAlbumBinding?= null
    private  val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabAlbumBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initView()
        if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
            binding?.emptyLayout?.visibility = View.VISIBLE
            binding.recyclerView?.visibility = View.GONE
            binding.imgs?.visibility = View.GONE
        }else{
            loadAlbums(false)
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private var adapter: AlbumAdapter? = null
    private var horAdapter: TabHome.SongAdapter2? = null


    private fun initView() {
        binding?.emoScan?.setOnClickListener {
            activity?.apply {
                if (isFinishing || isDestroyed){
                    return@apply
                }
                var ctx = this as EiMainPage
                ctx.updateUI()
                ctx.animation()
            }
            loadAlbums(true)
        }
    }
    override fun onResume() {
        super.onResume()
        if (binding?.emptyLayout?.visibility == View.VISIBLE &&  MMKV.defaultMMKV().getBoolean("isAuth",false)){
            val songs = EiScanTools.getLocalAlbums(requireContext()) as ArrayList<Album>
            if (songs.isNullOrEmpty()){
                binding?.emptyLayout?.visibility = View.VISIBLE
                binding.recommendView?.visibility = View.GONE
                binding.recyclerView?.visibility = View.GONE
                binding.emIv?.setImageResource(R.mipmap.empty_logo)
                binding.emTx?.text = "No music found in this device!"
                return
            }
            changeUI(songs)
        }
    }
    private fun setupRecyclerView() {
        binding.recyclerView?.setLayoutManager(GridLayoutManager(getContext(),2))
//        recyclerView?.addItemDecoration( GridSpaceItemDecoration(2,
//            SizeUtils.dp2px( 20f),
//            SizeUtils.dp2px( 16f))
//        );
        binding.recyclerView?.addItemDecoration(
            SpaceItemDecoration(
                SizeUtils.dp2px(20f)
            )
        )
        adapter = AlbumAdapter()
        binding.recyclerView?.setAdapter(adapter)
    }

    private fun loadAlbums(b: Boolean) {
        try {
            XXPermissions.with(this).permission(Permission.READ_MEDIA_AUDIO)
                .request { permissions, allGranted ->
                    if (allGranted) {
//                    if (adapter?.items?.isNotEmpty() == true){
//                        return@request
//                    }
                        if (b){
                            activity?.apply {
                                if (isFinishing || isDestroyed){
                                    return@apply
                                }
                                val ctx = this as EiMainPage
                                ctx.updateUI()
                                ctx.animation()
                            }
                        }
                        MMKV.defaultMMKV().putBoolean("isAuth",true)
                        val songs = EiScanTools.getLocalAlbums(requireContext()) as ArrayList<Album>
                        if (songs.isNullOrEmpty()){
                            binding?.emptyLayout?.visibility = View.VISIBLE
                            binding.recommendView?.visibility = View.GONE
                            binding.recyclerView?.visibility = View.GONE
                            binding.emIv?.setImageResource(R.mipmap.empty_logo)
                            binding.emTx?.text = "No music found in this device!"
                            return@request
                        }
                        changeUI(songs,b)
                    }
                }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun changeUI(songs: ArrayList<Album>, isCheck : Boolean = false) {
        try {
            binding?.emptyLayout?.visibility = View.GONE
            binding.recyclerView?.visibility = View.VISIBLE
            binding?.imgs?.visibility = View.VISIBLE

            adapter?.add(songs)
            loadSongs(Song()).let { songs ->
                if (songs.size == 0){
                    val items = EiScanTools.getLocalSongs(requireContext()) as ArrayList<Song>
                    initCreateData(items)
                    if (items.size >= 2){
                        binding.recommendView?.visibility = View.VISIBLE
                        binding.recommendList?.setLayoutManager(
                            LinearLayoutManager(getContext(),
                                LinearLayoutManager.HORIZONTAL,false)
                        )
                        horAdapter = TabHome.SongAdapter2()
                        binding.recommendList?.setAdapter(horAdapter)
                        horAdapter?.add(items.take(2) as ArrayList<Song>)
                    }
                }else{
                    if (songs.size >= 2){
                        binding.recommendView?.visibility = View.VISIBLE
                        binding.recommendList?.setLayoutManager(
                            LinearLayoutManager(getContext(),
                                LinearLayoutManager.HORIZONTAL,false)
                        )
                        horAdapter = TabHome.SongAdapter2()
                        binding.recommendList?.setAdapter(horAdapter)
                        horAdapter?.add(songs.take(2) as ArrayList<Song>)
                    }
                }
                if (isCheck){
                    activity?.apply {
                        if (isFinishing || isDestroyed){
                            return@apply
                        }
                        val ctx = this as EiMainPage
                        ctx.updateUI()
                    }
                }

            }

        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    inner class AlbumAdapter : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {
        var items: ArrayList<Album> = arrayListOf()
        fun add( datas: ArrayList<Album>){
            items = datas
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById<TextView>(R.id.title)
            val tvArtist: TextView = itemView.findViewById<TextView>(R.id.artist)
            val img: ImageView = itemView.findViewById(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.album_ei_item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val song = items[position]
            holder.apply {
                tvTitle.text = song.artist
                tvArtist.text = song.name
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumid.toLong()
                )
                val options: RequestOptions = RequestOptions()
                    .placeholder(posBitmap(position))
                    .error(posBitmap(position))
                    .transform(RoundedCorners(SizeUtils.dp2px(8f)))
                Glide.with(itemView)
                    .load(albumArtUri)
//                    .load(posBitmap(position))
                    .apply(options)
                    .thumbnail(
                        Glide.with(itemView)
                            .load(posBitmap(position))
                            .apply(options)
                    )

                    .into(img)
            }


            holder.itemView.setOnClickListener {
                if (activity == null || activity?.isDestroyed == true|| activity?.isFinishing == true){
                    return@setOnClickListener
                }
                val ctx = activity as? EiMainPage
                ctx?.apply {
                    startInterActivity(Intent(this, EiAlbumDetail::class.java).apply {
                        putExtra("type", position)
                        putExtra("album_id", song.albumid.toString())
                        putExtra("title", song.name)
                        putExtra("art_name", song.artist)
                    }, albumDetInter(),false)
                }
            }

        }


        override fun getItemCount(): Int = items.size

    }



}