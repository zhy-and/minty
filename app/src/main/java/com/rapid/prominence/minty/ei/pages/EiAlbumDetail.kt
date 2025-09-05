package com.rapid.prominence.minty.ei.pages
import android.content.ContentUris
import com.rapid.prominence.minty.ei.main.EiMainPage.Companion.eiPlays
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.databinding.MintyEiAlbumDetBinding
import com.rapid.prominence.minty.ei.CornerTransform
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.playLocalMusic
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.ei.EiPlayer
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.albumDetBanner
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.ei.pages.tabs.TabHome
import com.rapid.prominence.minty.roundOptions
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool


class EiAlbumDetail : MintyBaseActivity() {
    private lateinit var binding: MintyEiAlbumDetBinding

    private var adapter: TabHome.SongAdapter? = null
    private fun loadSpeNative() {
        val normalBan: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            albumDetBanner(), this)
        if (normalBan == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("albumDetBanner  ban inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("albumDetBanner  ban destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(albumDetBanner())) {
                LogUtils.e("albumDetBanner  ban limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(albumDetBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed) {
                        return
                    }
                    if (sucFlag) {
                        LogUtils.e("albumDetBanner  ban load  suc------")
                        loadSpeNative()
                    }
                }
            })
        } else {
            LogUtils.e("albumDetBanner  ban show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(normalBan)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeMintyTools.homeStudy.getShowCyAds(albumDetInter(),this)

        binding = MintyEiAlbumDetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.mintyIncContainerPlay.container.setOnClickListener {
            startActivity(Intent(this, EiPlayer::class.java))
        }
        val queryType = intent.getIntExtra("type",0)
        val queryId = intent.getStringExtra("album_id") ?: return
        loadSongs( queryId)
        val title = intent.getStringExtra("title")
        val art_name = intent.getStringExtra("art_name")
        binding.title.text = title
        binding.artist.text = art_name
        binding.playAlbum.setOnClickListener {
           if (adapter?.items.isNullOrEmpty()){
               return@setOnClickListener
           }

            adapter?.items?.let {
                playLocalMusic(it,0)
            }

            val intent = Intent(
                this,
                EiPlayer::class.java
            )
            startActivity(intent)
        }
        binding.closeButton.setOnClickListener {
            finish()
        }

        try {
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                queryId.toLong()
            )
            val cornerTransform = CornerTransform(this, SizeUtils.dp2px(52f).toFloat())
            cornerTransform.setNeedCorner(false, false, true, false)
            val options: RequestOptions = RequestOptions()
                .placeholder(posBitmap(queryType))
                .error(posBitmap(queryType))
            Glide.with(this)
//                .load(posBitmap(queryType))
                .load(albumArtUri)
                .apply(options)
                .thumbnail(
                    Glide.with(this)
                        .load(posBitmap(queryType))
                        .apply(options)
                )
                .into(binding.album)

            lifecycleScope.launch {
                eiPlays?.isPlaying?.collect{
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


    private fun setupRecyclerView() {
        binding.listRecycleview.layoutManager = LinearLayoutManager(this)
        adapter =
            TabHome.SongAdapter() // 传递点击监听器‌:ml-citation{ref="1,7" data="citationList"}
        binding.listRecycleview.adapter = adapter
    }

    private fun loadSongs(id: String?) {
        try {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            var selection: String? = null
            var selectionArgs: Array<String?>? = null

            // 构建查询条件‌:ml-citation{ref="5,6" data="citationList"}
//        if ("album" == type) {
            selection = MediaStore.Audio.Media.ALBUM_ID + "=?"
            selectionArgs = arrayOf(id)
//        }
//        else if ("artist" == type) {
//            selection = MediaStore.Audio.Media.ARTIST_KEY + "=?"
//            selectionArgs = arrayOf(id)
//        }

            contentResolver.query(
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
//                Log.e("------",",,"+songs.size)
                adapter?.add(songs)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }
    override fun onResume() {
        super.onResume()
        if (eiPlays != null && eiPlays?.player?.currentMetadata != null && MMKV.defaultMMKV().getBoolean("isAuth",false)){
            eiPlays?.mediaMetadata?.let {
                bottomSettingChange(it.value)
            }
        }
        loadSpeNative()
    }


    private fun bottomSettingChange(metadata: MediaMetadata?) {
        if (!TextUtils.isEmpty(metadata?.title)){
            binding.mintyIncContainerPlay.title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            binding.mintyIncContainerPlay.artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        Glide.with(this@EiAlbumDetail)
            .load(metadata?.thumbnailUrl)
            .apply(roundOptions)
            .thumbnail(
                Glide.with(this)
                    .load(posBitmap(0))
                    .apply(roundOptions)
            )
            .into(binding.mintyIncContainerPlay.thumbnail)
    }

}