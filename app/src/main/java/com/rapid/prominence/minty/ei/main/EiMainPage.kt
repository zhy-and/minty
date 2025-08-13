package com.rapid.prominence.minty.ei.main

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.cons.MUSIC_CONSTANT_CURRENT_PAGE
import com.rapid.prominence.minty.cons.StopMusicOnTaskClearKey
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.databinding.MintyEiHomeBinding
import com.rapid.prominence.minty.ei.kts.mmkvs.loadSongs
import com.rapid.prominence.minty.ei.EiPlayer
import com.rapid.prominence.minty.ei.pages.tabs.TabArtist
import com.rapid.prominence.minty.ei.pages.tabs.TabFavorite
import com.rapid.prominence.minty.ei.pages.EiMintyQueue
import com.rapid.prominence.minty.ei.pages.tabs.TabHome
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.ei.kts.toMediaMetadata
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.service.MusicService
import com.rapid.prominence.minty.service.PlayerConnection
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.special.SpecialAbout
import com.rapid.prominence.minty.ei.pages.tabs.TabAlbum
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.roundOptions
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch

class EiMainPage : MintyBaseActivity() {
    override fun onStart() {
        super.onStart()
        startService(Intent(this@EiMainPage, MusicService::class.java))
        bindService(Intent(this@EiMainPage, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    companion object {
        var playerConnection by mutableStateOf<PlayerConnection?>(null)
    }
    private fun authGetLocalData(localMusic: ArrayList<Song>) {
        val mediaItems = localMusic.map { music ->
            MediaItem.Builder()
                .setMediaId(music.id.toString())
                .setUri(Uri.parse(music.uri).buildUpon().appendPath(music.uriId.toString()).build())
                .setCustomCacheKey(music.id.toString())
                .setTag(music.toMediaMetadata())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(music.title)
                        .setSubtitle(music.artist)
                        .setArtist(music.artist)
//                        .setArtworkUri(music.albumArtUri.uri)
//                        .setAlbumTitle(music.album)
                        .setMediaType(MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()

        }
        if (playerConnection?.queueWindows != null && playerConnection?.queueWindows?.value?.isEmpty() == true){
            playerConnection?.addToQueue(mediaItems)
        }

    }
    private fun onPagerSelected(position: Int) {
        when (position) {
            0 -> {
                binding.pageHome.setBackgroundResource(R.drawable.home_tab_shape)
                binding.homeStatus.setTextColor(Color.parseColor("#2EEB69"))
                binding.favStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.albumStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.artistStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.pageArtist.background = null
                binding.pageAlbum.background = null
                binding.pageFav.background = null
            }

            1 -> {

                binding.pageArtist.setBackgroundResource(R.drawable.home_tab_shape)
                binding.artistStatus.setTextColor(Color.parseColor("#2EEB69"))
                binding.favStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.albumStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.homeStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.pageHome.background = null
                binding.pageAlbum.background = null
                binding.pageFav.background = null
            }

            2 -> {
                binding.pageAlbum.setBackgroundResource(R.drawable.home_tab_shape)
                binding.albumStatus.setTextColor(Color.parseColor("#2EEB69"))
                binding.favStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.homeStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.artistStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.pageArtist.background = null
                binding.pageHome.background = null
                binding.pageFav.background = null

            }

            3 -> {
                binding.pageFav.setBackgroundResource(R.drawable.home_tab_shape)
                binding.favStatus.setTextColor(Color.parseColor("#2EEB69"))
                binding.homeStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.albumStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.artistStatus.setTextColor(Color.parseColor("#FFFFFF"))
                binding.pageArtist.background = null
                binding.pageAlbum.background = null
                binding.pageHome.background = null
            }

        }
    }

    private  var home: TabHome?= null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@EiMainPage, service, database, lifecycleScope)
                lifecycleScope.launch {
                    playerConnection?.mediaMetadata?.collect{
                        if (MMKV.defaultMMKV().getBoolean("isAuth",false)){
                            updatePlayerUI(it)
                        }
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
                if (isReload){
                    updateUI()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown( keyCode,event)
    }
    private lateinit var database: MusicDatabase
    private lateinit var binding: MintyEiHomeBinding
    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) && playerConnection?.isPlaying?.value == true && isFinishing) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }
    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        try {
            if (playerConnection != null && playerConnection?.player?.currentMetadata != null && MMKV.defaultMMKV().getBoolean("isAuth",false)){
                playerConnection?.mediaMetadata?.let {
                    updatePlayerUI(it.value)
                }
                if (playerConnection?.mediaMetadata == null){
                    //todo
                }
            }
            //    //todo
//            if (MMKV.defaultMMKV().getBoolean(MUSIC_CONSTANT_IS_M_USER,false)){
//                binding.progressBar.visibility = View.VISIBLE
//                ThreadUtils.runOnUiThreadDelayed({
//                    try {
//
//                        startActivity(Intent(this, HomeActivity::class.java))
//                        finish()
//                    }catch (e : Exception){
//                        e.printStackTrace()
//                    }
//
//                },2000)
//            }
//            SnapBasicInterfaces.pageAEventsUpload()
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun updatePlayerUI(metadata: MediaMetadata?) {
        if (!TextUtils.isEmpty(metadata?.title)){
            binding.bottomPlayer.title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            binding.bottomPlayer.artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        Glide.with(this@EiMainPage)
            .load(metadata?.thumbnailUrl)
            .apply(roundOptions)
            .thumbnail(
                Glide.with(this)
                    .load(posBitmap(0))
                    .apply(roundOptions)
            )
            .into(binding.bottomPlayer.thumbnail)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MintyEiHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MMKV.defaultMMKV().putString(MUSIC_CONSTANT_CURRENT_PAGE,"local")

        database =  InternalDatabase.newInstance(this)
        binding.pageHome.setOnClickListener {
            binding.viewPager.setCurrentItem(0,false)
        }
        binding.pageArtist.setOnClickListener {
            binding.viewPager.setCurrentItem(1,false)
        }
        binding.pageAlbum.setOnClickListener {
            binding.viewPager.setCurrentItem(2,false)
        }
        binding.pageFav.setOnClickListener {
            binding.viewPager.setCurrentItem(3,false)
        }
        setupViewPager()
        setupTabLayout()
        // Initialize adapters
        // Set up RecyclerViews
        binding.bottomPlayer.container.setOnClickListener {
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                home?.outClick()
                return@setOnClickListener
            }
            startActivity(Intent(this, EiPlayer::class.java))
        }
//        binding.bottomPlayer.nextButton.setOnClickListener {
//            playerConnection?.player?.seekToNext()
//        }
//        binding.bottomPlayer.playPauseButton.setOnClickListener {
//            if (playerConnection?.isPlaying?.value == true){
//                    binding.bottomPlayer.playPauseButton.setImageResource(R.drawable.play)
//            }else{
//                binding.bottomPlayer.playPauseButton.setImageResource(R.drawable.pause)
//            }
//            if (playerConnection?.player?.playbackState == Player.STATE_ENDED) {
//                playerConnection?.player?.seekTo(0, 0)
//                playerConnection?.player?.playWhenReady = true
//            } else {
//                playerConnection?.player?.togglePlayPause()
//            }
//        }
    }


    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
         home = TabHome()
        home?.let {
            adapter.addFragment(it, "Playlist")
        }
        adapter.addFragment(TabArtist(), "Artist")
        adapter.addFragment(TabAlbum(), "Album")
        adapter.addFragment(TabFavorite(), "Favorite")
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                onPagerSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }
    var sizes = 4
    private fun setupTabLayout() {

        binding.tops.lists.setOnClickListener {
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                home?.outClick()
                return@setOnClickListener
            }
            startInterActivity(Intent(this, EiMintyQueue::class.java), queueInter(),false)
        }
        binding.tops.closeButton.setOnClickListener {
            startActivity(Intent(this, SpecialAbout::class.java))
        }
    }



    var isReload = false
    fun updateUI() {
        try {
            if (playerConnection == null){
                isReload = true
                return
            }
            val data =  loadSongs(Song())
            if (data.isNotEmpty()){
                authGetLocalData(data)
            }
            playerConnection?.mediaMetadata?.let {
                updatePlayerUI(it.value)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    fun animation() {
        try {
            val dialog =  showDialogs(this, R.layout.dialog_ei_pop)
            binding?.root?.postDelayed({
                if (dialog.isShowing && !isFinishing && !isDestroyed){
                    dialog.dismiss()
                }
            },3000)
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    fun denPermission() {
        startActivity(Intent(this, EiPlayer::class.java))
    }

    inner  class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        private val fragments: MutableList<Fragment> = ArrayList<Fragment>()
        private val titles: MutableList<String> = ArrayList()

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            titles.add(title)
        }

        fun getPageTitle(position: Int): String {
            return titles[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }

}
