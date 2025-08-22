package com.rapid.prominence.minty.application

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.rapid.prominence.minty.BuildConfig
import com.rapid.prominence.minty.cons.ContentCountryKey
import com.rapid.prominence.minty.cons.ContentLanguageKey
import com.rapid.prominence.minty.cons.CountryCodeToName
import com.rapid.prominence.minty.cons.InnerTubeCookieKey
import com.rapid.prominence.minty.cons.LanguageCodeToName
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
import com.tencent.mmkv.MMKV
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
import timber.log.Timber
import java.net.Proxy
import java.util.Locale
import java.util.UUID


@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    private var oldTimes = 0L
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private fun initAdIdWithTimeout() {


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
        var isOrganic = false
        var isNextPlay = false
        var ouUserIsCallBack = false
        var referStatusCallBack = false
        var isClicked = false
        var lauchFlag = true

    }



    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        lauchFlag = true
        MMKV.initialize(this)
        if (BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
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
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
//        if (level == TRIM_MEMORY_UI_HIDDEN) {
//            LogUtils.e("appppp ----")
//            ActivityUtils.finishAllActivities()
//        }
    }
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
}