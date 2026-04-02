package com.rapid.prominence.minty.cons

import com.blankj.utilcode.util.LogUtils
import java.util.Locale
const  val MINTY_APP_JK_CLICK_CT = "minty_mmkv_network_click_count"

const  val MINTY_APP_JK_SHOW_CT = "minty_mmkv_network_show_count"

fun checkRDP(): Boolean {
    val currentCountry =  Locale.getDefault().country.toString().lowercase()
    LogUtils.e("currentCountry--- $currentCountry")
    for (i in countryCode){
        if (i == currentCountry){
            LogUtils.e("currentCountry true")
            return  true
        }
    }
    LogUtils.e("currentCountry false")
    return  false
}

private var countryCode = arrayOf("ie", "ee", "at", "bg", "be", "pl", "dk", "de", "fr",
    "fi", "nl", "cz", "hr", "lv", "lt", "lu", "ro", "mt",
    "pt", "se", "cy", "sk", "si", "es", "gr", "hu", "it","gb","ch")


fun searchInter(): Long {
    return  1962345098160377856
}

fun historyInter(): Long {
    return  1962345098378350592
}


fun collectInter(): Long {
    return  1962345098404564992
}


fun albumDetInter(): Long {
    return   1962345098256453632
}


fun searchRecordInter(): Long {
    return  1962345098130493440
}
fun artistDetInter(): Long {
    return  1962345098303377408
}
fun projectDetInter(): Long {
    return  1962345098206253056
}

fun playViewsInter(): Long {
    return 1962345098353840128
}
fun playlistDetInter(): Long {
    return  1962345098232074240
}
fun queueInter(): Long {
    return  1962345098328281088
}
fun releaseDetailInter(): Long {
    return 1962345098182135808
}
fun homeMoodDetInter(): Long {
    return  1962345098280833024
}


fun finishPageInterOne(): Long {
    return  1962345098035073024
}

fun finishPageInterTwo(): Long {
    return 1962345098058534912
}
fun finishPageInterThr(): Long {
    return  1962345098084487168
}
fun finishPageInterFor(): Long {
    return  1962345098109128704
}

fun musColdStart(): Long {
    return  1961265238292697088

}

fun musHotStart(): Long {
    return 1961265413821042688
}
fun homeBanner(): Long {
    return 1962345097712898048
}
fun historyBanner(): Long {
    return 1962345097989591040
}
fun collectBanner(): Long {
    return 1962345098010562560
}
fun playListDetBanner(): Long {
    return 1962345097840824320
}
fun searchHistoryBanner(): Long {
    return 1962345097736097792
}
fun homeMoodDetailBanner(): Long {
    return 1962345097888141312
}
fun homeReleaseDetailBanner(): Long {
    return 1962345097861533696
}
fun projectDetailBanner(): Long {
    return 1962345097816838144
}
fun searchResultBanner(): Long {
    return 1962345097765195776
}


fun albumDetBanner(): Long {
    return 1962345097861533696
}

fun artistDetBanner(): Long {
    return 1962345097912913920
}

fun playViewBanner(): Long {
    return 1962345097964425216
}


fun queueListBanner(): Long {
    return 1962345097937293312
}

const val MINTY_APP_JK_SHOW_ST = "minty_mmkv_network_is_show"
const val MINTY_APP_BD_CLICK_CT = "minty_mmkv_native_click_count"
const val MINTY_APP_BD_SHOW_CT = "minty_mmkv_native_show_count"
const val MINTY_APP_APP_AD_CACHE_TM = "minty_mmkv_ads_cm"
const val MINTY_APP_APP_APP_CACHE_TM = "minty_mmkv_app_cm"
const val MINTY_APP_APP_REFRESH = "minty_mmkv_refresh"
const val MINTY_APP_APP_REFER_INFO = "minty_mmkv_re_infos"
//用戶來源
const val MINTY_APP_IS_M_USER = "is_non_organic"
//渠道
const val MINTY_APP_AF_MEDIA_SOURCE = "af_media_source"
const val MINTY_APP_GOOGLE_REFER_SOURCE = "google_refer_source"
//廣告id
const val MINTY_APP_AF_CAMPAIGN = "af_campaign"
const val MINTY_APP_AF_USER = "af_status"
const val MINTY_APP_CURRENT_PAGE= "current_page"
const val MINTY_APP_APP_AD_DATA = "minty_mmkv_ads_data"
const val MMKV_MINTY_LINES = "minty_lines"
const val MMKV_MINTY_TOTAL_LINES = "minty_total_lines"
const val MMKV_MINTY_TOTAL_LINES_TWO = "minty_total_lines_two"
const val MMKV_MINTY_COEFS = "minty_coefs"
const val MMKV_MINTY_SYS = "minty_sys_tm"
const val MMKV_LAUCH_TIME = "lauch_time"
const val MMKV_NOTIFICATION_TITLE = "notification_title"
const val MMKV_NOTIFICATION_CONTENT = "notification_content"

const val MESSAGE_MSG_CODE = 0x100


