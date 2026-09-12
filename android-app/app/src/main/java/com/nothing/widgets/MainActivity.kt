package com.nothing.widgets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nothing.widgets.databinding.ActivityMainBinding
import com.nothing.widgets.spotify.SpotifyAuthManager
import com.nothing.widgets.spotify.SpotifyLiked2x2WidgetProvider
import com.nothing.widgets.spotify.SpotifyLikedWidgetProvider
import com.nothing.widgets.spotify.SpotifyRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun setupUI() {
        binding.btnConnectSpotify.setOnClickListener {
            if (SpotifyAuthManager.isConnected(this)) {
                SpotifyAuthManager.disconnect(this)
                updateAuthStatus()
                SpotifyLikedWidgetProvider.updateAllWidgets(this)
                SpotifyLiked2x2WidgetProvider.updateAllWidgets(this)
                Toast.makeText(this, "Disconnected from Spotify", Toast.LENGTH_SHORT).show()
            } else {
                SpotifyAuthManager.startSpotifyLogin(this)
            }
        }

        binding.btnRefreshWidgets.setOnClickListener {
            SpotifyLikedWidgetProvider.updateAllWidgets(this)
            SpotifyLiked2x2WidgetProvider.updateAllWidgets(this)
            loadCurrentTrackPreview()
            Toast.makeText(this, "Triggered widget refresh", Toast.LENGTH_SHORT).show()
        }

        updateAuthStatus()
        loadCurrentTrackPreview()
    }

    private fun handleIncomingIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "nothingwidgets" && uri.host == "spotify-callback") {
            lifecycleScope.launch {
                binding.authStatusProgress.visibility = View.VISIBLE
                val success = SpotifyAuthManager.handleAuthCallback(this@MainActivity, uri)
                binding.authStatusProgress.visibility = View.GONE
                if (success) {
                    Toast.makeText(this@MainActivity, "Connected to Spotify!", Toast.LENGTH_LONG).show()
                    updateAuthStatus()
                    loadCurrentTrackPreview()
                    SpotifyLikedWidgetProvider.updateAllWidgets(this@MainActivity)
                    SpotifyLiked2x2WidgetProvider.updateAllWidgets(this@MainActivity)
                } else {
                    Toast.makeText(this@MainActivity, "Spotify login failed or canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateAuthStatus() {
        val connected = SpotifyAuthManager.isConnected(this)
        if (connected) {
            val user = SpotifyAuthManager.getStoredUserName(this)
            binding.txtSpotifyStatus.text = "ACTIVE // USER: $user"
            binding.txtSpotifyStatus.setTextColor(getColor(R.color.nothing_red))
            binding.btnConnectSpotify.text = "DISCONNECT SPOTIFY"
        } else {
            binding.txtSpotifyStatus.text = "READY TO CONNECT"
            binding.txtSpotifyStatus.setTextColor(getColor(R.color.black))
            binding.btnConnectSpotify.text = "CONNECT SPOTIFY"
        }
    }

    private fun loadCurrentTrackPreview() {
        lifecycleScope.launch {
            val track = SpotifyRepository.getMostRecentlyLikedSong(this@MainActivity)
            if (track != null) {
                binding.previewTitle.text = track.title.uppercase()
                binding.previewArtist.text = "${track.artist.uppercase()} // ${track.album.uppercase()}"
                binding.previewTag.text = "[LIKED • ${track.addedAtFormatted}]"
            }
        }
    }
}
