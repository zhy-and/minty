package media.library.flys.tools

import com.blankj.utilcode.util.LogUtils
import com.rapid.prominence.minty.cons.MINTY_APP_BD_CLICK_CT
import com.rapid.prominence.minty.cons.MINTY_APP_BD_SHOW_CT
import com.rapid.prominence.minty.cons.MINTY_APP_JK_CLICK_CT
import com.rapid.prominence.minty.cons.MINTY_APP_JK_SHOW_CT
import com.rapid.prominence.minty.cons.MINTY_APP_JK_SHOW_ST
import com.rapid.prominence.minty.cons.albumDetBanner
import com.rapid.prominence.minty.cons.albumDetInter
import com.rapid.prominence.minty.cons.artistDetBanner
import com.rapid.prominence.minty.cons.artistDetInter
import com.rapid.prominence.minty.cons.finishPageInterOne
import com.rapid.prominence.minty.cons.finishPageInterTwo
import com.rapid.prominence.minty.cons.homeBanner
import com.rapid.prominence.minty.cons.musColdStart
import com.rapid.prominence.minty.cons.musHotStart
import com.rapid.prominence.minty.cons.playViewBanner
import com.rapid.prominence.minty.cons.playViewsInter
import com.rapid.prominence.minty.cons.queueInter
import com.rapid.prominence.minty.cons.queueListBanner
import com.rapid.prominence.minty.cons.searchInter
import com.rapid.prominence.minty.cons.searchResultBanner
import com.tencent.mmkv.MMKV
import java.util.Calendar

object EventsAppendTool  {


    fun showCountAdd(postion: Long) {
        mmkvClickRules()?.run {
            var showCount = getInt("$MINTY_APP_BD_SHOW_CT-${postion}", 0)
            showCount += 1
            LogUtils.e(basicInfoLog(postion) +"show  count ====="+showCount)
            putInt("$MINTY_APP_BD_SHOW_CT-${postion}", showCount)
        }
    }

    fun appendClick(pos: Long) {
        mmkvClickRules()?.run {
            var count = getInt("$MINTY_APP_BD_CLICK_CT-${pos}", 0)
            count += 1
            LogUtils.e(basicInfoLog(pos) +" click count ====="+count)
            putInt("$MINTY_APP_BD_CLICK_CT-${pos}", count)
        }
    }


    fun checkTimes(cacheTime : Long): Boolean {
        if (System.currentTimeMillis() - cacheTime   > 60 * 60 * 1000) {
            return true
        }
        return false
    }



    fun posIsCanShow(artPos: Long): Boolean {
        val minty_valuelateAnyClickCount =   MMKV.mmkvWithID("mmkv_music_app").getInt("$MINTY_APP_JK_CLICK_CT-${artPos}",0)
        val minty_valuelateAnyShowCount = MMKV.mmkvWithID("mmkv_music_app").getInt("$MINTY_APP_JK_SHOW_CT-${artPos}",0)
        val minty_valuelateAnyShow = MMKV.mmkvWithID("mmkv_music_app").getBoolean("$MINTY_APP_JK_SHOW_ST-${artPos}",false)
        LogUtils.e(basicInfoLog(artPos) +"配置开关"+minty_valuelateAnyShow)
        if (!minty_valuelateAnyShow) {
            return true
        }
        mmkvClickRules()?.run {
            val nativeTranslateAnyClick =  getInt("$MINTY_APP_BD_CLICK_CT-${artPos}",0)
            LogUtils.e(basicInfoLog(artPos) +"本地累积点击次数"+nativeTranslateAnyClick)
            if (nativeTranslateAnyClick >= minty_valuelateAnyClickCount) {
                return true
            }
        }
        mmkvClickRules()?.run {
            val nativeTranslateAnyShow = getInt("$MINTY_APP_BD_SHOW_CT-${artPos}",0)
            LogUtils.e(basicInfoLog(artPos) +"本地累积展示次数"+nativeTranslateAnyShow)
            if (nativeTranslateAnyShow >= minty_valuelateAnyShowCount) {
                return true
            }
        }
        return false
    }

    private fun mmkvClickRules(): MMKV? {
        val mmkv = MMKV.mmkvWithID("minty_mmkv")
                val currentD = Calendar.getInstance().get(Calendar.DATE)
                val lastD = mmkv.getInt("sp_day", 0)
                if (currentD != lastD){
                    mmkv.clear()
                }
                mmkv.putInt("sp_day", currentD)

        return mmkv
    }

    @JvmStatic
    var cyIsShowingFLag = false
    @JvmStatic
    var lauchFlag = false

    fun basicInfoLog(position: Long): String {
        var name = ""
        when (position) {
            homeBanner() -> {name = "homeBanner minty_value pos :"}
            searchResultBanner() -> {name ="searchResultBanner minty_value pos :"}
            albumDetBanner() -> {name ="albumDetBanner minty_value pos :"}
            artistDetBanner() -> {name ="artistDetBanner minty_value pos :"}
            playViewBanner() -> {name ="playViewBanner minty_value pos :"}
            queueListBanner() -> {name ="queueListBanner minty_value pos :"}
            searchInter() -> {name ="searchInter minty_value pos :"}
            albumDetInter() -> {name ="albumDetInter minty_value pos :"}
            artistDetInter() -> {name ="artistDetInter minty_value pos :"}
            playViewsInter() -> {name ="playViewsInter minty_value pos :"}
            queueInter() -> {name ="queueInter minty_value pos :"}
            finishPageInterOne() -> {name ="finishPageInterOne minty_value pos :"}
            finishPageInterTwo() -> {name ="finishPageInterTwo minty_value pos :"}
            musColdStart() -> {name ="KepCold minty_value pos :"}
            musHotStart() -> {name ="KepHot minty_value pos :"}
        }
        return name
    }

    fun intellIsCanLoadStart(): Boolean {
        return !posIsCanShow(musColdStart())
                || !posIsCanShow(musHotStart())
    }



}