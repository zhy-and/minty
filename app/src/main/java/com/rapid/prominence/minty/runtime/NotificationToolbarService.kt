package com.rapid.prominence.minty.runtime

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rapid.prominence.minty.R

/** EnMusic-style quick-access notification, independent of MusicService playback. */
class NotificationToolbarService : Service() {
    private var startRejected = false
    private var promoted = false

    override fun onCreate() {
        super.onCreate()
        if (promote()) MintyRuntime.reportServiceStage(this, "service_on_create", "success")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promote()) return START_NOT_STICKY
        if (!MintyRuntime.notificationPermissionGranted(this) || !RuntimeConfig.isServiceEnabled(this)) {
            MintyRuntime.reportServiceStage(this, "service_stop", "permission_or_switch_off")
            stopSelf()
            return START_NOT_STICKY
        }
        promote(forceRefresh = true)
        MintyRuntime.reportServiceStage(this, "service_on_start_command", "flags=$flags,startId=$startId")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        MintyRuntime.bootstrap(applicationContext, MintyRuntime.Trigger.SERVICE_TASK_REMOVED)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (!startRejected && RuntimeConfig.isServiceEnabled(this)) {
            MintyRuntime.bootstrap(applicationContext, MintyRuntime.Trigger.SERVICE_DESTROY)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promote(forceRefresh: Boolean = false): Boolean {
        if (startRejected) return false
        if (promoted && !forceRefresh) return true
        createChannel()
        val full = runCatching { NotificationToolbarHelper.build(this).build() }
            .getOrElse { Log.w(TAG, "Toolbar build failed; minimal fallback", it); minimalNotification() }
        return try {
            startForeground(NotificationToolbarHelper.NOTIFICATION_ID, full)
            promoted = true
            true
        } catch (e: Exception) {
            if (isStartNotAllowed(e)) return reject("start_foreground_rejected")
            Log.w(TAG, "Toolbar promote failed; minimal fallback", e)
            try {
                startForeground(NotificationToolbarHelper.NOTIFICATION_ID, minimalNotification())
                promoted = true
                true
            } catch (fallback: Exception) {
                reject("start_foreground_failed:${fallback.javaClass.simpleName}")
            }
        }
    }

    private fun reject(detail: String): Boolean {
        startRejected = true
        MintyRuntime.reportServiceStage(this, "start_foreground_rejected", detail)
        MintyRuntime.onForegroundServiceDeferred(applicationContext, detail)
        stopSelf()
        return false
    }

    private fun isStartNotAllowed(error: Exception): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && error is ForegroundServiceStartNotAllowedException

    private fun minimalNotification(): Notification =
        NotificationCompat.Builder(this, NotificationToolbarHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_toolbar_minty)
            .setColor(getColor(R.color.notif_toolbar_icon_bg))
            .setContentTitle(getString(R.string.notif_toolbar_brand_title))
            .setContentText(getString(R.string.notif_toolbar_hint))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.deleteNotificationChannel("toolbar_channel")
            manager.deleteNotificationChannel("toolbar_channel_v2")
            manager.createNotificationChannel(NotificationChannel(
                NotificationToolbarHelper.CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            })
        }.onFailure { Log.w(TAG, "Create toolbar channel failed", it) }
    }

    companion object {
        private const val TAG = "MintyToolbarService"

        /** False means the request was safely deferred to the single recovery Job. */
        fun start(context: Context, hasBackgroundExemption: Boolean = false): Boolean {
            val app = context.applicationContext
            if (!MintyRuntime.notificationPermissionGranted(app)) return false
            if (!ForegroundStartGate.isAllowed(app, hasBackgroundExemption)) {
                MintyRuntime.onForegroundServiceDeferred(app, "process_not_foreground")
                return false
            }
            return try {
                val intent = Intent(app, NotificationToolbarService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(intent)
                else app.startService(intent)
                MintyRuntime.onForegroundServiceLaunchRequested()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Start FGS failed", e)
                MintyRuntime.onForegroundServiceDeferred(app, "start_exception:${e.javaClass.simpleName}")
                false
            }
        }
    }
}
