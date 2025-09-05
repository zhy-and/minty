package media.library.flys.tools
import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.application.App.Companion.isClicked
import com.rapid.prominence.minty.application.App.Companion.isCanJumpOm
import com.rapid.prominence.minty.application.App.Companion.isSkipLoad
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.MINTY_APP_APP_AD_CACHE_TM
import com.rapid.prominence.minty.cons.MINTY_APP_APP_AD_DATA
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.cons.MINTY_APP_JK_CLICK_CT
import com.rapid.prominence.minty.cons.MINTY_APP_JK_SHOW_CT
import com.rapid.prominence.minty.cons.MINTY_APP_JK_SHOW_ST
import com.rapid.prominence.minty.cons.NativeWeavesListener
import com.rapid.prominence.minty.cons.ShowListener
import com.rapid.prominence.minty.cons.StartUpListener
import com.rapid.prominence.minty.cons.TemTranslateIntersLis
import com.rapid.prominence.minty.cons.albumDetBanner
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetBanner
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.collectBanner
import com.rapid.prominence.minty.cons.finishPageInterFor
import com.rapid.prominence.minty.cons.finishPageInterOne
import com.rapid.prominence.minty.cons.finishPageInterThr
import com.rapid.prominence.minty.cons.finishPageInterTwo
import com.rapid.prominence.minty.cons.homeBanner
import com.rapid.prominence.minty.cons.musColdStart
import com.rapid.prominence.minty.cons.musHotStart
import com.rapid.prominence.minty.cons.playViewBanner
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.queueListBanner
import com.rapid.prominence.minty.cons.searchInter
import com.rapid.prominence.minty.cons.searchRecordInter
import com.rapid.prominence.minty.cons.searchResultBanner
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.second.view.MintySecMainPage

import com.tencent.mmkv.MMKV
import media.library.flys.model.DismissEvents
import media.library.flys.GoogleMobileAdsConsentManager
import media.library.flys.tools.MintyStuTools.valueUpload
import media.library.flys.tools.EventsAppendTool.cyIsShowingFLag
import media.library.flys.model.MintyCoreInfo
import media.library.flys.model.loadEvents
import org.greenrobot.eventbus.EventBus

class SpeMintyTools {
    private fun nativeWeavesIsLoading(status: Boolean): Boolean {
        if (status){
            if (nativeWeavesStatus){
                LogUtils.e("result ad is loading")
                return true
            }
        }else{
            if (musWeavesNaIsLoading){
                LogUtils.e( "banner ad is loading")
                return true
            }
        }
        return false
    }


    private fun notifyNative(vos: Boolean, status1 : Boolean) {
        if (vos &&  nativeWeavesStatus){
            nativeWeavesStatus = status1
        }else if (!vos &&  musWeavesNaIsLoading){
            musWeavesNaIsLoading = status1
        }
    }


    private var dataBean: MutableList<MintyCoreInfo.GxjovnBean.HzaxklBean.WbncvqBean.QsvsBean>? =
        null
    var currentPos: Long = -1

    private var tempStatus: Boolean = false



    private fun checkNativeP(adPos: Long): Boolean {
        return false
    }


    private fun isUmeng() : Boolean{
        return  App.isCanJumpOm || GoogleMobileAdsConsentManager.getInstance(
            App.instance
        ).canRequestAds()

    }

