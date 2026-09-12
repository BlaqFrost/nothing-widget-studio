package com.nothing.widgets.spotify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LikedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUrl: String?,
    val spotifyUri: String,
    val addedAtFormatted: String,
    val durationMs: Long
)

object SpotifyRepository {
    private val httpClient = OkHttpClient()
    private const val PREFS_CACHE = "spotify_widget_cache"

    /**
     * Fetches the user's single most recently liked song from Spotify
     */
    suspend fun getMostRecentlyLikedSong(context: Context): LikedTrack? = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuthManager.getValidAccessToken(context) ?: return@withContext getCachedTrack(context)

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/tracks?limit=1")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return@withContext null
                val root = JSONObject(jsonString)
                val items = root.getJSONArray("items")
                if (items.length() > 0) {
                    val item = items.getJSONObject(0)
                    val addedAt = item.getString("added_at") // ISO 8601 string
                    val trackObj = item.getJSONObject("track")

                    val trackId = trackObj.getString("id")
                    val title = trackObj.getString("name")
                    val durationMs = trackObj.getLong("duration_ms")
                    val uri = trackObj.getString("uri")

                    // Artists string
                    val artistsArray = trackObj.getJSONArray("artists")
                    val artistNames = mutableListOf<String>()
                    for (i in 0 until artistsArray.length()) {
                        artistNames.add(artistsArray.getJSONObject(i).getString("name"))
                    }
                    val artistsStr = artistNames.joinToString(", ")

                    // Album and Art
                    val albumObj = trackObj.getJSONObject("album")
                    val albumName = albumObj.getString("name")
                    val imagesArray = albumObj.getJSONArray("images")
                    val artUrl = if (imagesArray.length() > 0) imagesArray.getJSONObject(0).getString("url") else null

                    // Format addedAt date to "MMM dd, HH:mm"
                    val formattedDate = formatAddedDate(addedAt)

                    val track = LikedTrack(
                        id = trackId,
                        title = title,
                        artist = artistsStr,
                        album = albumName,
                        albumArtUrl = artUrl,
                        spotifyUri = uri,
                        addedAtFormatted = formattedDate,
                        durationMs = durationMs
                    )

                    cacheTrack(context, track)
                    return@withContext track
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext getCachedTrack(context)
    }

    private fun formatAddedDate(isoString: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = isoFormat.parse(isoString) ?: Date()
            val targetFormat = SimpleDateFormat("MM.dd // HH:mm", Locale.US)
            targetFormat.format(date)
        } catch (e: Exception) {
            "RECENT"
        }
    }

    private fun cacheTrack(context: Context, track: LikedTrack) {
        val prefs = context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("title", track.title)
            .putString("artist", track.artist)
            .putString("album", track.album)
            .putString("artUrl", track.albumArtUrl)
            .putString("uri", track.spotifyUri)
            .putString("addedAt", track.addedAtFormatted)
            .apply()
    }

    fun getCachedTrack(context: Context): LikedTrack? {
        val prefs = context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
        val title = prefs.getString("title", null) ?: return null
        return LikedTrack(
            id = "cached",
            title = title,
            artist = prefs.getString("artist", "UNKNOWN ARTIST") ?: "",
            album = prefs.getString("album", "") ?: "",
            albumArtUrl = prefs.getString("artUrl", null),
            spotifyUri = prefs.getString("uri", "spotify:app") ?: "",
            addedAtFormatted = prefs.getString("addedAt", "RECENT") ?: "RECENT",
            durationMs = 0L
        )
    }

    suspend fun downloadAlbumArt(urlStr: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (urlStr.isNullOrEmpty()) return@withContext null
        try {
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            return@withContext BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
