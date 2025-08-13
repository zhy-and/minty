package com.rapid.prominence.minty.extensions

//import com.google.firebase.analytics.FirebaseAnalytics

fun logClickEvents(eventName: String){
    try {
//        App?.instance?.let {
////            LogUtils.e(eventName)
//            FirebaseAnalytics.getInstance(it).logEvent(eventName,null)
//        }
    }catch (e : Exception){
        e.printStackTrace()
    }

}