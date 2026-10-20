package com.rapid.prominence.minty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.cons.MINTY_APP_IS_M_USER
import com.rapid.prominence.minty.ei.main.EiMainPage
import com.rapid.prominence.minty.second.view.MintySecMainPage
import com.tencent.mmkv.MMKV

/** Mirrors EnMusic's 3x2 widget: every action opens the music player. */
class MusicPlayerAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, manager, it) }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.rapid.prominence.minty.widget.OPEN_PLAYER"
        private const val PROGRESS_MAX = 1000

        @Volatile
        private var playback = PlaybackSnapshot()

        /** Called when the MusicService track, playback state, or position changes. */
        fun updatePlayback(
            context: Context,
            title: String?,
            artist: String?,
            durationMs: Long,
            positionMs: Long,
            isPlaying: Boolean,
        ) {
            playback = PlaybackSnapshot(title, artist, durationMs, positionMs, isPlaying)
            refreshAll(context)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MusicPlayerAppWidgetProvider::class.java),
            )
            ids.forEach { updateAppWidget(context, manager, it) }
        }

        private fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val state = playback
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)
            views.setTextViewText(
                R.id.widgetSongTitle,
                state.title?.takeIf(String::isNotBlank) ?: context.getString(R.string.widget_empty_title),
            )
            views.setTextViewText(
                R.id.widgetSongArtist,
                state.artist?.takeIf(String::isNotBlank) ?: context.getString(R.string.widget_empty_artist),
            )
            val progress = if (state.durationMs > 0) {
                ((state.positionMs.coerceAtLeast(0).toDouble() / state.durationMs) * PROGRESS_MAX)
                    .toInt().coerceIn(0, PROGRESS_MAX)
            } else {
                0
            }
            views.setProgressBar(R.id.widgetProgress, PROGRESS_MAX, progress, false)
            views.setImageViewResource(
                R.id.widgetPlayPause,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )

            val openPlayer = openPlayerPendingIntent(context, id)
            intArrayOf(
                R.id.widgetRoot,
                R.id.widgetSongTitle,
                R.id.widgetSongArtist,
                R.id.widgetProgressArea,
                R.id.widgetFavorite,
                R.id.widgetPrev,
                R.id.widgetPlayPause,
                R.id.widgetNext,
                R.id.widgetQueue,
            ).forEach { views.setOnClickPendingIntent(it, openPlayer) }
            manager.updateAppWidget(id, views)
        }

        private fun openPlayerPendingIntent(context: Context, id: Int): PendingIntent {
            val isSecondary = MMKV.defaultMMKV().getBoolean(MINTY_APP_IS_M_USER, false)
            val target = if (isSecondary) MintySecMainPage::class.java else EiMainPage::class.java
            val intent = Intent(context, target).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_PLAYER, true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(context, 20_000 + id, intent, flags)
        }
    }

    private data class PlaybackSnapshot(
        val title: String? = null,
        val artist: String? = null,
        val durationMs: Long = 0,
        val positionMs: Long = 0,
        val isPlaying: Boolean = false,
    )
}
