package com.rapid.prominence.minty.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.Utils
import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.ads.mediation.pangle.PangleMediationAdapter
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.rapid.prominence.minty.BuildConfig
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.cons.ContentCountryKey
import com.rapid.prominence.minty.cons.ContentLanguageKey
import com.rapid.prominence.minty.cons.CountryCodeToName
import com.rapid.prominence.minty.cons.InnerTubeCookieKey
import com.rapid.prominence.minty.cons.LanguageCodeToName
import com.rapid.prominence.minty.cons.MINTY_APP_AF_CAMPAIGN
import com.rapid.prominence.minty.cons.MINTY_APP_AF_MEDIA_SOURCE
import com.rapid.prominence.minty.cons.MINTY_APP_AF_USER
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFER_INFO
import com.rapid.prominence.minty.cons.MINTY_APP_GOOGLE_REFER_SOURCE
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.cons.MaxImageCacheSizeKey
import com.rapid.prominence.minty.cons.ProxyEnabledKey
import com.rapid.prominence.minty.cons.ProxyTypeKey
import com.rapid.prominence.minty.cons.ProxyUrlKey
import com.rapid.prominence.minty.cons.SYSTEM_DEFAULT
import com.rapid.prominence.minty.cons.UseLoginForBrowse
import com.rapid.prominence.minty.cons.VisitorDataKey
import com.rapid.prominence.minty.extensions.toEnum
import com.rapid.prominence.minty.extensions.toInetSocketAddress
import com.rapid.prominence.minty.utils.dataStore
import com.rapid.prominence.minty.utils.get
import com.rapid.prominence.minty.view.AppStart
import com.rapid.prominence.minty.analytics.MintyPushAnalytics
import com.rapid.prominence.minty.notification.localpush.LocalEventPushBootstrap
import com.rapid.prominence.minty.notification.localpush.LocalPushTelemetry
import com.rapid.prominence.minty.notification.localpush.LocalPushUi
import com.rapid.prominence.minty.runtime.MintyRuntime
import com.rapid.prominence.minty.runtime.RuntimeRemoteConfig
import com.rapid.prominence.minty.runtime.UserSegmentHelper
import com.rapid.prominence.minty.widget.WidgetPinHelper
import com.tencent.mmkv.MMKV
import com.unity3d.ads.metadata.MetaData
import com.zionhuang.innertube.MintyMusicBe
import com.zionhuang.innertube.models.YouTubeLocale
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import media.library.flys.tools.EventsAppendTool.lauchFlag
import media.library.manager.MintyWorkQueue
import timber.log.Timber
import java.net.Proxy
import java.util.Arrays
import java.util.Locale
import java.util.UUID


