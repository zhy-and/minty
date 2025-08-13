package com.rapid.prominence.minty.second.view

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.RatingBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.appbar.AppBarLayout
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.MUSIC_CONSTANT_CURRENT_PAGE
import com.rapid.prominence.minty.cons.StopMusicOnTaskClearKey
import com.rapid.prominence.minty.cons.collectInter
import com.rapid.prominence.minty.cons.historyInter
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.releaseDetailInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.databinding.MintySecMainBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.logClickEvents
import com.rapid.prominence.minty.second.appbar.AppBarStateChangeListener
import com.rapid.prominence.minty.second.view.favorite.MintySecFavPage
import com.rapid.prominence.minty.second.SecGridItemDecoration
import com.rapid.prominence.minty.second.view.history.HistoryView
import com.rapid.prominence.minty.second.SecMainLayoutAdapter
import com.rapid.prominence.minty.second.SecPlayer
import com.rapid.prominence.minty.second.HomeStyleAdapter
import com.rapid.prominence.minty.second.SecReleaseAlbumAdapter
import com.rapid.prominence.minty.second.MintyReDetailPage

import com.rapid.prominence.minty.second.databean.HomeViewModel
import com.rapid.prominence.minty.service.MusicService
import com.rapid.prominence.minty.service.PlayerConnection
import com.rapid.prominence.minty.special.SpecialAbout
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.getInstallSys
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import timber.log.Timber

class MintySecMainPage : MintyBaseActivity() {
    override fun onStart() {
        super.onStart()
        startService(Intent(this@MintySecMainPage, MusicService::class.java))
        bindService(Intent(this@MintySecMainPage, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    companion object {
        var playerConnection by mutableStateOf<PlayerConnection?>(null)
        var canShowFlag = false
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is  MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@MintySecMainPage, service, database, lifecycleScope)
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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
           //todo
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown( keyCode,event)
    }
    private lateinit var database: MusicDatabase
    private lateinit var binding: MintySecMainBinding
    private  var secMainLayoutAdapter: SecMainLayoutAdapter?= null
    private  var secReleaseAlbumAdapter: SecReleaseAlbumAdapter?= null
    private  var homeStyleAdapter: HomeStyleAdapter?= null


    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        try {
            if (playerConnection != null && playerConnection?.player?.currentMetadata != null){
                playerConnection?.mediaMetadata?.let {
                    updatePlayerUI(it.value)
                }
            }
            if (!MMKV.defaultMMKV().getBoolean("showSuc",false)
                && (System.currentTimeMillis() - getInstallSys() > 12 * 60 * 60 * 1000)  && canShowFlag
            ) {
                isShowStar()
            }
        }catch (e : Exception){
            e.printStackTrace()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey,false)&& playerConnection?.isPlaying?.value == true && isFinishing){
            stopService(Intent(this,MusicService::class.java))
            serviceConnection?.let {
                unbindService(it)
            }
            playerConnection = null
        }
    }
    private fun updatePlayerUI(metadata: MediaMetadata?) {
        addUIPlayer(binding.bottomPlayer.artist,binding.bottomPlayer.title,binding.bottomPlayer.thumbnail,metadata)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database =  InternalDatabase.newInstance(this)
        val homeViewModel = HomeViewModel(this, database)
        binding = MintySecMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MMKV.defaultMMKV().putString(MUSIC_CONSTANT_CURRENT_PAGE,"snap")
            //todo
        binding.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?) {
                state ?: return
                when (state) {
                    State.COLLAPSED -> {
//                        binding.collapsing.setBackgroundResource(R.mipmap.bg3)
                        binding.searchEditText.visibility = View.VISIBLE
                        binding.a.visibility = View.GONE
                        binding.searchEditText2.visibility = View.GONE
//                        binding.tools.fitsSystemWindows = true

//                        binding.searchEditText2.visibility = View.VISIBLE
                    }
                    State.EXPANDED -> {
//                        binding.appbar.setBackgroundResource(R.mipmap.bg2)
                        binding.searchEditText.visibility = View.GONE
                        binding.a.visibility = View.VISIBLE
                        binding.searchEditText2.visibility = View.VISIBLE
//                        binding.tools.fitsSystemWindows = false
//                        binding.searchEditText2.visibility = View.VISIBLE
                    }

                    State.IDLE -> {}

                }
            }


        })


        binding.specAnm.post {
            binding.specAnm.visibility = View.VISIBLE
            binding.specAnm.startShimmerAnimation()
        }
        secMainLayoutAdapter = SecMainLayoutAdapter(this)
        secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@MintySecMainPage)
        homeStyleAdapter = HomeStyleAdapter(this)

        // Set up RecyclerViews
        binding.homePageSectionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.homePageSectionsRecyclerView.adapter = secMainLayoutAdapter

        binding.newReleaseAlbumsRecyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false)
        binding.newReleaseAlbumsRecyclerView.adapter = secReleaseAlbumAdapter

        binding.moodAndGenresRecyclerView.layoutManager = GridLayoutManager(this, 2)
