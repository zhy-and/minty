package media.library.flys.tools
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.*
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.rapid.prominence.minty.application.App
import com.rapid.prominence.minty.base.MintyBaseActivity
import com.rapid.prominence.minty.cons.ShowListener
import com.rapid.prominence.minty.cons.StartUpListener
import com.rapid.prominence.minty.cons.musColdStart
import com.rapid.prominence.minty.cons.musHotStart
import media.library.flys.tools.MintyStuTools.valueUpload
import media.library.flys.tools.EventsAppendTool.lauchFlag

class KepingValueTools{



    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - startTimes
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        // Ad references in the app open beta will time out after four hours, but this time limit
        // may change in future beta versions. For details, see:
        // https://support.google.com/admob/answer/9341964?hl=en
        return intellObj != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */



    fun loadStartAds(context: MintyBaseActivity?, upListener : StartUpListener?= null) {
        context ?:return
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        // Do not load ad if there is an unused ad or one is already loading.
        if (TextUtils.isEmpty(startIds)){
            LogUtils.e("start id empty")
            return
        }
        LogUtils.e("start load kp")
        if (startIsLoading || isAdAvailable()) {
            return
        }
        startIsLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            startIds,
            request,
            object : AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param ad the loaded app open ad.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    LogUtils.d("kp Adapter class name:" + ad.responseInfo?.toString())

                    intellObj = ad
                    startIsLoading = false
                    startTimes = Date().time
                    LogUtils.d("AppOpenAdLoadCallback onAdLoaded.")
                    upListener?.loadSuc()
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    startIsLoading = false
                    upListener?.LoadedError()
                    LogUtils.d("AppOpenAdLoadCallback onAdFailedToLoad: " + loadAdError.toString())
                }
            }
        )
    }
    fun configStartIds(ids: String?) {
        if (TextUtils.isEmpty(ids)){
            LogUtils.e("empty kep id!")
            return
        }
        if (ids != null) {
            this.startIds = ids
        }
    }
    var intellObj: AppOpenAd? = null

    private var startIsLoading = false

    var startIsShowing = false

    private var  startIds = ""

    companion object {
        val intellTools: KepingValueTools by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            KepingValueTools() }
    }

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var startTimes: Long = 0

    /**
     * Load an app open ad.
     *
     * @param context the activity that shows the app open ad
     */
    /**
     * Shows an app open ad.
     *
     * @param context the activity that shows the app open ad
     * @param listener the listener to be notified when an app open ad is complete
     */
    fun showStartAds(context: MintyBaseActivity?, listener: ShowListener?= null) {
        context ?: return
        val pos : Long
        if (lauchFlag){
            pos =  musColdStart()
            LogUtils.e("kp  pos ------ cold")
        }else{
            pos = musHotStart()
            LogUtils.e("kp  pos ------ hot")
        }
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        // If the app open ad is already showing, do not show the ad again.
        if (EventsAppendTool.posIsCanShow(pos)){
            LogUtils.e("kp pod limit ------"+ EventsAppendTool.basicInfoLog(pos))
            return
        }
        if (startIsShowing) {
            LogUtils.d( "The app open ad is already showing.")
            return
        }
        if (context.isDestroyed || context.isFinishing){
            return
        }

        // If the app open ad is not available yet, invoke the callback.
        if (!isAdAvailable()) {
            LogUtils.d( "The app open ad is not ready yet. cache tm more than 4 h")
            intellObj = null
            loadStartAds(context)
            return
        }

        LogUtils.d("Will show ad.")

        intellObj?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                var startClicked = false
                override fun onAdClicked() {
                    super.onAdClicked()
                  try {
                      if (!startClicked) {
                          startClicked = true
                          EventsAppendTool.appendClick(pos)
                      }
                      App.isClicked = true
//                      if (playerConnection?.isPlaying?.value == true){
//                          ThreadUtils.runOnUiThreadDelayed({ ActivityUtils.finishAllActivities()},500)
//                      }
                  }catch (e :Exception){
                      e.printStackTrace()
                  }

                }

                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    intellObj = null
                    startIsShowing = false
                    LogUtils.d( "onAdDismissedFullScreenContent.")
                    listener?.dispathSuc()
//                        loadAd(activity)
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    intellObj = null
                    startIsShowing = false
                    LogUtils.d("onAdFailedToShowFullScreenContent: " + adError.message)
                    listener?.dispathSuc()
//                        loadAd(activity)
                }

                /** Called when fullscreen content is shown. */
                override fun onAdShowedFullScreenContent() {
                    //todo
//                    MobileAds.openAdInspector(context) { error ->
//                        // Error will be non-null if ad inspector closed due to an error.
//                    }
                    LogUtils.d("onAdShowedFullScreenContent.")
                    listener?.dispathError()
                    EventsAppendTool.showCountAdd(pos)

                }
            }
        intellObj?.setOnPaidEventListener {
                valueUpload(it, "Appopen",startIds,pos)
        }
        startIsShowing = true
        intellObj?.show(context)
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */


    /** Inner class that loads and shows app open ads. */





}