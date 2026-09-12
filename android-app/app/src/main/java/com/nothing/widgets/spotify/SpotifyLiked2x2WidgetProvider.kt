package com.nothing.widgets.spotify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.nothing.widgets.MainActivity
import com.nothing.widgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyLiked2x2WidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SpotifyLiked2x2WidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, SpotifyLiked2x2WidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_spotify_liked_2x2)

        if (!SpotifyAuthManager.isConnected(context)) {
            views.setViewVisibility(R.id.layout_2x2_placeholder, View.VISIBLE)
            views.setViewVisibility(R.id.img_2x2_album_art, View.GONE)
            views.setTextViewText(R.id.txt_2x2_badge, "CONNECT")

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, openPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val track = SpotifyRepository.getMostRecentlyLikedSong(context)
            if (track != null) {
                val spotifyIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(track.spotifyUri)
                    `package` = "com.spotify.music"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, appWidgetId + 2000, spotifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)
                views.setOnClickPendingIntent(R.id.btn_2x2_play, pendingIntent)

                val artBitmap = SpotifyRepository.downloadAlbumArt(track.albumArtUrl)
                if (artBitmap != null) {
                    views.setImageViewBitmap(R.id.img_2x2_album_art, artBitmap)
                    views.setViewVisibility(R.id.img_2x2_album_art, View.VISIBLE)
                    views.setViewVisibility(R.id.layout_2x2_placeholder, View.GONE)
                } else {
                    views.setViewVisibility(R.id.layout_2x2_placeholder, View.VISIBLE)
                    views.setViewVisibility(R.id.img_2x2_album_art, View.GONE)
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
