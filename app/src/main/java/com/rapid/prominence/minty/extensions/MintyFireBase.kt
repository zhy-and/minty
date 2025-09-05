package com.rapid.prominence.minty.extensions

import com.blankj.utilcode.util.LogUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.rapid.prominence.minty.application.App


fun firebaseLog(eventName: String){
    try {
        App?.instance?.let {
            LogUtils.e(eventName)
            FirebaseAnalytics.getInstance(it).logEvent(eventName,null)
        }
    }catch (e : Exception){
        e.printStackTrace()
    }

}