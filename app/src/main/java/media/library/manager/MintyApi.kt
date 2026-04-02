package media.library.manager

import android.content.Intent
import android.os.Build
import android.text.TextUtils
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.JsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.ads.AdValue
import com.google.gson.Gson
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.application.App.Companion.isCanJumpOm
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.MMKV_MINTY_COEFS
import com.rapid.prominence.minty.cons.MMKV_LAUCH_TIME
import com.rapid.prominence.minty.cons.MMKV_MINTY_LINES
import com.rapid.prominence.minty.cons.MMKV_MINTY_SYS
import com.rapid.prominence.minty.cons.MINTY_APP_AF_CAMPAIGN
import com.rapid.prominence.minty.cons.MINTY_APP_AF_MEDIA_SOURCE
import com.rapid.prominence.minty.cons.MINTY_APP_AF_USER
import com.rapid.prominence.minty.cons.MINTY_APP_APP_AD_CACHE_TM
import com.rapid.prominence.minty.cons.MINTY_APP_APP_AD_DATA
import com.rapid.prominence.minty.cons.MINTY_APP_APP_APP_CACHE_TM
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFER_INFO
import com.rapid.prominence.minty.cons.MINTY_APP_APP_REFRESH
import com.rapid.prominence.minty.cons.MINTY_APP_CURRENT_PAGE
import com.rapid.prominence.minty.cons.MINTY_APP_GOOGLE_REFER_SOURCE
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.cons.MMKV_MINTY_TOTAL_LINES
import com.rapid.prominence.minty.cons.MMKV_MINTY_TOTAL_LINES_TWO
import com.rapid.prominence.minty.cons.MMKV_NOTIFICATION_CONTENT
import com.rapid.prominence.minty.cons.MMKV_NOTIFICATION_TITLE
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.extensions.firebaseLog
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.rapid.prominence.minty.utils.getInstallSys
import com.tencent.mmkv.MMKV
import media.library.flys.model.MintyProductInfo
import media.library.flys.tools.MintyStuTools.getCountryInfo
import media.library.flys.tools.MintyStuTools.uniqueId
import media.library.flys.GoogleMobileAdsConsentManager
import media.library.flys.model.ParamsInfoArgsModel
import media.library.flys.model.ModeInfoArgsModel
import media.library.flys.model.MintyCoreInfo
import media.library.flys.model.PageEvArgsModel
import media.library.flys.tools.SpeMintyTools
import media.library.flys.tools.EventsAppendTool.lauchFlag

class MintyApi {
    companion object{
        @JvmStatic
        fun commonConfig(): String {
            val ctx = App.instance ?: return ""
            return ctx.getString(R.string.minty_ser)
        }

        private var  common_basic_info=
            "${commonConfig()}/qlltm/rhzhu/vfjw/build1/vcjang?syc=${
                uniqueId(
                    App.instance
                )
            }&pgdmhi=${commonUniIds()}"

        var value_info =
            "${commonConfig()}/tuylpw/gdd/sjvb/build2/tcota"


