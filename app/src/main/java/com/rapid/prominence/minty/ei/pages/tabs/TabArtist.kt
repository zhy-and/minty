package com.rapid.prominence.minty.ei.pages.tabs

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.ei.scan.EiScanTools
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.databinding.TabArtistBinding
import com.rapid.prominence.minty.ei.scan.Artist
import com.rapid.prominence.minty.ei.kts.mmkvs
import com.rapid.prominence.minty.ei.kts.mmkvs.initCreateData
import com.rapid.prominence.minty.posBitmap
import com.tencent.mmkv.MMKV
import jp.wasabeef.glide.transformations.CropCircleTransformation


class TabArtist : Fragment() {

    private  var _binding: TabArtistBinding?= null
    private  val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabArtistBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.emoScan?.setOnClickListener {

            loadAlbums(true)
        }
        setupRecyclerView()
        if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
            binding?.emptyLayout?.visibility = View.VISIBLE
            binding.imgs?.visibility = View.GONE
        }else{
            loadAlbums(false)
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var adapter: ArtistAdapter? = null
    private var songAdapter: TabHome.SongAdapter? = null



    override fun onResume() {
        super.onResume()
        if (binding?.emptyLayout?.visibility == View.VISIBLE &&  MMKV.defaultMMKV().getBoolean("isAuth",false)){
            val songs = EiScanTools.getLocalArtists(requireContext()) as ArrayList<Artist>
            if (songs.isNullOrEmpty()){
                binding?.emptyLayout?.visibility = View.VISIBLE
                binding?.recyclerView?.visibility = View.GONE
                binding?.lines?.visibility = View.GONE
                binding?.emIv?.setImageResource(R.mipmap.empty_logo)
                binding?.emTx?.text = "No music found in this device!"
                return
            }
            binding?.emptyLayout?.visibility = View.GONE
            binding?.recyclerView?.visibility = View.VISIBLE
            binding?.imgs?.visibility = View.VISIBLE

            binding?.songList?.visibility = View.VISIBLE
            binding?.lines?.visibility = View.GONE
            adapter?.add(songs)
            loadSong(false)
        }
    }

    private fun setupRecyclerView() {
        binding?.recyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = ArtistAdapter()
        binding?.recyclerView?.adapter = adapter

        binding?.songList?.layoutManager = LinearLayoutManager(context)
        songAdapter = TabHome.SongAdapter()
        binding?.songList?.adapter = songAdapter
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
                                ctx.animation()
                            }
                        }
                        MMKV.defaultMMKV().putBoolean("isAuth",true)
                        val songs = EiScanTools.getLocalArtists(requireContext()) as ArrayList<Artist>
                        if (songs.isNullOrEmpty()){
                            binding?.emptyLayout?.visibility = View.VISIBLE
                            binding?.recyclerView?.visibility = View.GONE
                            binding?.lines?.visibility = View.GONE
                            binding?.emIv?.setImageResource(R.mipmap.empty_logo)
                            binding?.emTx?.text = "No music found in this device!"
                            return@request
                        }
                        binding?.emptyLayout?.visibility = View.GONE
                        binding?.recyclerView?.visibility = View.VISIBLE
                        binding?.imgs?.visibility = View.VISIBLE
                        binding?.songList?.visibility = View.VISIBLE
                        binding?.lines?.visibility = View.GONE
                        adapter?.add(songs)
                        loadSong(b)
                    }
                }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun loadSong(b: Boolean) {
        try {
            mmkvs.loadSongs(Song()).let { songs ->
                if (songs.size == 0){
                    val songs = EiScanTools.getLocalSongs(requireContext()) as ArrayList<Song>
                    initCreateData(songs)
                    if (b){
                        activity?.apply {
                            if (isFinishing || isDestroyed){
                                return@apply
                            }
                            val ctx = this as EiMainPage
                            ctx.updateUI()
                        }
                    }
                }
            }

        }catch (e : Exception){
            e.printStackTrace()
        }

    }


    inner class ArtistAdapter : RecyclerView.Adapter<ArtistAdapter.ViewHolder>() {
        var items: ArrayList<Artist> = arrayListOf()
        fun add( datas: ArrayList<Artist>){
            try {
                items = datas
                if (items.size > 0){
                    items?.forEachIndexed() { index,it ->
                        if (index == 0){
                            it.isSelect = true
                        }else{
                            it.isSelect = false
                        }
                    }
                    loadSongs(items.firstOrNull()?.key)
                }
                notifyDataSetChanged()
            }catch (e : Exception){
                e.printStackTrace()
            }

        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val tvTitle: TextView = itemView.findViewById<TextView>(R.id.title)
            val tvArtist: TextView = itemView.findViewById<TextView>(R.id.artist)
//            val tvDuration: TextView = itemView.findViewById<TextView>(R.id.count)
            val img: ImageView = itemView.findViewById(R.id.thumbnail)
            val select_bg: ImageView = itemView.findViewById(R.id.select_bg)
            val unselect_bg: ImageView = itemView.findViewById(R.id.unselect_bg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.artist_ei_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val song = items[position]
            holder.apply {
                if (song.isSelect){
                    select_bg.visibility = View.VISIBLE
                    unselect_bg.visibility = View.GONE
                }else{
                    select_bg.visibility = View.INVISIBLE
                    unselect_bg.visibility = View.VISIBLE
                }
                tvArtist.text = song.name
//                tvArtist.text = song.albumCount.toString()
//                tvDuration.text = "gong --"+song.songCount

                Glide.with(holder.itemView.context).asBitmap()
                    .apply(RequestOptions.bitmapTransform(CropCircleTransformation())) // 设置圆形图片
                    .load(posBitmap(position)).into(holder.img)
            }

            holder.itemView.setOnClickListener {
                items?.forEachIndexed() { index,it ->
                    if (index == position){
                        it.isSelect = true
                    }else{
                        it.isSelect = false
                    }
                }
               notifyDataSetChanged()
                loadSongs(song.key)
            }

        }
        private fun loadSongs(id: String?) {
            try {
                Thread{
                    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    var selection: String? = null
                    var selectionArgs: Array<String?>? = null

                    // 构建查询条件‌:ml-citation{ref="5,6" data="citationList"}
                    selection = MediaStore.Audio.Media.ARTIST_KEY + "=?"
                    selectionArgs = arrayOf(id)

                    context?.contentResolver?.query(
                        uri,
                        arrayOf<String>(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ALBUM_ID,
                        ),
                        selection,
                        selectionArgs,
                        MediaStore.Audio.Media.TITLE + " ASC"
                    ).use { cursor ->
                        val songs = ArrayList<Song>()
                        if (cursor != null) {
                            val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                            val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                            val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                            val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                            val pathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                            while (cursor.moveToNext()) {
                                songs.add(
                                    Song(
                                        cursor.getLong(idIndex),
                                        cursor.getString(titleIndex),
                                        cursor.getString(artistIndex),
                                        cursor.getLong(durationIndex),
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
                                        cursor.getLong(idIndex),
                                        cursor.getString(pathIndex),
                                        cursor.getString(albumColumn),
                                        cursor.getLong(albumIdColumn)
                                    )
                                )
                            }
                        }
                        activity?.runOnUiThread {
                            songAdapter?.add(songs)
                        }
                    }
                }.start()
            }catch (e : Exception){
                e.printStackTrace()
            }


        }


        override fun getItemCount(): Int = items.size

    }



}