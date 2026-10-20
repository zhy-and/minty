package com.rapid.prominence.minty.second.view

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.View
import android.widget.RatingBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.appbar.AppBarLayout
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.application.App.Companion.isSkipLoad
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.bean.toMediaMetadata
import com.rapid.prominence.minty.cons.MESSAGE_MSG_CODE
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.MINTY_APP_CURRENT_PAGE
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.StopMusicOnTaskClearKey
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.collectInter
import com.rapid.prominence.minty.cons.historyInter
import com.rapid.prominence.minty.cons.homeBanner
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.playlistDetInter
import com.rapid.prominence.minty.cons.releaseDetailInter
import com.rapid.prominence.minty.database.InternalDatabase
import com.rapid.prominence.minty.database.MusicDatabase
import com.rapid.prominence.minty.databinding.MintySecMainBinding
import com.rapid.prominence.minty.ei.dialog.showDialogs
import com.rapid.prominence.minty.extensions.currentMetadata
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.second.HomeStyleAdapter
import com.rapid.prominence.minty.second.MintyReDetailPage
import com.rapid.prominence.minty.second.MintySecAlbumDetailPage
import com.rapid.prominence.minty.second.MintySecArtistDetailPage
import com.rapid.prominence.minty.second.MintySecPyDetailView
import com.rapid.prominence.minty.second.SecGridItemDecoration
import com.rapid.prominence.minty.second.SecHomeDetailAdapter
import com.rapid.prominence.minty.second.SecMainLayoutAdapter
import com.rapid.prominence.minty.second.SecPlayer
import com.rapid.prominence.minty.widget.MusicPlayerAppWidgetProvider
import com.rapid.prominence.minty.widget.WidgetEntryUi
import com.rapid.prominence.minty.second.SecReleaseAlbumAdapter
import com.rapid.prominence.minty.second.appbar.AppBarStateChangeListener
import com.rapid.prominence.minty.second.databean.HomeViewModel
import com.rapid.prominence.minty.second.view.favorite.MintySecFavPage
import com.rapid.prominence.minty.second.view.history.HistoryView
import com.rapid.prominence.minty.service.MusicService
import com.rapid.prominence.minty.service.PlayerConnection
import com.rapid.prominence.minty.service.queues.YouTubeQueue
import com.rapid.prominence.minty.special.SpecialAbout
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.utils.getInstallSys
import com.tencent.mmkv.MMKV
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.launch
import media.library.flys.model.loadEvents
import media.library.flys.MintyValueStys
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool
import media.library.manager.MintyApi
import media.library.manager.MintyApi.Companion.getCoreInfo
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class MintySecMainPage : MintyBaseActivity() {
    private var pendingOpenPlayer = false

    private fun openPlayerIfRequested() {
        if (pendingOpenPlayer && secPlayConnect != null) {
            pendingOpenPlayer = false
            startActivity(Intent(this, SecPlayer::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenPlayer = intent.getBooleanExtra(MusicPlayerAppWidgetProvider.EXTRA_OPEN_PLAYER, false)
        WidgetEntryUi.requestFromNotification(this, intent)
        openPlayerIfRequested()
    }
    override fun onStart() {
        super.onStart()
        startService(Intent(this@MintySecMainPage, MusicService::class.java))
        bindService(Intent(this@MintySecMainPage, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    companion object {
        var secPlayConnect by mutableStateOf<PlayerConnection?>(null)
        var canShowFlag = false
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                if (service is  MusicService.MusicBinder) {
                    secPlayConnect = PlayerConnection(this@MintySecMainPage, service, database, lifecycleScope)
                    openPlayerIfRequested()
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
            }catch (e : Exception){
                e.printStackTrace()
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            secPlayConnect?.dispose()
            secPlayConnect = null
        }
    }

    override fun handleBackPressed() {
        if (SpeMintyTools.homeStudy.uniqueInter != null && !SpeMintyTools.homeStudy.backLimit()){
            SpeMintyTools.homeStudy.showCachedInter(this)
            return
        }
        if (SpeMintyTools.homeStudy.intellBanners != null ){
            val natived = SpeMintyTools.homeStudy.intellBanners
            SpeMintyTools.homeStudy.intellBanners = null
            showCachedBan(natived!!)
            messages?.removeCallbacksAndMessages(null)
            return
        }
        WidgetEntryUi.showExitRetain(this) { moveTaskToBack(false) }
    }

    private fun showCachedBan(intellBanners: NativeAd) {
        try {
            if (isFinishing || isDestroyed){
                return
            }
            isSkipLoad = true
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(intellBanners)
            }
        }catch (e : Exception){
            e.printStackTrace()
        }

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
        // Home-top notification/widget tips are intentionally disabled for now.
        requestNotificationPermissionAtHome()
        try {
            if (secPlayConnect != null && secPlayConnect?.player?.currentMetadata != null){
                secPlayConnect?.mediaMetadata?.let {
                    bottomSettingChange(it.value)
                }
            }
            if (!MMKV.defaultMMKV().getBoolean("showSuc",false)
                && (System.currentTimeMillis() - getInstallSys() > 12 * 60 * 60 * 1000)  && canShowFlag
            ) {
                isShowStar()
            }
            getCoreInfo(this,true)
            getNative()
            MintyApi.pageBEventsUpload()
        }catch (e : Exception){
            e.printStackTrace()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        isLoaded = false
        if (EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this)
        }
        EventBus.getDefault().removeAllStickyEvents()
        messages?.removeCallbacksAndMessages(null)
        if (dataStore.get(StopMusicOnTaskClearKey,false)&& secPlayConnect?.isPlaying?.value == true && isFinishing){
            stopService(Intent(this,MusicService::class.java))
            serviceConnection?.let {
                unbindService(it)
            }
            secPlayConnect = null
        }
    }
    private var isLoaded = false
    override fun onPause() {
        super.onPause()
        if (messages?.hasMessages(MESSAGE_MSG_CODE) == true){
            messages?.removeMessages(MESSAGE_MSG_CODE)
        }
    }
    private fun bottomSettingChange(metadata: MediaMetadata?) {
        addUIPlayer(binding.mintyIncContainerPlay.artist,binding.mintyIncContainerPlay.title,binding.mintyIncContainerPlay.thumbnail,metadata)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpenPlayer = intent.getBooleanExtra(MusicPlayerAppWidgetProvider.EXTRA_OPEN_PLAYER, false)
        database =  InternalDatabase.newInstance(this)
        val homeViewModel = HomeViewModel(this, database)
        binding = MintySecMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WidgetEntryUi.requestFromNotification(this, intent)
        MMKV.defaultMMKV().putString(MINTY_APP_CURRENT_PAGE,"minty")
        binding.specAnm.post {
            binding.specAnm.visibility = View.VISIBLE
            binding.specAnm.startShimmerAnimation()
        }
        binding.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?) {
                state ?: return
                when (state) {
                    State.COLLAPSED -> {

                        binding.searchEditText.visibility = View.VISIBLE
                        binding.a.visibility = View.GONE
                        binding.searchEditText2.visibility = View.GONE

                    }
                    State.EXPANDED -> {

                        binding.searchEditText.visibility = View.GONE
                        binding.a.visibility = View.VISIBLE
                        binding.searchEditText2.visibility = View.VISIBLE

                    }

                    State.IDLE -> {}

                }
            }


        })

        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this)
        }

        secMainLayoutAdapter = SecMainLayoutAdapter(this)
        secReleaseAlbumAdapter = SecReleaseAlbumAdapter(this@MintySecMainPage)
        homeStyleAdapter = HomeStyleAdapter(this)

        // Set up RecyclerViews
        binding.homePageSectionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.homePageSectionsRecyclerView.adapter = secMainLayoutAdapter

        binding.newReleaseAlbumsRecyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false)
        binding.newReleaseAlbumsRecyclerView.adapter = secReleaseAlbumAdapter

        binding.listRecycleview.layoutManager = GridLayoutManager(this, 2)

        binding.listRecycleview?.addItemDecoration(SecGridItemDecoration(SizeUtils.dp2px( 10f),2))
        binding.listRecycleview.adapter = homeStyleAdapter

        binding.searchEditText.setOnClickListener {
            startSearchPage()
            firebaseLog("home_to_search")
        }
        binding.searchEditText2.setOnClickListener {
            startSearchPage()
            firebaseLog("home_to_search")
        }
        binding.listButton.setOnClickListener {
            startQueuePage()
            firebaseLog("home_to_queue")

        }
        binding.history.setOnClickListener {
            firebaseLog("home_to_history")

            startInterActivity(Intent(this, HistoryView::class.java), historyInter(),false)
        }
        binding.fav.setOnClickListener {
            firebaseLog("home_to_collect")

            startInterActivity(Intent(this, MintySecFavPage::class.java), collectInter(),false)

        }
        binding.mintyIncContainerPlay.container.setOnClickListener {
            firebaseLog("home_to_player")
            startInterActivity(Intent(this, SecPlayer::class.java), playViewsInter(),false)
        }

        binding.emoScan.setOnClickListener {
            firebaseLog("home_refresh_btn")
            binding.specAnm.post {
                binding.specAnm.visibility = View.VISIBLE
                binding.specAnm.startShimmerAnimation()
            }
            homeViewModel?.refresh()
        }
        binding.newReleaseTitle.setOnClickListener {
            firebaseLog("home_to_newRelease")

            startInterActivity(Intent(this, MintyReDetailPage::class.java), releaseDetailInter(),false)
        }


        binding.aboutBtn.setOnClickListener {
            firebaseLog("home_to_about")

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
        lifecycleScope.launch{
            try {
                homeViewModel?.similarRecommendations?.collect{
                    if (isFinishing || isDestroyed){
                        return@collect
                    }

                    Timber.e(it.toString())
                    if (it.isNullOrEmpty()) {
                        return@collect
                    }
                    binding.specAnm.visibility = View.GONE
                    binding.specAnm.stopShimmerAnimation()
                    if (binding.emptyLayout.isVisible){
                        binding.emptyLayout.visibility = View.GONE
                    }
                    binding.guess.visibility = View.VISIBLE
//                binding.guess.text = it?.firstOrNull()?.title?.title ?: "Guess you like it"
                    binding.similarList.apply {
                        visibility = View.VISIBLE
                        layoutManager = LinearLayoutManager(this@MintySecMainPage,LinearLayoutManager.HORIZONTAL,false)
                        val detAdapter = SecHomeDetailAdapter(
                            onItemClickListener = { item ->
                                when (item) {
                                    is SongItem -> {
//                            Log.e("-------type","song")
                                        secPlayConnect?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                        if (isFinishing || isDestroyed){
                                            return@SecHomeDetailAdapter
                                        }
                                        firebaseLog("home_to_player")
                                        startInterActivity(Intent(this@MintySecMainPage, SecPlayer::class.java), playViewsInter(),false)
//                            playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                    }
//                            playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                    is AlbumItem ->{
//                            Log.e("-------type","album")
                                        if (isFinishing ||  isDestroyed ){
                                            return@SecHomeDetailAdapter
                                        }
                                        firebaseLog("home_to_album_det")

                                        startInterActivity(Intent(this@MintySecMainPage, MintySecAlbumDetailPage::class.java).apply {
                                            putExtra("albumId",item.id)
                                        }, albumDetInter(),false)
                                    }
//                            navController.navigate("album/${item.id}")
                                    is ArtistItem -> {
//                            Log.e("-------type","artist")
                                        if (isFinishing ||  isDestroyed ){
                                            return@SecHomeDetailAdapter
                                        }
                                        firebaseLog("home_to_artist_det")

                                        startInterActivity(Intent(this@MintySecMainPage, MintySecArtistDetailPage::class.java).apply {
                                            putExtra("artistId",item.id)
                                        }, artistDetInter(),false)

                                    }
//                            navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> {
//                            Log.e("-------type","PlaylistItem")

                                        if (isFinishing ||  isDestroyed ){
                                            return@SecHomeDetailAdapter
                                        }
                                        firebaseLog("home_to_playlist_det")

                                        startInterActivity(Intent(this@MintySecMainPage, MintySecPyDetailView::class.java).apply {
                                            putExtra("playlistId",item.id)
                                        }, playlistDetInter(),false)

                                    }
//                            navController.navigate("online_playlist/${item.id}")
                                }
                            },
                        )
                        adapter = detAdapter
                        detAdapter.setList(it.first().items)

                    }
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
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



    private val messages: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                MESSAGE_MSG_CODE -> {
                    getNative()
                }
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
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun loadHomeData(reModel: loadEvents?) {
        LogUtils.e("BannerEvs------")
        if (reModel == null) {
            return
        }
        if (isFinishing || isDestroyed) {
            return
        }
        if (reModel.bannerType && !isLoaded) {
            getNative()
        }
    }
    private fun getNative() {
        val mainAds: NativeAd? = SpeMintyTools.homeStudy.showWeaveNatives(
            homeBanner(),this)
        if (mainAds == null) {
            if (EventsAppendTool.cyIsShowingFLag) {
                LogUtils.e("main banner inter is showing return------")
                return
            }
            if (isFinishing || isDestroyed) {
                LogUtils.e("main banner destroy  return------")
                return
            }
            if (EventsAppendTool.posIsCanShow(homeBanner())) {
                LogUtils.e("main banner limit  return------")
                return
            }
            SpeMintyTools.homeStudy.initLoadBanners(homeBanner(),object :
                NativeWeavesListener {
                override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                    if (isFinishing || isDestroyed ) {
                        return
                    }
                    try{
                        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            LogUtils.e("home not resumed return------")
                            return
                        }
                    }catch (e : Exception){
                        e.printStackTrace()
                    }

                    if (sucFlag) {
                        LogUtils.e("main banner load  suc------")
                        getNative()
                    }
                }
            })
        } else {
            isLoaded = true
            LogUtils.e("main banner show  suc------")
            binding?.mobBans?.let {
                it.visibility = View.VISIBLE
                it.setStyles(MintyValueStys.Builder().build())
                it.setNativeAd(mainAds)
                if(!EventsAppendTool.posIsCanShow(homeBanner())){
                    val cts = MMKV.defaultMMKV().getInt(MINTY_APP_APP_REFRESH, 0)
                    if (cts != 0){
                        messages.removeMessages(MESSAGE_MSG_CODE)
                        messages.sendEmptyMessageDelayed(MESSAGE_MSG_CODE,(cts * 1000).toLong())
                    }
                }
            }
        }
    }




}
