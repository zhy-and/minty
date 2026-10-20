package com.rapid.prominence.minty.ei.main

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
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
import com.blankj.utilcode.util.ThreadUtils
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.Song
import com.rapid.prominence.minty.cons.MINTY_APP_CURRENT_PAGE
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
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
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.rapid.prominence.minty.widget.MusicPlayerAppWidgetProvider
import com.rapid.prominence.minty.widget.WidgetEntryUi
import com.rapid.prominence.minty.runtime.NotificationToolbarHelper
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import media.library.manager.MintyApi

class EiMainPage : MintyBaseActivity() {
    private var pendingOpenPlayer = false

    private fun openPlayerIfRequested() {
        if (pendingOpenPlayer && eiPlays != null) {
            pendingOpenPlayer = false
            startActivity(Intent(this, EiPlayer::class.java))
        }
    }

    private fun openToolbarTabIfRequested() {
        when (intent.getStringExtra(NotificationToolbarHelper.EXTRA_ACTION)) {
            "favorites" -> binding.viewPager.setCurrentItem(3, false)
            "library" -> binding.viewPager.setCurrentItem(0, false)
        }
        intent.removeExtra(NotificationToolbarHelper.EXTRA_ACTION)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenPlayer = intent.getBooleanExtra(MusicPlayerAppWidgetProvider.EXTRA_OPEN_PLAYER, false)
        WidgetEntryUi.requestFromNotification(this, intent)
        openToolbarTabIfRequested()
        openPlayerIfRequested()
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this@EiMainPage, MusicService::class.java))
        bindService(Intent(this@EiMainPage, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    companion object {
        var eiPlays by mutableStateOf<PlayerConnection?>(null)
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
        if (eiPlays?.queueWindows != null && eiPlays?.queueWindows?.value?.isEmpty() == true){
            eiPlays?.addToQueue(mediaItems)
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
                eiPlays = PlayerConnection(this@EiMainPage, service, database, lifecycleScope)
                lifecycleScope.launch {
                    eiPlays?.mediaMetadata?.collect{
                        if (MMKV.defaultMMKV().getBoolean("isAuth",false)){
                            bottomSettingChange(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    eiPlays?.isPlaying?.collect{
                        if (it){
                            binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.pause)
                        }else{
                            binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.mipmap.home_plays)
                        }
                    }
                }
                if (isReload){
                    updateUI()
                }
                openPlayerIfRequested()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eiPlays?.dispose()
            eiPlays = null
        }
    }

    override fun handleBackPressed() {
        WidgetEntryUi.showExitRetain(this) { moveTaskToBack(false) }
    }
    private lateinit var database: MusicDatabase
    private lateinit var binding: MintyEiHomeBinding
    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) && eiPlays?.isPlaying?.value == true && isFinishing) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            eiPlays = null
        }
    }
    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        // Home-top notification/widget tips are intentionally disabled for now.
        requestNotificationPermissionAtHome()
        try {
            //
            if (MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER,false)){
                showLottie()
                ThreadUtils.runOnUiThreadDelayed({
                    try {
                        cancelLoadingBar()
                        startActivity(Intent(this, MintySecMainPage::class.java))
                        finish()
                    }catch (e : Exception){
                        e.printStackTrace()
                    }

                },2000)
            }
            MintyApi.pageAEventsUpload()
            if (eiPlays != null && eiPlays?.player?.currentMetadata != null && MMKV.defaultMMKV().getBoolean("isAuth",false)){
                eiPlays?.mediaMetadata?.let {
                    bottomSettingChange(it.value)
                }
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    private fun bottomSettingChange(metadata: MediaMetadata?) {
        if (!TextUtils.isEmpty(metadata?.title)){
            binding.mintyIncContainerPlay.title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            binding.mintyIncContainerPlay.artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        Glide.with(this@EiMainPage)
            .load(metadata?.thumbnailUrl)
            .apply(roundOptions)
            .thumbnail(
                Glide.with(this)
                    .load(posBitmap(0))
                    .apply(roundOptions)
            )
            .into(binding.mintyIncContainerPlay.thumbnail)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpenPlayer = intent.getBooleanExtra(MusicPlayerAppWidgetProvider.EXTRA_OPEN_PLAYER, false)
        binding = MintyEiHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WidgetEntryUi.requestFromNotification(this, intent)
        MMKV.defaultMMKV().putString(MINTY_APP_CURRENT_PAGE,"local")
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
        openToolbarTabIfRequested()
        setupTabLayout()
        // Initialize adapters
        // Set up RecyclerViews
        binding.mintyIncContainerPlay.container.setOnClickListener {
            if (!MMKV.defaultMMKV().getBoolean("isAuth",false)){
                home?.outClick()
                return@setOnClickListener
            }
            startActivity(Intent(this, EiPlayer::class.java))
        }



//        binding.mintyIncContainerPlay.nextButton.setOnClickListener {
//            playerConnection?.player?.seekToNext()
//        }
//        binding.mintyIncContainerPlay.playPauseButton.setOnClickListener {
//            if (playerConnection?.isPlaying?.value == true){
//                    binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.drawable.play)
//            }else{
//                binding.mintyIncContainerPlay.playPauseButton.setImageResource(R.drawable.pause)
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
            if (eiPlays == null){
                isReload = true
                return
            }
            val data =  loadSongs(Song())
            if (data.isNotEmpty()){
                authGetLocalData(data)
            }
            eiPlays?.mediaMetadata?.let {
                bottomSettingChange(it.value)
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