    fun createInter(
        interPos: Long,
        tperklyTranslateIntersLis: TemTranslateIntersLis? = null
    ) {
        try {
            var adId = ""
            // 缓存拦截 插页成功回调
            if (interPoolsIsLoading()) {
                LogUtils.e(
                    "inter is loading return=====" + EventsAppendTool.basicInfoLog(
                        interPos
                    ) + ""
                )
                return
            }
            if (checkInterExists()) {
                LogUtils.e(
                    "inter cached has ads return=====" + EventsAppendTool.basicInfoLog(
                        interPos
                    ) + ""
                )
                tperklyTranslateIntersLis?.interIsWeavesSuc(true)
                return
            }
            interStatus = true
            interWeavesId?.run { adId = this }
            if (TextUtils.isEmpty(adId)) {
                LogUtils.e("inter id empty=====" + EventsAppendTool.basicInfoLog(interPos) + "")
                notifyCySts(false)
                return
            }

            val adRequest = AdRequest.Builder().build()
            val context = App.instance ?: return
            InterstitialAd.load(context, adId, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        LogUtils.e(
                            "inter ad load suc =====" + EventsAppendTool.basicInfoLog(
                                interPos
                            ) + ""
                        )

                        uniqueInter = interstitialAd
                        interWeavesTimes = System.currentTimeMillis()
                        notifyCySts(false)
                        tperklyTranslateIntersLis?.interIsWeavesSuc(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        LogUtils.e(
                            "inter ad load error =====" + EventsAppendTool.basicInfoLog(
                                interPos
                            ) + ""
                        )
                        notifyCySts(false)
                        tperklyTranslateIntersLis?.interIsWeavesSuc(false)
                    }
                })
        } catch (e: Exception) {
            notifyCySts(false)
            e.printStackTrace()
        }
    }





    fun showWeaveNatives(nativePos: Long, context: Activity): NativeAd? {
        val nativeAd: NativeAd?
        if (context.isDestroyed || context.isFinishing) return null
        if (EventsAppendTool.posIsCanShow(nativePos)){
            LogUtils.e("native  limit ------" + EventsAppendTool.basicInfoLog(nativePos))
            return null
        }
        if (cyIsShowingFLag){
            return null
        }
        currentPos = nativePos
        if (intellNatives != null){
            if (checkMusWeavesNaTimes(nativeWeavesTimes)){
                nativeWeavesTimes  = 0
                LogUtils.e( "curr pos${EventsAppendTool.basicInfoLog(nativePos)}more than sixty mins ,result cache pools")
                intellNatives = null
                return null
            }
            tempStatus = true
            nativeAd = intellNatives
            intellNatives = null
            return nativeAd
        }else if(intellBanners != null){
            if (checkMusWeavesNaTimes(banWeavesTimes)){
                banWeavesTimes  = 0
                LogUtils.e( "curr pos${EventsAppendTool.basicInfoLog(nativePos)}more than sixty mins,banner cache pools")
                intellBanners = null
                return null
            }
            tempStatus = false
            nativeAd = intellBanners
            intellBanners = null
            return nativeAd
        }
        return null
    }

    private var nativeWeavesTimes: Long = 0

    private var nativeWeavesStatus: Boolean = false

    private var banWeavesIds: String? = null

    private var interWeavesId: String? = null


    private fun musWeavesTimesToLoadConfig() {
        musWeavesLoadNatives()
//        if (CommonStudyUtils.interIsCanLoad()) {
//            createInter(trackListInter())
//        }
    }
    private fun notifyCySts(sts: Boolean) {
        interStatus = sts
    }

    private var interStatus: Boolean = false

    var uniqueInter: InterstitialAd? = null

    private fun mintyConfigicNatCheckExists(transSt: Boolean): Boolean {
        if (transSt && intellNatives != null && !checkMusWeavesNaTimes(nativeWeavesTimes)){
            LogUtils.e( "result cache pools has cache,stop cac")
            return true
        }else if(!transSt && intellBanners != null && !checkMusWeavesNaTimes(banWeavesTimes)){
            LogUtils.e( "banner cache pools has cache,stop cac")
            return true
        }
        return false
    }



    private var musKpIds: String? = null

     var intellBanners: NativeAd? = null

    private var intellNatives: NativeAd? = null

    private var musWeavesNaIsLoading: Boolean = false





    fun initStartAds(baseTrans: MintyBaseActivity?) {
        if (EventsAppendTool.intellIsCanLoadStart()) { //kp can load
            KepingValueTools.intellTools.configStartIds(musKpIds)
            ThreadUtils.runOnUiThread {
                if (KepingValueTools.intellTools.intellObj == null) { // null -> load
                    KepingValueTools.intellTools.loadStartAds(baseTrans, object :
                        StartUpListener {
                        override fun loadSuc() {
//                            initLoadInter(baseTrans)
                            if (baseTrans == null || baseTrans.isDestroyed || baseTrans.isFinishing) {
                                return
                            }
                            showStartWeaves(baseTrans)
                            //to load inter
                        }

                        override fun LoadedError() {
//                            initLoadInter(baseTrans)
                            //to load inter

                        }
                    })
                } else {
//                    initLoadInter(baseTrans)
                    // has ad -> show
                    showStartWeaves(baseTrans)
                    //to load inter
                }
            }
        }
    }

    private var banWeavesTimes: Long = 0

    private var interWeavesTimes: Long = 0





    companion object {
        val homeStudy: SpeMintyTools by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SpeMintyTools()
        }
    }


    fun musWeavesLoadNatives() {
        if (!EventsAppendTool.posIsCanShow(homeBanner())) {
            initLoadBanners(
                homeBanner(),
                object : NativeWeavesListener {
                    override fun nativeIsWeavesSuc(sucFlag: Boolean) {
                        if (sucFlag) {
                            EventBus.getDefault().postSticky(
                                loadEvents(
                                    true
                                )
                            )
                        }
                    }

                })
        }
    }

    private var musNativesId: String? = null

    private fun checkMusWeavesNaTimes(nativeHt : Long): Boolean {
        if (System.currentTimeMillis() - nativeHt   > 60 * 60 * 1000) {
            return true
        }
        return false
    }

    private fun checkInterExists(): Boolean {
        //
        if (uniqueInter != null && !EventsAppendTool.checkTimes(interWeavesTimes)) {
            return true
        }
        return false
    }


    private fun loadInterWeaves(
        interstitialAd: InterstitialAd?,
        pos: Long,
    ) {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            var currentClicked = false
            override fun onAdClicked() {
                try {
                    if (!currentClicked) {
                        currentClicked = true
                        EventsAppendTool.appendClick(pos)
                    }
                    isClicked = true
//                    if (playerConnection?.isPlaying?.value == true){
//                        ThreadUtils.runOnUiThreadDelayed({ ActivityUtils.finishAllActivities()},500)
//                    }
                }catch (e : Exception){
                    e.printStackTrace()
                }

            }

            override fun onAdDismissedFullScreenContent() {
                LogUtils.e("onAdDismissedFullScreenContent")
                cyIsShowingFLag = false
            }

            override fun onAdFailedToShowFullScreenContent(fa: AdError) {
                // Called when ad fails to show.
                cyIsShowingFLag = false
                LogUtils.e("onAdFailedToShowFullScreenContent")

            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
//                cyIsShowingFLag = true
                LogUtils.e("onAdShowedFullScreenContent")

                EventsAppendTool.showCountAdd(pos)
//                ThreadUtils.runOnUiThreadDelayed({
//                    LogUtils.e(CommonStudyUtils.basicInfoLog(pos) + "load a new cy ad")
//                    createInter(pos)
//                }, 1500)
            }
        }
        interstitialAd?.setOnPaidEventListener {
            valueUpload(it,"Interstitial", interstitialAd.adUnitId,pos)
        }

    }







    fun initMusConfig(
        activity: MintyBaseActivity?, load: Boolean = false
    ) {
        try {
            if (TextUtils.isEmpty(
                    MMKV.defaultMMKV().getString(MINTY_APP_APP_AD_DATA, "")
                )
            ) {
                MMKV.defaultMMKV().putLong(MINTY_APP_APP_AD_CACHE_TM, 0)
                return
            }

            val mintyCoreInfo = Gson().fromJson(
                MMKV.defaultMMKV().getString(MINTY_APP_APP_AD_DATA, ""),
                MintyCoreInfo::class.java
            ) ?: return

            if (mintyCoreInfo?.gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.qsvs != null) {
                dataBean = mintyCoreInfo?.gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.qsvs
            }
            if (dataBean.isNullOrEmpty()) {
                return
            }
           dataBean?.forEach {
               mmkvInit(it)
               when (it.sesmhh) {
                   musColdStart(), musHotStart()
                   -> {
                       if (!TextUtils.isEmpty(it.qwaqzg)) {
                           musKpIds = it.qwaqzg
                       }
                   }

                   homeBanner(), searchResultBanner(), albumDetBanner(),
                   queueListBanner(),  artistDetBanner(), playViewBanner(), collectBanner()

                   -> {
                       if (!TextUtils.isEmpty(it.qwaqzg)) {
                            banWeavesIds = it.qwaqzg

                       }
                   }

                   searchInter(),  searchRecordInter(), albumDetInter(),artistDetInter(),
                    playViewsInter(), finishPageInterOne(), finishPageInterTwo()
                   -> {
                       if (!TextUtils.isEmpty(it.qwaqzg)) {

                            interWeavesId = it.qwaqzg

                       }

                   }
               }
           }

            if (!load && (isCanJumpOm || GoogleMobileAdsConsentManager.getInstance(
                    App.instance
                ).canRequestAds())) {
                LogUtils.e("load um  true ----  ")
                initStartAds(activity)
            }

            if (isHomePage(load)) return
            if (!isUmeng()){
                LogUtils.e("skip  ----  ")
                return
            }
            if (!EventsAppendTool.intellIsCanLoadStart()){
//                initLoadInter(activity) // can't load kp to load inter
            }
//            musWeavesLoadNatives()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initLoadBanners(
        showAdPosition: Long,
        resultAdLoadBack: NativeWeavesListener? = null
    ) {
        val mintyConfigicNatPools = checkNativeP(showAdPosition)
        try {
            var postion = showAdPosition
            var unitVoxyId = ""
            if (nativeWeavesIsLoading(mintyConfigicNatPools) ){
                return
            }
            if (mintyConfigicNatCheckExists(mintyConfigicNatPools)){
                resultAdLoadBack?.nativeIsWeavesSuc(true)
                return
            }
            if (mintyConfigicNatPools){
                nativeWeavesStatus = true
                musNativesId?.let { unitVoxyId = it }
            }else{
                musWeavesNaIsLoading = true
                banWeavesIds?.let { unitVoxyId = it }
            }
            if (TextUtils.isEmpty(unitVoxyId)){
                notifyNative(mintyConfigicNatPools, false)
                LogUtils.e("native id error------"+ EventsAppendTool.basicInfoLog(postion))
                return
            }
            val context = App.instance ?: return
            val loader = AdLoader.Builder(context, unitVoxyId)
                .forNativeAd { nativeAd ->
                    nativeAd.setOnPaidEventListener {
                        valueUpload(it,"Native",unitVoxyId,postion)
                    }
                    if (mintyConfigicNatPools) {
                        LogUtils.e( "result ad load suc${unitVoxyId}curr pos is ${
                            EventsAppendTool.basicInfoLog(
                                postion
                            )
                        }")
                        intellNatives = nativeAd
                        nativeWeavesTimes = System.currentTimeMillis()
                    } else {
                        LogUtils.e( "ban ad load suc${unitVoxyId}curr pos is ${
                            EventsAppendTool.basicInfoLog(
                                postion
                            )
                        }")
                        intellBanners = nativeAd
                        banWeavesTimes = System.currentTimeMillis()
                    }
                    notifyNative(mintyConfigicNatPools, false)
                    resultAdLoadBack?.nativeIsWeavesSuc(true)
                }
                .withAdListener(object : AdListener() {
                    var clicked = false
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        LogUtils.e(
                            "native  onAdFailedToLoad ------" + EventsAppendTool.basicInfoLog(
                                postion
                            )
                        )
                        notifyNative(mintyConfigicNatPools,false)
                        resultAdLoadBack?.nativeIsWeavesSuc(false)
                    }

                    override fun onAdClicked() {
                        //前一条广告失败时position处理
                        try {

                            if (currentPos.toInt() != -1) {
                                postion = currentPos
                            }
                            if (!clicked) {
                                EventsAppendTool.appendClick(postion)
                                clicked = true
                            }
                            isClicked = true
//                            if (playerConnection?.isPlaying?.value == true){
//                                ThreadUtils.runOnUiThreadDelayed({ ActivityUtils.finishAllActivities()},500)
//                            }
                        }catch (e : Exception){
                            e.printStackTrace()
                        }

                    }

                    override fun onAdClosed() {
                    }

                    override fun onAdImpression() {
                        try {
                            if (currentPos.toInt() != -1) {
                                postion = currentPos
                            }
                            EventsAppendTool.showCountAdd(postion)
                            //返回消耗
                            if (isSkipLoad){
                                isSkipLoad = false
                                return
                            }
                            ThreadUtils.runOnUiThreadDelayed({
                                LogUtils.e(
                                    "native  load a new ad ------" + EventsAppendTool.basicInfoLog(
                                        postion
                                    )
                                )
                                initLoadBanners(postion)
                            }, 1500)
                        }catch (e : Exception){
                            e.printStackTrace()
                        }

                    }

                    override fun onAdOpened() {
                    }
                })
                .build()

            loader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            notifyNative(mintyConfigicNatPools,false)
            e.printStackTrace()
        }
    }



    private fun showStartWeaves(activity: MintyBaseActivity?) {
        KepingValueTools.intellTools.showStartAds(activity,
            object : ShowListener {
                override fun dispathSuc() {
                    if (activity == null || activity.isFinishing || activity.isDestroyed) {
                        LogUtils.e("return --------")
                        return
                    }
                    musWeavesLoadNatives()
                    if (MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER,false)){
                        activity.startActivity(Intent(activity, MintySecMainPage::class.java))

                    }else{
                        activity.startActivity(Intent(activity, EiMainPage::class.java))
                    }
                    activity.finish()
                }

                override fun dispathError() {
                    if (activity == null || activity.isFinishing || activity.isDestroyed) {
                        return
                    }
                    EventBus.getDefault().post(
                        DismissEvents(
                            true
                        )
                    )
                }
            })
    }

    private fun isHomePage(
        jump: Boolean
    ): Boolean {
        if (jump) {
            ThreadUtils.runOnUiThread {
                musWeavesTimesToLoadConfig()
            }
            return true
        }
        return false
    }





    fun getShowCyAds(recAds: Long, context: Activity) {
        if (EventsAppendTool.posIsCanShow(recAds)) {
            LogUtils.e("inter flag limit ------" + EventsAppendTool.basicInfoLog(recAds))
            return
        }
        if (uniqueInter != null) {
            if (EventsAppendTool.checkTimes(interWeavesTimes)) {
                LogUtils.e("inter flag limit ------" + EventsAppendTool.basicInfoLog(recAds))
                uniqueInter = null
                interWeavesTimes = 0
                return
            }
//            cyIsShowingFLag = true
            uniqueInter?.show(context)
            LogUtils.e("inter showing current pos ===== " + EventsAppendTool.basicInfoLog(recAds))
            loadInterWeaves(uniqueInter, recAds)
            uniqueInter = null
        } else {
            LogUtils.e("inter cache no to load new  ===== " + EventsAppendTool.basicInfoLog(recAds))
            createInter(recAds, object : TemTranslateIntersLis {
                override fun interIsWeavesSuc(inSucFlag: Boolean) {
                    if (inSucFlag) {
                        LogUtils.e(
                            "inter cache no to load new suc  ===== " + EventsAppendTool.basicInfoLog(
                                recAds
                            )
                        )
                    } else {
                        LogUtils.e(
                            "inter cache no to load new  error  ===== " + EventsAppendTool.basicInfoLog(
                                recAds
                            )
                        )
                    }
                }

            })
        }

    }



    fun showCachedInter(context: Activity) {
        if (uniqueInter != null) {
            uniqueInter?.show(context)
            LogUtils.e("inter showing cache current pos ===== " )
            uniqueInter = null
        }
    }

     fun backLimit(): Boolean {
        if (EventsAppendTool.posIsCanShow(finishPageInterOne()) && EventsAppendTool.posIsCanShow(
                finishPageInterTwo()
            ) && EventsAppendTool.posIsCanShow(finishPageInterThr()) && EventsAppendTool.posIsCanShow(
                finishPageInterFor()
            )
        ) {
            LogUtils.e("inter cache flag limit ------")
            return true
        }
        return false
    }



    private fun interPoolsIsLoading(): Boolean {
        if (interStatus) {
            return true
        }
        return false
    }


    private fun mmkvInit(qsvsBean: MintyCoreInfo.GxjovnBean.HzaxklBean.WbncvqBean.QsvsBean) {
        MMKV.mmkvWithID("mmkv_music_app").putBoolean("$MINTY_APP_JK_SHOW_ST-${qsvsBean.sesmhh}",
            "0" == qsvsBean.zkzvc
        )
        MMKV.mmkvWithID("mmkv_music_app").putInt("$MINTY_APP_JK_CLICK_CT-${qsvsBean.sesmhh}", qsvsBean.vsg)


        MMKV.mmkvWithID("mmkv_music_app").putInt("$MINTY_APP_JK_SHOW_CT-${qsvsBean.sesmhh}", qsvsBean.hzyt)
    }





}