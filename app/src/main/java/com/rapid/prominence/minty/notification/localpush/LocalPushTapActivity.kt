package com.rapid.prominence.minty.notification.localpush

import android.app.Activity
import android.os.Bundle

/** Activity trampoline retains EnMusic's immediate notification click and open funnel. */
class LocalPushTapActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        val router = intent?.getStringExtra(EXTRA_LOCAL_PUSH_ROUTER)
            ?: uri?.getQueryParameter("router").orEmpty()
        val event = intent?.getStringExtra(EXTRA_LOCAL_PUSH_EVENT_TYPE)
            ?: uri?.getQueryParameter("type").orEmpty()
        val notifyId = intent?.getIntExtra(EXTRA_LOCAL_PUSH_NOTIFY_ID, -1)
            ?.takeIf { it >= 0 } ?: uri?.getQueryParameter("nid")?.toIntOrNull() ?: -1
        LocalEventPushHelper.onNotificationClicked(applicationContext, router, event, notifyId)
        if (!LocalEventPushHelper.consumePendingDeepLink(this)) {
            packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        }
        finish()
    }

    companion object {
        const val ACTION_LOCAL_PUSH_TAP = "com.rapid.prominence.minty.action.LOCAL_PUSH_TAP"
        const val EXTRA_LOCAL_PUSH_ROUTER = "local_push_router"
        const val EXTRA_LOCAL_PUSH_EVENT_TYPE = "local_push_event_type"
        const val EXTRA_LOCAL_PUSH_NOTIFY_ID = "local_push_notify_id"
        const val EXTRA_OPEN_PLAYER = "local_push_open_player"
        const val EXTRA_REQUEST_WIDGET_PIN = "local_push_request_widget_pin"
        const val EXTRA_NOTIFY_ID = "extra_notify_id"
        const val EXTRA_ROUTER = "extra_router"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
    }
}
