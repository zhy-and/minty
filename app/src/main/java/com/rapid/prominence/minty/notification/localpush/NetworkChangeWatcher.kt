package com.rapid.prominence.minty.notification.localpush

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

/** 网络变化本地推送监听（去抖 + 指纹比对，逻辑对齐 purgu_push）。 */
object NetworkChangeWatcher {
    private const val TAG = "NetworkChangeWatcher"
    private const val DEBOUNCE_MS = 1_500L

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingNotify: Runnable? = null

    @Volatile
    private var lastFingerprint: String? = null

    private var appContextRef: Context? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scheduleEvaluate("onAvailable")
        }

        override fun onLost(network: Network) {
            scheduleEvaluate("onLost")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            scheduleEvaluate("onCapabilitiesChanged")
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "网络监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        appContextRef = appContext
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            LogUtils.e(TAG, "无法获取 ConnectivityManager，网络场景不可用")
            return
        }
        lastFingerprint = currentFingerprint(cm)
        val request = NetworkRequest.Builder().build()
        runCatching {
            cm.registerNetworkCallback(request, callback)
            registered = true
            LogUtils.i(TAG, "网络监听注册成功 debounceMs=$DEBOUNCE_MS seed=$lastFingerprint")
        }.onFailure {
            LogUtils.e(TAG, "网络监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun scheduleEvaluate(reason: String) {
        val appContext = appContextRef ?: return
        pendingNotify?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingNotify = null
            evaluateAndMaybeShow(appContext, reason)
        }
        pendingNotify = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun evaluateAndMaybeShow(context: Context, reason: String) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val fingerprint = currentFingerprint(cm)
        val previous = lastFingerprint
        if (fingerprint == previous) {
            LogUtils.i(TAG, "去抖后指纹未变化，跳过 network_change：reason=$reason")
            return
        }
        lastFingerprint = fingerprint
        LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.NETWORK_CHANGE)
    }

    private fun currentFingerprint(cm: ConnectivityManager): String {
        val active = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(active) ?: return "unknown"
        val transports = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("bt")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
        }
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return "net=${transports.joinToString("+").ifEmpty { "other" }};internet=$internet;validated=$validated"
    }
}
