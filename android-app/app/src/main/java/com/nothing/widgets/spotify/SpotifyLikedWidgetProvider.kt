package com.nothing.widgets.spotify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.nothing.widgets.MainActivity
import com.nothing.widgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyLikedWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_SPOTIFY = "com.nothing.widgets.ACTION_REFRESH_SPOTIFY"
        const val ACTION_PLAY_TRACK = "com.nothing.widgets.ACTION_PLAY_TRACK"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SpotifyLikedWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, SpotifyLikedWidgetProvider::class.java).apply {
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_SPOTIFY) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SpotifyLikedWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, showSyncing = true)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        showSyncing: Boolean = false
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_spotify_liked)

        // Setup Refresh intent
        val refreshIntent = Intent(context, SpotifyLikedWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_SPOTIFY
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        // If not logged in yet
        if (!SpotifyAuthManager.isConnected(context)) {
            views.setTextViewText(R.id.txt_status_tag, "[AUTH // REQUIRED]")
            views.setTextViewText(R.id.txt_song_title, "CONNECT SPOTIFY")
            views.setTextViewText(R.id.txt_artist_album, "Tap to open studio & authenticate")
            views.setViewVisibility(R.id.img_album_art, View.GONE)
            views.setViewVisibility(R.id.art_placeholder_text, View.VISIBLE)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPending)
            views.setOnClickPendingIntent(R.id.btn_play_action, openAppPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        if (showSyncing) {
            views.setTextViewText(R.id.txt_status_tag, "[SYNCING...]")
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        // Fetch track in CoroutineScope
        CoroutineScope(Dispatchers.Main).launch {
            val track = SpotifyRepository.getMostRecentlyLikedSong(context)
            if (track != null) {
                views.setTextViewText(R.id.txt_status_tag, "[LIKED • ${track.addedAtFormatted}]")
                views.setTextViewText(R.id.txt_song_title, track.title.uppercase())
                views.setTextViewText(R.id.txt_artist_album, "${track.artist.uppercase()} // ${track.album.uppercase()}")

                // Deep link intent to Spotify
                val spotifyIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(track.spotifyUri)
                    `package` = "com.spotify.music"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val spotifyPendingIntent = PendingIntent.getActivity(
                    context, appWidgetId + 1000, spotifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_play_action, spotifyPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_container, spotifyPendingIntent)

                // Download & render album art
                val artBitmap = SpotifyRepository.downloadAlbumArt(track.albumArtUrl)
                if (artBitmap != null) {
                    views.setImageViewBitmap(R.id.img_album_art, artBitmap)
                    views.setViewVisibility(R.id.img_album_art, View.VISIBLE)
                    views.setViewVisibility(R.id.art_placeholder_text, View.GONE)
                } else {
                    views.setViewVisibility(R.id.img_album_art, View.GONE)
                    views.setViewVisibility(R.id.art_placeholder_text, View.VISIBLE)
                }
            } else {
                views.setTextViewText(R.id.txt_status_tag, "[NO TRACKS YET]")
                views.setTextViewText(R.id.txt_song_title, "LIKE A SONG")
                views.setTextViewText(R.id.txt_artist_album, "Tap heart on any Spotify track")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
