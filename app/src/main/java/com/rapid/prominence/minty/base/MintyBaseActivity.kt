package com.rapid.prominence.minty.base

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ThreadUtils
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.bean.MediaMetadata
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.cons.searchInter
import com.rapid.prominence.minty.posBitmap
import com.rapid.prominence.minty.roundOptions
import com.rapid.prominence.minty.second.view.MintySecMainPage.Companion.canShowFlag
import com.rapid.prominence.minty.second.SecPlayer
import com.rapid.prominence.minty.second.SecPlayDlView
import com.rapid.prominence.minty.second.search.MusicSearchHistoryActivity

open class MintyBaseActivity : AppCompatActivity() {

    fun startInterActivity(intent: Intent, pos : Long,isFinish : Boolean = true){
        canShowFlag = true
            startActivity(intent)
            if (isFinish){
                finish()
            }
    }
    @JvmField
    var loadingStart: AlertDialog? = null

    override fun onDestroy() {
        super.onDestroy()
        if (null != loadingStart && loadingStart!!.isShowing && !isFinishing && !isDestroyed) {
            loadingStart!!.dismiss()
        }
    }
    open fun cancelLoadingBar() {
        ThreadUtils.runOnUiThread {
            try {
                if (null != loadingStart && loadingStart!!.isShowing && !isFinishing && !isDestroyed) {
                    loadingStart!!.dismiss()
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
        else if (localClassName.contains("EiPlayer") ||  localClassName.contains("MainPlayer")){
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
            //todo
            finish()
        }



}
