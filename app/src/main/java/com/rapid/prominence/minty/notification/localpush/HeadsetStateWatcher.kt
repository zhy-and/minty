package com.rapid.prominence.minty.notification.localpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

/**
 * 有线耳机插拔 + 蓝牙**音频**设备路由变化。
 *
 * 蓝牙侧不走 ACL、不申请蓝牙权限：用 [AudioManager.registerAudioDeviceCallback]
 * 仅在 A2DP/SCO（及 12+ LE Audio）成为可用音频输出时触发。
 * 场景键仍映射 [LocalEventPushHelper.EventType.BLUETOOTH_CONNECT] /
 * [LocalEventPushHelper.EventType.BLUETOOTH_DISCONNECT]。
 */
object HeadsetStateWatcher {
    private const val TAG = "HeadsetStateWatcher"
    private const val EXTRA_HEADSET_STATE = "state"
    /** A2DP + SCO 可能连续回调，合并为一次边沿。 */
    private const val BT_DEBOUNCE_MS = 400L

    @Volatile
    private var wiredRegistered = false

    @Volatile
    private var audioCallbackRegistered = false

    /** 注册后忽略首轮「当前已连接设备」快照，只认之后的边沿。 */
    @Volatile
    private var acceptAudioEdges = false

    @Volatile
    private var appContext: Context? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingBtConnect: Runnable? = null
    private var pendingBtDisconnect: Runnable? = null

    private val wiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_HEADSET_PLUG) return
            val plugged = intent.getIntExtra(EXTRA_HEADSET_STATE, -1) == 1
            val type = if (plugged) {
                LocalEventPushHelper.EventType.WIRED_HEADSET_CONNECT
            } else {
                LocalEventPushHelper.EventType.WIRED_HEADSET_DISCONNECT
            }
            LogUtils.i(TAG, "有线耳机 state=$plugged → ${type.name}")
            LocalEventPushHelper.show(context.applicationContext, type)
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (!acceptAudioEdges) {
                LogUtils.i(TAG, "忽略注册时音频设备快照(added)：${describeBluetooth(addedDevices)}")
                return
            }
            if (!addedDevices.any { it.isBluetoothAudioOutput() }) return
            LogUtils.i(TAG, "蓝牙音频设备已加入：${describeBluetooth(addedDevices)}")
            scheduleBluetoothEdge(connect = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (!acceptAudioEdges) {
                LogUtils.i(TAG, "忽略注册时音频设备快照(removed)：${describeBluetooth(removedDevices)}")
                return
            }
            if (!removedDevices.any { it.isBluetoothAudioOutput() }) return
            LogUtils.i(TAG, "蓝牙音频设备已移除：${describeBluetooth(removedDevices)}")
            scheduleBluetoothEdge(connect = false)
        }
    }

    fun start(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        startWiredHeadset(ctx)
        startBluetoothAudio(ctx)
    }

    private fun startWiredHeadset(ctx: Context) {
        if (wiredRegistered) {
            LogUtils.i(TAG, "有线耳机监听已注册，跳过")
            return
        }
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(wiredReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                ctx.registerReceiver(wiredReceiver, filter)
            }
            wiredRegistered = true
            LogUtils.i(TAG, "有线耳机监听注册成功")
        }.onFailure {
            LogUtils.e(TAG, "有线耳机监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun startBluetoothAudio(ctx: Context) {
        if (audioCallbackRegistered) {
            LogUtils.i(TAG, "蓝牙音频路由监听已注册，跳过")
            return
        }
        val audioManager = ctx.getSystemService(AudioManager::class.java)
        if (audioManager == null) {
            LogUtils.e(TAG, "AudioManager 不可用，无法监听蓝牙音频路由")
            return
        }
        runCatching {
            acceptAudioEdges = false
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
            audioCallbackRegistered = true
            // 注册过程中/紧随其后的首轮回调视为快照；下一帧起才接受边沿
            mainHandler.post {
                acceptAudioEdges = true
                LogUtils.i(TAG, "蓝牙音频路由监听已就绪（AudioDeviceCallback，无蓝牙权限）")
            }
        }.onFailure {
            LogUtils.e(
                TAG,
                "蓝牙音频路由监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}",
            )
        }
    }

    private fun scheduleBluetoothEdge(connect: Boolean) {
        if (connect) {
            pendingBtDisconnect?.let { mainHandler.removeCallbacks(it) }
            pendingBtDisconnect = null
            pendingBtConnect?.let { mainHandler.removeCallbacks(it) }
            val task = Runnable {
                pendingBtConnect = null
                val ctx = appContext ?: return@Runnable
                LogUtils.i(TAG, "蓝牙音频连接边沿 → BLUETOOTH_CONNECT")
                LocalEventPushHelper.show(ctx, LocalEventPushHelper.EventType.BLUETOOTH_CONNECT)
            }
            pendingBtConnect = task
            mainHandler.postDelayed(task, BT_DEBOUNCE_MS)
            return
        }

        pendingBtConnect?.let { mainHandler.removeCallbacks(it) }
        pendingBtConnect = null
        pendingBtDisconnect?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            pendingBtDisconnect = null
            val ctx = appContext ?: return@Runnable
            // 防抖结束后若仍有蓝牙音频输出，说明是路由切换而非真正断开
            if (hasBluetoothAudioOutput(ctx)) {
                LogUtils.i(TAG, "蓝牙音频 remove 后仍有输出设备，视为切换而非断开")
                return@Runnable
            }
            LogUtils.i(TAG, "蓝牙音频断开边沿 → BLUETOOTH_DISCONNECT")
            LocalEventPushHelper.show(ctx, LocalEventPushHelper.EventType.BLUETOOTH_DISCONNECT)
        }
        pendingBtDisconnect = task
        mainHandler.postDelayed(task, BT_DEBOUNCE_MS)
    }

    private fun AudioDeviceInfo.isBluetoothAudioOutput(): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            -> true
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                } else {
                    false
                }
            }
        }
    }

    private fun describeBluetooth(devices: Array<out AudioDeviceInfo>): String {
        val bt = devices.filter { it.isBluetoothAudioOutput() }
        if (bt.isEmpty()) return "none"
        return bt.joinToString { "type=${it.type} id=${it.id}" }
    }

    private fun hasBluetoothAudioOutput(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.isBluetoothAudioOutput() }
    }
}
