package com.rapid.prominence.minty.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.finishPageInterFor
import com.rapid.prominence.minty.cons.finishPageInterOne
import com.rapid.prominence.minty.cons.finishPageInterThr
import com.rapid.prominence.minty.cons.finishPageInterTwo
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.cons.searchInter
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.roundOptions
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.canShowFlag
import com.rapid.prominence.minty.second.SecPlayer
import com.rapid.prominence.minty.second.SecPlayDlView
import com.rapid.prominence.minty.second.search.MusicSearchHistoryActivity
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool

open class MintyBaseActivity : AppCompatActivity() {



        // 检查并请求通知权限(Android 13+)
        fun checkAndRequestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        10001
                    )
                }
            }
        }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            10001 -> {
                if ( grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }else{

                }

            }
        }
    }


    fun startInterActivity(intent: Intent, pos : Long,isFinish : Boolean = true){
        canShowFlag = true
        if (!EventsAppendTool.posIsCanShow(pos) && SpeMintyTools.homeStudy.uniqueInter == null){
            SpeMintyTools.homeStudy.createInter(pos)
            showLottie()
            ThreadUtils.runOnUiThreadDelayed({
                cancelLoadingBar()
                startActivity(intent)
                if (isFinish){
                    finish()
                }
            },3500)
        }else{
            startActivity(intent)
            if (isFinish){
                finish()
            }
        }
    }
    @JvmField
    var lottieDialog: AlertDialog? = null
    open fun showLottie() {
        ThreadUtils.runOnUiThread {
            try {
                if (lottieDialog == null){
                    lottieDialog = AlertDialog.Builder(this).create()
                    lottieDialog!!.window!!.setBackgroundDrawable(ColorDrawable())
                    lottieDialog!!.setCancelable(false)
                    lottieDialog!!.setOnKeyListener { _, keyCode, event -> keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK }
                }
                lottieDialog?.apply {
                    setOnDismissListener {
                        findViewById<LottieAnimationView>(R.id.progress)?.cancelAnimation()
                    }
                }
                if (null != lottieDialog && !lottieDialog!!.isShowing && !isFinishing && !isDestroyed) {
                    lottieDialog!!.show()
                    lottieDialog!!.setContentView(R.layout.loading)
                    lottieDialog!!.setCanceledOnTouchOutside(false)
                }

            }catch (e : Exception){
                e.printStackTrace()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != lottieDialog && lottieDialog!!.isShowing && !isFinishing && !isDestroyed) {
            lottieDialog!!.dismiss()
        }
    }
    open fun cancelLoadingBar() {
        ThreadUtils.runOnUiThread {
            try {
                if (null != lottieDialog && lottieDialog!!.isShowing && !isFinishing && !isDestroyed) {
                    lottieDialog!!.dismiss()
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (localClassName.contains("LocalHomeActivity")){
            ImmersionBar.with(this).transparentStatusBar().init()
        }
        else if (localClassName.contains("SongActivity")){
            enableEdgeToEdge()
        }
        else if (localClassName.contains("AlbumDetActivity") || localClassName.contains("PlayListDetActivity") || localClassName.contains("MoodDetailsActivity") || localClassName.contains("ArtistDetActivity") || localClassName.contains("NewReleaseDetailsActivity")){
        }
        else if (localClassName.contains("EiPlayer") ||  localClassName.contains("SecPlayer")){
            ImmersionBar.with(this).navigationBarColor(R.color.new_color).init()

        }
        else if (localClassName.contains("AppStart")){
            ImmersionBar.with(this).transparentBar().init()
        }
        else if (localClassName.contains("ArtistDetActivity")){


        }
        else{
            ImmersionBar.with(this).transparentStatusBar().init()

        }
    }

    open fun addUIPlayer(
        artist: TextView,
        title: TextView,
        thumbnail: ImageView,
        metadata: MediaMetadata?
    ) {
        if (!TextUtils.isEmpty(metadata?.title)){
            title.text = metadata?.title
        }
        if (!TextUtils.isEmpty(metadata?.artists?.joinToString(", ") { it.name })){
            artist.text = metadata?.artists?.joinToString(", ") { it.name }
        }
        metadata?.thumbnailUrl?.let { thumbnailUrl ->
            Glide.with(this@MintyBaseActivity)
                .load(thumbnailUrl)
                .apply(roundOptions)
                .thumbnail(
                    Glide.with(this@MintyBaseActivity)
                        .load(posBitmap(0))
                        .apply(roundOptions)
                )
                .into(thumbnail)
        }

    }

    open  fun startSearchPage(isClose : Boolean = false){
        startInterActivity(Intent(this, MusicSearchHistoryActivity::class.java), searchInter(),isClose)

    }
    open  fun startPlayerPage(isClose : Boolean = false){
        startInterActivity(Intent(this, SecPlayer::class.java), playViewsInter(),isClose)
    }
    open  fun startQueuePage(isClose : Boolean = false){
        startInterActivity(Intent(this, SecPlayDlView::class.java), queueInter(),isClose)
    }
    open  fun finishCurrent(isClose : Boolean = false){
        if (checkPageOne()){
            LogUtils.e("------------------------f one back ")
            SpeMintyTools.homeStudy.getShowCyAds(finishPageInterOne(),this)
        }else if (checkPageTwo()){
            LogUtils.e("------------------------f two back ")
            SpeMintyTools.homeStudy.getShowCyAds(finishPageInterTwo(),this)
        }else if (checkPageThr()){
            LogUtils.e("------------------------f thr back ")
            SpeMintyTools.homeStudy.getShowCyAds(finishPageInterThr(),this)
        }else if (checkPageFor()){
            LogUtils.e("------------------------f for back ")
            SpeMintyTools.homeStudy.getShowCyAds(finishPageInterFor(),this)
        }
        finish()
    }
    private fun checkPageOne(): Boolean {
        return this.localClassName.contains("MainPlayer")
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (checkPageOne()){
                LogUtils.e("------------------------one back ")
                SpeMintyTools.homeStudy.getShowCyAds(finishPageInterOne(),this)
                finish()
                return true
            }else if(checkPageTwo()){
                LogUtils.e("------------------------two back ")
                SpeMintyTools.homeStudy.getShowCyAds(finishPageInterTwo(),this)
                finish()
                return true
            }else if(checkPageThr()){
                LogUtils.e("------------------------thr back ")
                SpeMintyTools.homeStudy.getShowCyAds(finishPageInterThr(),this)
                finish()
                return true
            }else if(checkPageFor()){
                LogUtils.e("------------------------for back ")
                SpeMintyTools.homeStudy.getShowCyAds(finishPageInterFor(),this)
                finish()
                return true
            }else{
                finish()
                return true
            }
        }
        return  super.onKeyDown(keyCode,event)
    }
    private fun checkPageTwo(): Boolean {
        return (this.localClassName.contains("MintySecMdDetailPage")
                || this.localClassName.contains("MintyReDetailPage")
                || this.localClassName.contains("MintySecAlbumDetailPage")
                || this.localClassName.contains("MintySecPyDetailView"))
    }
    private fun checkPageThr(): Boolean {
        return (this.localClassName.contains("SecPlayDlView")
                || this.localClassName.contains("MintySecArtistDetailPage")
                || this.localClassName.contains("ArtistMoreInfoView"))
    }
    private fun checkPageFor(): Boolean {
        return (this.localClassName.contains("MusicSearchHistoryActivity")
                || this.localClassName.contains("MusicSearchActivity")
                || this.localClassName.contains("MintySecFavPage")
                || this.localClassName.contains("HistoryView"))
    }


}






