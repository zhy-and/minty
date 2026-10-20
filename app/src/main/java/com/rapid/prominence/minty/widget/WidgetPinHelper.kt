package com.rapid.prominence.minty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.rapid.prominence.minty.R

object WidgetPinHelper {
    const val EXTRA_FROM_KEY = "from_key"

    fun hasPinnedWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, MusicPlayerAppWidgetProvider::class.java),
        )
        return ids.isNotEmpty()
    }

    fun canRequestPin(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context)
        return manager.isRequestPinAppWidgetSupported && !hasPinnedWidget(context)
    }

    fun requestPin(context: Context, fromKey: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_SHORT).show()
            return false
        }
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_SHORT).show()
            return false
        }
        if (hasPinnedWidget(context)) {
            Toast.makeText(context, R.string.widget_already_added, Toast.LENGTH_SHORT).show()
            return false
        }
        val callbackIntent = Intent(context, AppWidgetPinReceiver::class.java).apply {
            action = AppWidgetPinReceiver.ACTION_PIN_SUCCESS
            putExtra(EXTRA_FROM_KEY, fromKey)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val callback = PendingIntent.getBroadcast(context, fromKey.hashCode(), callbackIntent, flags)
        return manager.requestPinAppWidget(
            ComponentName(context, MusicPlayerAppWidgetProvider::class.java),
            null,
            callback,
        )
    }
}