//        binding.moodAndGenresRecyclerView?.addItemDecoration(SpaceItemDecoration( SizeUtils.dp2px( 10f)))
        binding.moodAndGenresRecyclerView?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 10f),2))
        binding.moodAndGenresRecyclerView.adapter = homeStyleAdapter
//        binding.moodAndGenresRecyclerView.isNestedScrollingEnabled = false
//        binding.newReleaseAlbumsRecyclerView.isNestedScrollingEnabled = false
//        binding.homePageSectionsRecyclerView.isNestedScrollingEnabled = false
        binding.searchEditText.setOnClickListener {
            startSearchPage()
            logClickEvents("home_to_search")
        }
        binding.searchEditText2.setOnClickListener {
            startSearchPage()
            logClickEvents("home_to_search")
        }
        binding.listButton.setOnClickListener {
            startQueuePage()
            logClickEvents("home_to_queue")

        }
        binding.history.setOnClickListener {
            logClickEvents("home_to_history")

            startInterActivity(Intent(this, HistoryView::class.java), historyInter(),false)
        }
        binding.fav.setOnClickListener {
            logClickEvents("home_to_collect")

            startInterActivity(Intent(this, MintySecFavPage::class.java), collectInter(),false)

        }
        binding.bottomPlayer.container.setOnClickListener {
            logClickEvents("home_to_player")
            startInterActivity(Intent(this, SecPlayer::class.java), playViewsInter(),false)
        }
//        binding.bottomPlayer.nextButton.setOnClickListener {
//            playerConnection?.player?.seekToNext()
//
//        }
        binding.emoScan.setOnClickListener {
            logClickEvents("home_refresh_btn")
            binding.specAnm.post {
                binding.specAnm.visibility = View.VISIBLE
                binding.specAnm.startShimmerAnimation()
            }
            homeViewModel?.refresh()
        }
        binding.newReleaseTitle.setOnClickListener {
            logClickEvents("home_to_newRelease")

            startInterActivity(Intent(this, MintyReDetailPage::class.java), releaseDetailInter(),false)
        }

//        binding.scroll.post {
//            binding.moodAndGenresRecyclerView?.parent?.apply {
//
//            }
//        }
        binding.endBtn.setOnClickListener {
            logClickEvents("home_to_about")

            startInterActivity(Intent(this, SpecialAbout::class.java), 0,false)

        }


        lifecycleScope.launch {
//            loadHomePageFromMMKV()
            homeViewModel?.homePage?.collect {
                if (isFinishing || isDestroyed){
                    return@collect
                }
                binding.specAnm.visibility = View.GONE
                binding.specAnm.stopShimmerAnimation()
                if (binding.emptyLayout.isVisible){
                    binding.emptyLayout.visibility = View.GONE
                }
                Timber.e(it.toString())
                if (it == null) {
                    return@collect
                }
                // Set data to adapters
//                    withContext(Dispatchers.Main){
                it?.sections?.apply {
                    secMainLayoutAdapter?.submitList(this)
                }



                homeViewModel?.explorePage?.collect {
                    try {
                        if (isFinishing || isDestroyed){
                            return@collect
                        }
                        binding.specAnm.visibility = View.GONE
                        binding.specAnm.stopShimmerAnimation()
                        binding.newReleaseTitle.visibility = View.VISIBLE
                        binding.moodTitle.visibility = View.VISIBLE
                        it?.newReleaseAlbums?.apply {
                            secReleaseAlbumAdapter?.submitList(this)
                        }
                        it?.moodAndGenres?.apply {
                            homeStyleAdapter?.submitList(this)
                        }
                        Timber.e(it.toString())

                    }catch (e : Exception){
                        e.printStackTrace()
                    }


                }


            }
        }
//

        lifecycleScope.launch {
            try {
                homeViewModel?.isError?.collect {
                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    if (isFinishing || isDestroyed){
                        return@collect
                    }
                    if (it){
                        binding.emptyLayout.visibility = View.VISIBLE
                    }
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }


    }
    private fun isShowStar() {
        try {
            val dialog =   showDialogs(this, R.layout.minty_dialog_star,false, Gravity.CENTER)
            dialog.show()
            val ratingBar = dialog.findViewById<RatingBar>(R.id.rating_bar)
            ratingBar?.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                ThreadUtils.runOnUiThreadDelayed({
                    dialog.dismiss()
                    MMKV.defaultMMKV().putBoolean("showSuc",true)
                    if (rating == 5f) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.data =
                                Uri.parse("market://details?id=" + AppUtils.getAppPackageName()) //跳转到应用市场，非Google Play市场一般情况也实现了这个接口
                            //存在手机里没安装应用市场的情况，跳转会包异常，做一个接收判断
                            if (intent.resolveActivity(packageManager) != null) { //可以接收
                                startActivity(intent)
                            } else { //没有应用市场，我们通过浏览器跳转到Google Play
                                intent.data =
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + AppUtils.getAppPackageName())
                                //这里存在一个极端情况就是有些用户浏览器也没有，再判断一次
                                if (intent.resolveActivity(packageManager) == null) { //有浏览器
                                    startActivity(intent)
                                } else { //
                                    ToastUtils.showShort(
                                        "You don't have an app market installed, not even a browser!",
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },1000)

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



}