@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
  

    private fun requestId() {
        if (TextUtils.isEmpty(MMKV.defaultMMKV().getString("ad_id",""))){
            appScope.launch {
                try {
                    val adId = withTimeoutOrNull(15_000) { // 15秒超时
                        withContext(Dispatchers.IO) {
                            AdvertisingIdClient.getAdvertisingIdInfo(applicationContext).id
                        }
                    } ?: generateFallbackId()
//                LogUtils.e("adId---"+adId)
                    MMKV.defaultMMKV().putString("ad_id",adId)
                } catch (e: Exception) {
//                LogUtils.e("AdIdFailed: ${e.message}")
                }
            }
        }

    }

    private fun generateFallbackId(): String {
        return UUID.randomUUID().toString()
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
    companion object{
        @JvmField
        var instance: Context? = null
        //todo
        var isLocal = false
        var isNextPlay = false
        var isCanJumpOm = false
        var referStatusCallBack = false
        var isClicked = false
        var isSkipLoad = false
    }
   
    private var referrerClient: InstallReferrerClient?= null
    private var oldTimes = 0L
    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes((dataStore[MaxImageCacheSizeKey] ?: 512) * 1024 * 1024L)
                .build()
        )
        .build()
    private fun initManager() {
        //todo
//        MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
//        //todo
//        var config = RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("BE7678159E1EF50B7E22B167D305D6F2")).build()
//        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this)
        MintyWorkQueue.get().initNetWork()
        initReferInfos()
        initAppFly()
    }
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )
    private fun initAppFly() {
      //todo
//         MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
        LogUtils.getConfig().isLogSwitch = BuildConfig.DEBUG
        AppsFlyerLib.getInstance().init("Qdx2CL5Loy69WyHCQn3U3", null, this)
        AppsFlyerLib.getInstance().start(this)
        AppsFlyerLib.getInstance()
            .registerConversionListener(this, object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(conversionDataMap: Map<String, Any>) {
                    try {
                        val afStatus = conversionDataMap["af_status"] as String?
                        if (!TextUtils.isEmpty(afStatus)){
                            if (afStatus == "Non-organic"){
                                MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                            }
                        }
                        val media_source = conversionDataMap["media_source"] as String?
                        if (!TextUtils.isEmpty(media_source)){
                            if (media_source?.contains("facebook",true) == true){
                                MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                            }
                            if (media_source?.contains("googleadwords_int",true) == true || media_source?.contains("google",true) == true){
                                MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                            }
                        }
                        val campaign = conversionDataMap["campaign"] as String?
                        if (!TextUtils.isEmpty(campaign)){
                            if (campaign?.contains("0006") == true){
                                MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                            }
                        }
                        MMKV.defaultMMKV().putString(MINTY_APP_AF_MEDIA_SOURCE,conversionDataMap["media_source"] as String?)
                        MMKV.defaultMMKV().putString(MINTY_APP_AF_CAMPAIGN,conversionDataMap["campaign"] as String?)
                        MMKV.defaultMMKV().putString(MINTY_APP_AF_USER,conversionDataMap["af_status"] as String?)
                    }catch (e : Exception){
                        e.printStackTrace()
                    }

                }

                override fun onConversionDataFail(errorMessage: String) {


                } // 其他回调方法省略...

                override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
                }

                override fun onAttributionFailure(p0: String?) {
                }
            })

    }

    private fun initReferInfos() {
        try {
            referrerClient = InstallReferrerClient.newBuilder(this).build()
            referrerClient?.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    referStatusCallBack = true
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            LogUtils.e("InstallReferrerClient success")
                            try {
                                referrerClient?.installReferrer?.let {
                                    val referrerUrl: String = it.installReferrer
                                    LogUtils.e("InstallReferrerClient","InstallReferrerClient success$referrerUrl")
                                    if (!TextUtils.isEmpty(referrerUrl)){
                                        MMKV.defaultMMKV().putString(MINTY_APP_APP_REFER_INFO,referrerUrl)
                                        UserSegmentHelper.onReferInfoUpdated(this@App)
                                        if (referrerUrl.contains("facebook")){
                                            MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                                            MMKV.defaultMMKV().putString(MINTY_APP_GOOGLE_REFER_SOURCE,"facebook")
                                        }else if (referrerUrl.contains("instagram")){
                                            MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                                            MMKV.defaultMMKV().putString(MINTY_APP_GOOGLE_REFER_SOURCE,"instagram")
                                        }
                                        else if (referrerUrl.contains("gclid")){
                                            MMKV.defaultMMKV().putBoolean(MINTY_APP_IS_M_USER,true)
                                            MMKV.defaultMMKV().putString(MINTY_APP_GOOGLE_REFER_SOURCE,"gclid")
                                        }
                                        else{
                                            MMKV.defaultMMKV().putString(MINTY_APP_GOOGLE_REFER_SOURCE,"other")
                                        }
                                        referrerClient?.endConnection()
                                    }
                                }

                            }catch (e : Exception){
                                referrerClient?.endConnection()
                                e.printStackTrace()
                            }

                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            // API not available on the current Play Store app.
                            referrerClient?.endConnection()
                        }
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            // Connection couldn't be established.
                            referrerClient?.endConnection()
                        }
                    }
                    UserSegmentHelper.notifyReferFlowFinished(this@App)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })

            requestId()


            AppUtils.registerAppStatusChangedListener(object : Utils.OnAppStatusChangedListener{
                var count = 0
                override fun onForeground(activity: Activity?) {
                    MintyRuntime.onAppForeground(this@App)
                    LocalEventPushBootstrap.refreshOnForeground(this@App)
                }

                override fun onBackground(activity: Activity?) {
                    lauchFlag = false
                    try {
                        var times = System.currentTimeMillis()
                        count++
                        if (count> 50){
                            return
                        }
                        if (times - oldTimes < 2000){
                            LogUtils.e("empty ----------")
                            return
                        }
                        oldTimes = times
                        LogUtils.e("enter ht ----------")
                        ThreadUtils.runOnUiThreadDelayed({
                            ActivityUtils.finishAllActivities()
                        },300)
                        // LocalEventPushBootstrap owns the EnMusic app-background scene.

                    }catch (e : Exception){
                        e.printStackTrace()
                    }


                }
            })
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        lauchFlag = true
        MMKV.initialize(this)
        UserSegmentHelper.startGate(this)
        if (BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
        val gdprMetaData = MetaData(this)
        gdprMetaData["gdpr.consent"] = true
        gdprMetaData.commit()
         MBridgeSDKFactory.getMBridgeSDK().setConsentStatus(this, MBridgeConstans.IS_SWITCH_ON)
        initManager()
        LocalPushTelemetry.listener = object : LocalPushTelemetry.Listener {
            override fun funnel(context: Context, values: Map<String, String>) {
                MintyPushAnalytics.logLpFunnel(context, values)
            }

            override fun report(context: Context, route: String, reason: String, result: String) {
                MintyPushAnalytics.reportBackend(context, "10008", route, reason, result)
            }
        }
        LocalPushUi.widgetPinnedChecker = { WidgetPinHelper.hasPinnedWidget(it) }
        LocalEventPushBootstrap.start(this)
        MintyRuntime.bootstrap(this)
        RuntimeRemoteConfig.refreshIfNeeded(this)
        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        MintyMusicBe.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
//        if (languageTag == "zh-TW") {
//            KuGou.useTraditionalChinese = true
//        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                MintyMusicBe.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
//                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
//                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] == true) {
            MintyMusicBe.useLoginForBrowse = true
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    MintyMusicBe.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: MintyMusicBe.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        } ?: MintyMusicBe.DEFAULT_VISITOR_DATA
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    MintyMusicBe.cookie = cookie
                }
        }
    }


}
