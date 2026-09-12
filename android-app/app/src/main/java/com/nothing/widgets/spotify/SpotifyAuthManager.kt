package com.nothing.widgets.spotify

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

object SpotifyAuthManager {
    private const val PREFS_NAME = "spotify_auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_CODE_VERIFIER = "code_verifier"
    private const val KEY_USER_NAME = "user_name"

    // Default Client ID for widget setup (or user's own Spotify Developer Client ID)
    var clientId: String = "897c834cb2c24484bd79ce42533c38be" // Can be customized in Settings
    const val REDIRECT_URI = "nothingwidgets://spotify-callback"
    private const val SCOPES = "user-library-read user-read-currently-playing user-read-playback-state"

    private val httpClient = OkHttpClient()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isConnected(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_REFRESH_TOKEN, null) != null
    }

    fun getStoredUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "CONNECTED") ?: "CONNECTED"
    }

    fun disconnect(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    /**
     * Generate PKCE Code Verifier & Challenge (No Client Secret required on phone)
     */
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Start the Spotify Login Flow via Chrome Custom Tabs
     */
    fun startSpotifyLogin(context: Context) {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)

        getPrefs(context).edit()
            .putString(KEY_CODE_VERIFIER, verifier)
            .apply()

        val authUri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, authUri)
    }

    /**
     * Handle the redirect URI callback after user logs in
     */
    suspend fun handleAuthCallback(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val code = uri.getQueryParameter("code") ?: return@withContext false
        val verifier = getPrefs(context).getString(KEY_CODE_VERIFIER, null) ?: return@withContext false

        val formBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", verifier)
            .build()

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(formBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.getInt("expires_in")
                val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

                getPrefs(context).edit()
                    .putString(KEY_ACCESS_TOKEN, accessToken)
                    .putString(KEY_REFRESH_TOKEN, refreshToken)
                    .putLong(KEY_EXPIRES_AT, expiresAt)
                    .remove(KEY_CODE_VERIFIER)
                    .apply()

                // Fetch Profile name
                fetchUserProfile(context, accessToken)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private suspend fun fetchUserProfile(context: Context, token: String) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .addHeader("Authorization", "Bearer $token")
                .build()
            val res = httpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val json = JSONObject(res.body?.string() ?: "")
                val displayName = json.optString("display_name", "USER")
                getPrefs(context).edit().putString(KEY_USER_NAME, displayName).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieve a valid access token, auto-refreshing if expired
     */
    suspend fun getValidAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return@withContext null

        // If current token is still valid (with 60s buffer), return it
        val currentToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (currentToken != null && System.currentTimeMillis() < (expiresAt - 60_000)) {
            return@withContext currentToken
        }

        // Refresh token
        val formBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(formBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val newAccessToken = json.getString("access_token")
                val expiresIn = json.getInt("expires_in")
                val newExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
                val newRefreshToken = json.optString("refresh_token", refreshToken)

                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, newAccessToken)
                    .putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    .putLong(KEY_EXPIRES_AT, newExpiresAt)
                    .apply()

                return@withContext newAccessToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
