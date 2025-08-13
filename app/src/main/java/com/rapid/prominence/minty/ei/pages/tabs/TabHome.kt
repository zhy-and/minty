package com.rapid.prominence.minty.ei.pages.tabs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.ei.scan.EiScanTools
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.databinding.TabHomeBinding
import com.rapid.prominence.minty.ei.kts.mmkvs.initCreateData
import com.rapid.prominence.minty.ei.kts.mmkvs.loadSongs
import com.rapid.prominence.minty.playLocalMusic
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.ei.EiPlayer
import com.tencent.mmkv.MMKV
import java.util.concurrent.TimeUnit


class TabHome : Fragment() {
    private var adapter: SongAdapter? = null
    private var horAdapter: SongAdapter2? = null
    private  var _binding: TabHomeBinding?= null
    private  val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initView()
        if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
            binding?.emptyLayout?.visibility = View.VISIBLE
            binding.all?.visibility = View.GONE
            binding.imgs?.visibility = View.GONE
        }else{
            loadData(false)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun outClick(){
        loadData(true,true)
    }
    private fun initView() {
        binding?.emoScan?.setOnClickListener {

            loadData(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding?.emptyLayout?.visibility == View.VISIBLE &&  MMKV.defaultMMKV().getBoolean("isAuth",false)){
            loadSongs(Song()).let { songs ->
                if (songs.size == 0){
                    val items = EiScanTools.getLocalSongs(requireContext()) as ArrayList<Song>
                    initCreateData(items)
                    if (items.size >= 2){
                        binding?.recommendList?.setLayoutManager(LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false))
                        horAdapter = SongAdapter2()
                        binding?.recommendList?.setAdapter(horAdapter)
                        horAdapter?.add(items.take(2) as ArrayList<Song>)
                    }
                }else{
                    if (songs.size >= 2){
                        binding?.recommendList?.setLayoutManager(LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false))
                        horAdapter = SongAdapter2()
                        binding?.recommendList?.setAdapter(horAdapter)
                        horAdapter?.add(songs.take(2) as ArrayList<Song>)
                    }
                }
                binding?.emptyLayout?.visibility = View.GONE
                binding?.all?.visibility = View.VISIBLE
                adapter?.add(songs)
            }

        }
    }

    private fun setupRecyclerView() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(context)
        adapter = SongAdapter()
        binding?.recyclerView?.adapter = adapter


    }

    private fun loadData(b: Boolean,isHome : Boolean = false) {
        try {
            XXPermissions.with(this).permission(Permission.READ_MEDIA_AUDIO)
                .request(object : OnPermissionCallback{
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        if (allGranted) {
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
                            val songs = EiScanTools.getLocalSongs(requireContext()) as ArrayList<Song>
                            if (songs.isNullOrEmpty()){
                                binding?.all?.visibility = View.GONE
                                binding?.emptyLayout?.visibility = View.VISIBLE
                                binding?.emIv?.setImageResource(R.mipmap.empty_logo)
                                binding?.emTx?.text = "No music found in this device!"
                                return
                            }

                            binding?.emptyLayout?.visibility = View.GONE
                            binding?.all?.visibility = View.VISIBLE
                            binding?.imgs?.visibility = View.VISIBLE

                            loadSongs(Song())?.apply {
                                if (isNullOrEmpty()){
                                    return@apply
                                }
                                forEach {its ->
                                    for (i in songs){
                                        if (its.albumId == i.albumId && its.title == i.title){
                                            i.isCollect = its.isCollect
                                        }

                                    }

                                }
                            }
                            initCreateData(songs)
                            adapter?.add(songs)

                            if (songs.size >= 2){
                                binding?.recommendList?.setLayoutManager(LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false))
                                horAdapter = SongAdapter2()
                                binding?.recommendList?.setAdapter(horAdapter)
                                horAdapter?.add(songs.take(2) as ArrayList<Song>)
                            }else{
                                binding?.text?.visibility = View.GONE
                                binding?.recommendList?.visibility = View.GONE
                            }
//                    if (b){
                            activity?.apply {
                                if (isFinishing || isDestroyed){
                                    return@apply
                                }
                                val ctx = this as EiMainPage
                                ctx.updateUI()
//                        }
                            }
                        }

                    }
                    override fun onDenied(permissions: MutableList<String>, allGranted: Boolean) {
//                        Log.e("permission","---sd-s")
                        if (isHome){
                            activity?.apply {
                                if (isFinishing || isDestroyed){
                                    return@apply
                                }
                                val ctx = this as EiMainPage
                                ctx.denPermission()
                            }
                        }
                    }
                })
        }catch (e: Exception){
            e.printStackTrace()
        }



    }





     class SongAdapter : RecyclerView.Adapter<SongAdapter.ViewHolder>() {
        var items: ArrayList<Song> = arrayListOf()
         var norTy = 0
        fun add( datas: ArrayList<Song>){
            items = datas
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById<TextView>(R.id.title)
             val tvArtist: TextView = itemView.findViewById<TextView>(R.id.title2)
             val tvDuration: TextView = itemView.findViewById<TextView>(R.id.subtitle)
             val ivgs: ImageView = itemView.findViewById<ImageView>(R.id.ivgs)
             val thumbnail: ImageView = itemView.findViewById<ImageView>(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.home_ei_item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val song = items[position]
            holder.apply {
                tvTitle.text = song.title
                tvArtist.text = song.artist
                tvDuration.text = formatDuration(song.duration)
                if (norTy == 1){
                    holder.ivgs.setImageResource(R.mipmap.py_collect)
                }else{
                    holder.ivgs.setImageResource(R.mipmap.play_green)
                }
            }
                Glide.with(holder.itemView.context).asBitmap().load(posBitmap(position))
                    .apply(RequestOptions().transform(RoundedCorners(SizeUtils.dp2px(6f))))
                    .into(holder.thumbnail)
            holder.itemView.setOnClickListener {
                playLocalMusic(items,position)

                // 处理歌曲点击事件‌:ml-citation{ref="7,8" data="citationList"}
                val intent = Intent(
                    holder.itemView.context,
                    EiPlayer::class.java
                )
               holder.itemView.context.startActivity(intent)
            }

        }



         private fun formatDuration(milliseconds: Long): String {
            return java.lang.String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
            )
        }

        override fun getItemCount(): Int = items.size
         fun setNormalType(i: Int) {
             norTy = i
         }
     }

    class SongAdapter2 : RecyclerView.Adapter<SongAdapter2.ViewHolder>() {
        var items: ArrayList<Song> = arrayListOf()
        fun add( datas: ArrayList<Song>){
            items = datas
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById<TextView>(R.id.title)
            val tvArtist: TextView = itemView.findViewById<TextView>(R.id.title2)
            val ivgs: ImageView = itemView.findViewById<ImageView>(R.id.ivgs)
            val thumbnail: ImageView = itemView.findViewById<ImageView>(R.id.thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.home_ei_item_recommend, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val song = items[position]
            holder.apply {
                tvTitle.text = song.title
                tvArtist.text = song.artist
            }
            Glide.with(holder.itemView.context).asBitmap().load(posBitmap(position))
                .apply(RequestOptions().transform(RoundedCorners(SizeUtils.dp2px(6f))))
                .into(holder.thumbnail)
            holder.itemView.setOnClickListener {



                playLocalMusic(items,position)
                // 处理歌曲点击事件‌:ml-citation{ref="7,8" data="citationList"}
                val intent = Intent(
                    holder.itemView.context,
                    EiPlayer::class.java
                )
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = items.size


    }


}