        val app_info =
            "${commonConfig()}/snfr/vfb/otwyul/revision1/rnzy/iuj?berwb=${uniqueId(
                App.instance
            )}&dkz=${commonUniIds()}"


        @JvmStatic
        fun commonKys(): String {
            val ctx = App.instance ?: return ""
            return ctx.getString(R.string.minty_k)
        }

        @JvmStatic
        fun commonUniIds(): String {
            val ctx = App.instance ?: return ""
            return ctx.getString(R.string.minty_d)

        }

        fun getCoreInfo(view: MintyBaseActivity, flag : Boolean = false) {
            if (System.currentTimeMillis() - MMKV.defaultMMKV()
                    .getLong(MINTY_APP_APP_AD_CACHE_TM, 0) > 60 * 60 * 1000
            ) {
                MintyWorkQueue.get().requestConfigInfo(
                    HashMap(),
                    common_basic_info,
                    object : MintyCommonInter.BasicRequestListener {

                        override fun loadFailed(message: String) {
                        }

                        override fun loadSuccessConfig(configInfo: String) {
                            try {
                                Gson().fromJson(
                                    configInfo,
                                    MintyCoreInfo::class.java
                                )?.run {
                                    if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.sypus == 200) {
                                        if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.qsvs.isNullOrEmpty()) {
                                            if (!flag){
                                                musToMain(view)
                                            }
                                            return@run
                                        } else {
                                            LogUtils.e("vense succcc","vense suc ----- $configInfo")
                                            MMKV.defaultMMKV()
                                                .putString(MINTY_APP_APP_AD_DATA, configInfo)
                                            MMKV.defaultMMKV().putLong(
                                                MINTY_APP_APP_AD_CACHE_TM,
                                                System.currentTimeMillis()
                                            )
                                        }
                                        SpeMintyTools.homeStudy.initMusConfig(view,flag)
                                    } else {
                                        if (!flag){
                                            musToMain(view)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                MMKV.defaultMMKV().putLong(MINTY_APP_APP_AD_CACHE_TM, 0)
                                e.printStackTrace()
                            }

                        }
                    })
            } else {
                if (!flag){
                    SpeMintyTools.homeStudy.initMusConfig(view)
                }
            }
            getProductInfos()
        }



        private fun jsonInfo(value: AdValue, adId: String, adsType: String, fbIpValue: Double,pos: Long): String {
            var json = ""
//            val catPa: HashMap<String, String> = java.util.HashMap()
            try {
                json  = Gson().toJson(
                    ParamsInfoArgsModel().apply {
                    vsws = adId.replace("/","-")
                    gdfeu = "Show"
                    hdyov = if(lauchFlag)"Cold" else("Warm")
                    nfct = adsType
                    slthqf = pos.toString()
                    kjge = value.valueMicros.toString()
                    xuz = value.currencyCode
                    nqkcl = value.precisionType.toString()
                    udxn = getCountryInfo(
                        App.instance
                    ).lowercase()
                    lpijf = "AdMob"
                    mintyFInstall = getInstallSys().toString()
                    mintyFValue = fbIpValue.toString()
                    mintyFOpens = MMKV.defaultMMKV().getLong("lauch_f_mmkv",0L).toString()
                    mintyPastTm = ((System.currentTimeMillis() - getInstallSys()) / 1000).toString()
                    mintyMediaSource = "mediaSource"+  MMKV.defaultMMKV().getString(
                        MINTY_APP_AF_MEDIA_SOURCE,"")
                    mintyCampaign = "campaign"+MMKV.defaultMMKV().getString(MINTY_APP_AF_CAMPAIGN,"")
                    mintyAFUser = "mintyAFUser"+MMKV.defaultMMKV().getString(MINTY_APP_AF_USER,"")
                    googleRefer =  MMKV.defaultMMKV().getString(MINTY_APP_GOOGLE_REFER_SOURCE,"empty")
                    currentUserPage = MMKV.defaultMMKV().getString(MINTY_APP_CURRENT_PAGE,"local")
                })
//
            }catch (e : Exception){
                e.printStackTrace()
            }

            return json
        }

        private fun lauArgs(): String {
           var json = ""
            try {
               json =    Gson().toJson(
                   ModeInfoArgsModel().apply {
                   hdyov = if(lauchFlag)"Cold" else("Warm")
                })
            }catch (e : Exception){
                e.printStackTrace()
            }


            return json
        }

        fun handleValues(adsId: String, type: String, value: AdValue, fbIpValue: Double,pos: Long){
            try {
                MintyWorkQueue.get().upValues(
                    stringHashMap(adsId, type, value, fbIpValue,pos),
                    value_info,
                    object : MintyCommonInter.BasicRequestListener {

                        override fun loadFailed(message: String) {
                            LogUtils.e(" message",message)

                        }

                        override fun loadSuccessConfig(configInfo: String) {
                            LogUtils.e(" message",configInfo)

                        }


                    })
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
        private fun pageArgs(): String {
            var json = ""
            try {
                json =    Gson().toJson(
                    PageEvArgsModel().apply {
                    mintyMediaSource = "mediaSource"+  MMKV.defaultMMKV().getString(
                        MINTY_APP_AF_MEDIA_SOURCE,"")
                    mintyCampaign = "campaign"+MMKV.defaultMMKV().getString(MINTY_APP_AF_CAMPAIGN,"")
                    mintyAFUser = "mintyAFUser"+MMKV.defaultMMKV().getString(MINTY_APP_AF_USER,"")
                    currentUserPage = MMKV.defaultMMKV().getString(MINTY_APP_CURRENT_PAGE,"local")
                    googleRefer =  MMKV.defaultMMKV().getString(MINTY_APP_GOOGLE_REFER_SOURCE,"empty")
                })
            }catch (e : Exception){
                e.printStackTrace()
            }

            return json
        }


        private fun installEventsUpload(){
            try {
                if (!MMKV.defaultMMKV().getBoolean("install_event",false)){
                    MintyWorkQueue.get().upValues(
                        installEvents(),
                        value_info,
                        object : MintyCommonInter.BasicRequestListener {

                            override fun loadFailed(message: String) {

                            }

                            override fun loadSuccessConfig(configInfo: String) {
                                LogUtils.e(configInfo)
//                                Log.e("install","install suc ----- $configInfo")

                                try {
                                    Gson().fromJson(
                                        configInfo,
                                        MintyCoreInfo::class.java
                                    )?.run {
                                        if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.gfzzl == 200) {
                                            MMKV.defaultMMKV().putBoolean("install_event",true)
                                            LogUtils.e("install_event suc -----")
                                        }
                                    }
                                }catch (e : Exception){
                                    e.printStackTrace()
                                }

                            }


                        })
                }

            }catch (e : Exception){
                e.printStackTrace()
            }

        }


         fun pageAEventsUpload(){
            try {
                if (!MMKV.defaultMMKV().getBoolean("page_event_a",false)){
                    MintyWorkQueue.get().upValues(
                        pageEvents(),
                        value_info,
                        object : MintyCommonInter.BasicRequestListener {

                            override fun loadFailed(message: String) {

                            }

                            override fun loadSuccessConfig(configInfo: String) {
                                LogUtils.e(configInfo)
//                                Log.e("install","install suc ----- $configInfo")

                                try {
                                    Gson().fromJson(
                                        configInfo,
                                        MintyCoreInfo::class.java
                                    )?.run {
                                        if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.gfzzl == 200) {
                                            MMKV.defaultMMKV().putBoolean("page_event_a",true)
                                            LogUtils.e("AAAApage_event_a suc -----")
                                        }
                                    }
                                }catch (e : Exception){
                                    e.printStackTrace()
                                }

                            }


                        })
                }
                if (!MMKV.defaultMMKV().getBoolean("firebase_event_a",false)){
                    firebaseLog("app_local")
                    MMKV.defaultMMKV().putBoolean("firebase_event_a",true)
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }
         fun pageBEventsUpload(){
            try {
                if (!MMKV.defaultMMKV().getBoolean("page_event_b",false)){
                    MintyWorkQueue.get().upValues(
                        pageEvents(),
                        value_info,
                        object : MintyCommonInter.BasicRequestListener {

                            override fun loadFailed(message: String) {

                            }

                            override fun loadSuccessConfig(configInfo: String) {
                                LogUtils.e(configInfo)
//                                Log.e("install","install suc ----- $configInfo")

                                try {
                                    Gson().fromJson(
                                        configInfo,
                                        MintyCoreInfo::class.java
                                    )?.run {
                                        if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.gfzzl == 200) {
                                            MMKV.defaultMMKV().putBoolean("page_event_b",true)
                                            LogUtils.e("BBBBpage_event_b suc -----")
                                        }
                                    }
                                }catch (e : Exception){
                                    e.printStackTrace()
                                }

                            }


                        })
                }
                if (!MMKV.defaultMMKV().getBoolean("firebase_event_b",false)){
                    firebaseLog("app_minty")
                    MMKV.defaultMMKV().putBoolean("firebase_event_b",true)
                }
            }catch (e : Exception){
                e.printStackTrace()
            }

        }


        fun lauchEventsUpload(){
            try {
                    MintyWorkQueue.get().upValues(
                        lauchEvents(),
                        value_info,
                        object : MintyCommonInter.BasicRequestListener {

                            override fun loadFailed(message: String) {

                            }

                            override fun loadSuccessConfig(configInfo: String) {
                            }


                        })

            }catch (e : Exception){
                e.printStackTrace()
            }

        }
        private fun stringHashMap(
            adsId: String,
            adsType: String,
            value: AdValue, fbIpValue: Double,pos: Long
        ): HashMap<String, String> {
            val stringStringHashMap: HashMap<String, String> = java.util.HashMap()
            try {
                stringStringHashMap["mji"] = AppUtils.getAppName()
                stringStringHashMap["uqn"] = "Android"
                stringStringHashMap["csfq"] = Build.BRAND
                stringStringHashMap["ntmy"] = Build.MODEL
                stringStringHashMap["mrs"] = "10001"
                stringStringHashMap["nkwol"] = ""+System.currentTimeMillis()
                if (!TextUtils.isEmpty(ActivityUtils.getTopActivity().localClassName )){
                    stringStringHashMap["zewew"] = ActivityUtils.getTopActivity().localClassName
                }
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+"")){
                    stringStringHashMap["vlfhdy"] = MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+""
                }
                stringStringHashMap["sna"] =  JsonUtils.formatJson(jsonInfo(
                    value,
                    adsId,
                    adsType,
                    fbIpValue,pos
                ))
                stringStringHashMap["sst"] = commonUniIds()
                stringStringHashMap["wcww"] = "" +   uniqueId(
                    App.instance
                )
                stringStringHashMap["airftw"] = Build.VERSION.RELEASE
                stringStringHashMap["dgjva"] = AppUtils.getAppVersionName()
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString("ad_id",""))){
                    stringStringHashMap["wuzma"] = MMKV.defaultMMKV().getString("ad_id","")+""
                }
                LogUtils.e(stringStringHashMap.toString())
                return stringStringHashMap
            }catch (e : Exception){
                e.printStackTrace()
            }
            return stringStringHashMap
        }


        private fun installEvents(

        ): HashMap<String, String> {
            val stringStringHashMap: HashMap<String, String> = java.util.HashMap()
            try {
                stringStringHashMap["mji"] = AppUtils.getAppName()
                stringStringHashMap["uqn"] = "Android"
                stringStringHashMap["csfq"] = Build.BRAND
                stringStringHashMap["ntmy"] = Build.MODEL
                stringStringHashMap["mrs"] = "10003"
                stringStringHashMap["nkwol"] = ""+System.currentTimeMillis()
                if (!TextUtils.isEmpty(ActivityUtils.getTopActivity().localClassName )){
                    stringStringHashMap["zewew"] = ActivityUtils.getTopActivity().localClassName
                }
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+"")){
                    stringStringHashMap["vlfhdy"] = MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+""
                }
                stringStringHashMap["sst"] = commonUniIds()
                stringStringHashMap["wcww"] = "" +   uniqueId(
                    App.instance
                )
                stringStringHashMap["airftw"] = Build.VERSION.RELEASE
                stringStringHashMap["dgjva"] = AppUtils.getAppVersionName()
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString("ad_id",""))){
                    stringStringHashMap["wuzma"] = MMKV.defaultMMKV().getString("ad_id","")+""
                }
                LogUtils.e(stringStringHashMap.toString())
            }catch (e : Exception){
                e.printStackTrace()
            }
            return stringStringHashMap
        }



        private fun pageEvents(

        ): HashMap<String, String> {
            val stringStringHashMap: HashMap<String, String> = java.util.HashMap()
            try {
                stringStringHashMap["mji"] = AppUtils.getAppName()
                stringStringHashMap["uqn"] = "Android"
                stringStringHashMap["csfq"] = Build.BRAND
                stringStringHashMap["ntmy"] = Build.MODEL
                stringStringHashMap["mrs"] = "10009"
                stringStringHashMap["nkwol"] = ""+System.currentTimeMillis()
                if (!TextUtils.isEmpty(ActivityUtils.getTopActivity().localClassName )){
                    stringStringHashMap["zewew"] = ActivityUtils.getTopActivity().localClassName
                }
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+"")){
                    stringStringHashMap["vlfhdy"] = MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+""
                }
                stringStringHashMap["sst"] = commonUniIds()
                stringStringHashMap["wcww"] = "" +   uniqueId(
                    App.instance
                )
                stringStringHashMap["airftw"] = Build.VERSION.RELEASE
                stringStringHashMap["dgjva"] = AppUtils.getAppVersionName()
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString("ad_id",""))){
                    stringStringHashMap["wuzma"] = MMKV.defaultMMKV().getString("ad_id","")+""
                }
                stringStringHashMap["sna"] =  pageArgs()

                stringStringHashMap["smxly"] =  System.currentTimeMillis().toString()+ uniqueId(App.instance)
                LogUtils.e(stringStringHashMap.toString())
            }catch (e : Exception){
                e.printStackTrace()
            }
            return stringStringHashMap
        }


        private fun lauchEvents(

        ): HashMap<String, String> {
            val stringStringHashMap: HashMap<String, String> = java.util.HashMap()
            try {
                stringStringHashMap["mji"] = AppUtils.getAppName()
                stringStringHashMap["uqn"] = "Android"
                stringStringHashMap["csfq"] = Build.BRAND
                stringStringHashMap["ntmy"] = Build.MODEL
                stringStringHashMap["mrs"] = "10005"
                stringStringHashMap["nkwol"] = ""+System.currentTimeMillis()
                if (!TextUtils.isEmpty(ActivityUtils.getTopActivity().localClassName )){
                    stringStringHashMap["zewew"] = ActivityUtils.getTopActivity().localClassName
                }
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+"")){
                    stringStringHashMap["vlfhdy"] = MMKV.defaultMMKV().getString(
                        MINTY_APP_APP_REFER_INFO,"")+""
                }
                stringStringHashMap["sst"] = commonUniIds()
                stringStringHashMap["wcww"] = "" +   uniqueId(
                    App.instance
                )
                stringStringHashMap["airftw"] = Build.VERSION.RELEASE
                stringStringHashMap["dgjva"] = AppUtils.getAppVersionName()
                if(!TextUtils.isEmpty(MMKV.defaultMMKV().getString("ad_id",""))){
                    stringStringHashMap["wuzma"] = MMKV.defaultMMKV().getString("ad_id","")+""
                }
                stringStringHashMap["sna"] =  lauArgs()

                stringStringHashMap["smxly"] =  System.currentTimeMillis().toString()+ uniqueId(App.instance)
                LogUtils.e(stringStringHashMap.toString())
            }catch (e : Exception){
                e.printStackTrace()
            }
            return stringStringHashMap
        }


        fun musToMain(infoCtx: MintyBaseActivity?) {
            try {
                if (infoCtx == null || infoCtx.isFinishing || infoCtx.isDestroyed){
                    return
                }
                if ((!isCanJumpOm && !GoogleMobileAdsConsentManager.getInstance(
                        App.instance
                    ).canRequestAds())
                ){
                    LogUtils.e("---- canRequestAds ---- error")
                    return
                }
                infoCtx.runOnUiThread {
                    if (MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER,false)){
                        infoCtx.startActivity(Intent(infoCtx, MintySecMainPage::class.java))
                    }else{
                        infoCtx.startActivity(Intent(infoCtx, EiMainPage::class.java))
                    }
                    infoCtx.finish()
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }



        private fun cleanBasicInfo() {
            MMKV.mmkvWithID("mmkv_music_app").clear()
            MMKV.defaultMMKV()
                .putString(MINTY_APP_APP_AD_DATA, "")
            MMKV.defaultMMKV().putLong(
                MINTY_APP_APP_AD_CACHE_TM,
                0
            )
        }

        private fun getProductInfos() {
            if (System.currentTimeMillis() - MMKV.defaultMMKV()
                    .getLong(MINTY_APP_APP_APP_CACHE_TM, 0) > 60 * 60 * 1000
            ) {
                MintyWorkQueue.get().requestConfigInfo(
                    HashMap(),
                    app_info,
                    object : MintyCommonInter.BasicRequestListener {

                        override fun loadFailed(message: String) {
                            LogUtils.e(message)

                        }

                        override fun loadSuccessConfig(configInfo: String) {
                            try {
                                LogUtils.e(configInfo)
                                Gson().fromJson(
                                    configInfo,
                                    MintyProductInfo::class.java
                                )?.run {
                                    LogUtils.e("product info before","product ----- $configInfo")
                                    if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.wjw  == 200
                                        ) {
                                        if (gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.jbe != null) {
                                            gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.jbe?.fldvvu?.let {
                                                MMKV.defaultMMKV()
                                                    .putInt(MINTY_APP_APP_REFRESH, it)
                                            }

                                            MMKV.defaultMMKV().putLong(
                                                MINTY_APP_APP_APP_CACHE_TM,
                                                System.currentTimeMillis()
                                            )
                                            gxjovn?.hzaxkl?.firstOrNull()?.wbncvq?.jbe?.bscm?.apply {
                                                if (isNullOrEmpty()){
                                                    return@apply
                                                }
                                                for (i in this){
                                                    if ("standard_line" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_MINTY_LINES, i.nalzxc)
                                                    }
                                                    if ("total_line_first" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_MINTY_TOTAL_LINES, i.nalzxc)
                                                    }
                                                    if ("total_line_second" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_MINTY_TOTAL_LINES_TWO, i.nalzxc)
                                                    }
                                                    if ("upload_coef" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_MINTY_COEFS, i.nalzxc)
                                                    }
                                                    if ("window_period_time" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_MINTY_SYS, i.nalzxc)
                                                    }
                                                    
                                                    if ("is_exit_notification" == i.hyohx){
                                                        MMKV.defaultMMKV().putBoolean("is_send",i.nalzxc == "1")
                                                    } 
                                                    if ("lauch_time" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putInt(MMKV_LAUCH_TIME, i.nalzxc.toInt())
                                                    }
                                                  if ("notification_title" == i.hyohx){
                                                      MMKV.defaultMMKV()
                                                          .putString(MMKV_NOTIFICATION_TITLE, i.nalzxc)
                                                    }
                                                    if ("notification_content" == i.hyohx){
                                                        MMKV.defaultMMKV()
                                                            .putString(MMKV_NOTIFICATION_CONTENT, i.nalzxc)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                MMKV.defaultMMKV().putLong(MINTY_APP_APP_APP_CACHE_TM, 0)
                                e.printStackTrace()
                            }

                        }
                    })
            }
            installEventsUpload()
        }


    }

}