package com.rapid.prominence.minty.runtime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.second.search.MusicSearchActivity
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.rapid.prominence.minty.second.view.favorite.MintySecFavPage
import com.rapid.prominence.minty.view.AppStart
import com.tencent.mmkv.MMKV

object NotificationToolbarHelper {
    const val NOTIFICATION_ID = 1901
    const val CHANNEL_ID = "toolbar_channel_v3"
    private const val SEARCH = "search"
    private const val LIBRARY = "library"
    private const val FAVORITES = "favorites"
    private const val HOME = "home"
    const val EXTRA_ACTION = "extra_toolbar_action"

    fun build(context: Context): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_toolbar_minty)
            .setColor(context.getColor(R.color.notif_toolbar_icon_bg))
            .setContentTitle(context.getString(R.string.notif_toolbar_brand_title))
            .setContentText(context.getString(R.string.notif_toolbar_hint))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent(context, HOME, 0))
        if (Build.MANUFACTURER.equals("samsung", true) && Build.VERSION.SDK_INT >= 34) {
            return builder
                .addAction(R.drawable.ic_notif_toolbar_search, context.getString(R.string.notif_toolbar_search), pendingIntent(context, SEARCH, 1))
                .addAction(R.drawable.ic_notif_toolbar_playlist, context.getString(R.string.notif_toolbar_playlists), pendingIntent(context, LIBRARY, 2))
                .addAction(R.drawable.ic_notif_toolbar_favorite, context.getString(R.string.notif_toolbar_ai_music), pendingIntent(context, FAVORITES, 3))
        }
        val collapsed = RemoteViews(context.packageName, R.layout.notification_toolbar_collapsed)
        val expanded = RemoteViews(context.packageName, R.layout.notification_toolbar_expanded)
        for (view in listOf(collapsed, expanded)) {
            view.setOnClickPendingIntent(R.id.notif_action_search, pendingIntent(context, SEARCH, 1))
            view.setOnClickPendingIntent(R.id.notif_action_playlists, pendingIntent(context, LIBRARY, 2))
            view.setOnClickPendingIntent(R.id.notif_action_favorites, pendingIntent(context, FAVORITES, 3))
        }
        expanded.setOnClickPendingIntent(R.id.notif_header, pendingIntent(context, HOME, 4))
        return builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed).setCustomBigContentView(expanded)
    }

    private fun pendingIntent(context: Context, action: String, code: Int): PendingIntent {
        val isSecond = runCatching { MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER, false) }.getOrDefault(false)
        val target = when (action) {
            SEARCH -> MusicSearchActivity::class.java
            FAVORITES -> if (isSecond) MintySecFavPage::class.java else EiMainPage::class.java
            LIBRARY -> if (isSecond) MintySecMainPage::class.java else EiMainPage::class.java
            else -> AppStart::class.java
        }
        val intent = Intent(context, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, action)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, code, intent, flags)
    }
}
