package com.rapid.prominence.minty.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AppWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_PIN_SUCCESS) return
        Log.i("AppWidgetPin", "Widget pinned from ${intent.getStringExtra(WidgetPinHelper.EXTRA_FROM_KEY).orEmpty()}")
        MusicPlayerAppWidgetProvider.refreshAll(context)
    }

    companion object {
        const val ACTION_PIN_SUCCESS = "com.rapid.prominence.minty.action.WIDGET_PIN_SUCCESS"
    }
}
