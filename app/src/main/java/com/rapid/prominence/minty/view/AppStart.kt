package com.rapid.prominence.minty.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.blankj.utilcode.util.LogUtils
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.application.App.Companion.isCanJumpOm
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.cons.MMKV_LAUCH_TIME
import com.rapid.prominence.minty.cons.checkRDP
import com.rapid.prominence.minty.databinding.ActivityMainBinding
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.tencent.mmkv.MMKV
import media.library.flys.GoogleMobileAdsConsentManager
import media.library.flys.model.DismissEvents
import media.library.flys.tools.SpeMintyTools
import media.library.manager.MintyApi.Companion.getCoreInfo
import media.library.manager.MintyApi.Companion.lauchEventsUpload
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.atomic.AtomicBoolean


class AppStart : MintyBaseActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
            if (MMKV.defaultMMKV().getLong("lauch_f_mmkv",0L) == 0L){
                MMKV.defaultMMKV().putLong("lauch_f_mmkv",System.currentTimeMillis())
            }
            if (!checkRDP()){
                isCanJumpOm = true
            }

            startInit()
            if (checkRDP()){
                createView()
            }else{
                toHomePage()
            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    private val adsInitializeCalled = AtomicBoolean(false)

    private fun createView() {

        val googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(
            App.instance
        )
        googleMobileAdsConsentManager?.gatherConsent(
            this
        ) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
            }
            isCanJumpOm = true
            initializeMobileAdsSdk()
        }
        if (googleMobileAdsConsentManager?.canRequestAds() == true) {

            initializeMobileAdsSdk()
        }
    }


    private var messManager : Handler?= null


    private fun toHomePage() {
        val lauchTime =  MMKV.defaultMMKV().getInt(MMKV_LAUCH_TIME,15)
        messManager?.postDelayed({
            if (MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER,false)){
                startActivity(Intent(this, MintySecMainPage::class.java))
            }else{
                startActivity(Intent(this, EiMainPage::class.java))
            }
            finish()
        }, lauchTime * 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        messManager?.removeCallbacksAndMessages(null)
        binding?.anims?.cancelAnimation()
    }
    private fun startInit() {
        try {
            messManager =  Handler(Looper.getMainLooper())

            getCoreInfo(this,false)
            lauchEventsUpload()
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    private fun destroyHd() {
        try {
            messManager?.removeCallbacksAndMessages(null)
            messManager = null
        }catch (e : Exception){
            e.printStackTrace()
        }

    }



    private fun initializeMobileAdsSdk() {
        if (adsInitializeCalled.getAndSet(true)) {
            return
        }
        if (messManager == null){
            LogUtils.e("jumped   err---- ")
            return
        }
        // Initialize the Mobile Ads SDK.
        SpeMintyTools.homeStudy.initStartAds(this)
        toHomePage()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun remove(re: DismissEvents?) {
        LogUtils.e(" CancelEvent------")
        if (re?.isShow == true) {
            destroyHd()
        }
    }


}