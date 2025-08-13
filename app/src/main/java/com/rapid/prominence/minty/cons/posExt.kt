package com.rapid.prominence.minty.cons

import com.blankj.utilcode.util.LogUtils
import java.util.Locale

fun homeBanner(): Long {
    return 1922238084375121920
}
fun historyBanner(): Long {
    return 1922266169126817792
}
fun collectBanner(): Long {
    return 1922266250063515648
}
fun playListDetBanner(): Long {
    return 1922265739786719232
}
fun searchHistoryBanner(): Long {
    return 1922265399423275008
}
fun homeMoodDetailBanner(): Long {
    return 1922265876335431680
}
fun homeReleaseDetailBanner(): Long {
    return 1922265597492727808
}
fun projectDetailBanner(): Long {
    return 1922265660294828032
}
fun searchResultBanner(): Long {
    return 1922265517232623616
}


fun albumDetBanner(): Long {
    return 1922265804457775104
}

fun artistDetBanner(): Long {
    return 1922265944107388928
}

fun playViewBanner(): Long {
    return 1922266104577003520
}


fun queueListBanner(): Long {
    return 1922265999555956736
}

fun searchInter(): Long {
    return  1922553963234988032
}

fun historyInter(): Long {
    return  1922554567881134080
}


fun collectInter(): Long {
    return  1922554631888834560
}


fun albumDetInter(): Long {
    return   1922554254868484096
}


fun searchRecordInter(): Long {
    return  1922554023936921600
}
fun artistDetInter(): Long {
    return  1922554377829093376
}
fun projectDetInter(): Long {
    return  1922554137581981696
}

fun playViewsInter(): Long {
    return 1922554506886909952
}
fun playlistDetInter(): Long {
    return  1922554196373934080
}
fun queueInter(): Long {
    return  1922554436540567552
}
fun releaseDetailInter(): Long {
    return 1922554083040432128
}
fun homeMoodDetInter(): Long {
    return  1922554316175970304
}


fun finishPageInterOne(): Long {
    return  1922497796398780416
}

fun finishPageInterTwo(): Long {
    return 1922498001811542016
}
fun finishPageInterThr(): Long {
    return  1922498082836320256
}
fun finishPageInterFor(): Long {
    return  1922498154886729728
}

fun musColdStart(): Long {
    return  1922179115090710528
}

fun musHotStart(): Long {
    return 1922179420003241984
}

const  val MUSIC_CONSTANT_JK_CLICK_CT = "snap_mmkv_network_click_count"
const  val MUSIC_CONSTANT_JK_CLICK_UNIT = "snap_mmkv_network_click_unit"

const  val MUSIC_CONSTANT_JK_SHOW_CT = "snap_mmkv_network_show_count"
const  val MUSIC_CONSTANT_JK_SHOW_UNIT = "snap_mmkv_network_show_unit"

fun checkOUsers(): Boolean {
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


const val MUSIC_CONSTANT_JK_BUT_ST = "snap_mmkv_network_button_status"
const val MUSIC_CONSTANT_JK_SHOW_ST = "snap_mmkv_network_is_show"
const val MUSIC_CONSTANT_BD_CLICK_CT = "snap_mmkv_native_click_count"
const val MUSIC_CONSTANT_BD_SHOW_CT = "snap_mmkv_native_show_count"
const val MUSIC_CONSTANT_APP_AD_CACHE_TM = "snap_mmkv_ads_cm"
const val MUSIC_CONSTANT_APP_APP_CACHE_TM = "snap_mmkv_app_cm"
const val MUSIC_CONSTANT_APP_REFRESH = "snap_mmkv_refresh"
const val MUSIC_CONSTANT_APP_REFER_INFO = "snap_mmkv_re_infos"
//用戶來源
const val MUSIC_CONSTANT_IS_M_USER = "is_non_organic"
//渠道
const val MUSIC_CONSTANT_AF_MEDIA_SOURCE = "af_media_source"
const val MUSIC_CONSTANT_GOOGLE_REFER_SOURCE = "google_refer_source"
//廣告id
const val MUSIC_CONSTANT_AF_CAMPAIGN = "af_campaign"
const val MUSIC_CONSTANT_AF_USER = "af_status"
const val MUSIC_CONSTANT_CURRENT_PAGE= "current_page"
const val MUSIC_CONSTANT_APP_AD_DATA = "snap_mmkv_ads_data"
const val MUSIC_CONSTANT_APP_IS_AUTO_PLAY = "snap_mmkv_is_auto_play"
const val MMKV_LINES = "lines"
const val MMKV_COEFS = "coefs"
const val MMKV_SYS = "sys_tm"
const val MMKV_HOME_BACK_NO_FINISH = "finish_is"
const val MMKV_HOME_BTN_NO_FINISH = "home_btn_finish_is"
const val MMKV_LAUCH_TIME = "lauch_time"

const val MESSAGE_MSG_CODE = 0x100


