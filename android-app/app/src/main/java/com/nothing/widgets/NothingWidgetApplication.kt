package com.nothing.widgets

import android.app.Application
import androidx.work.*
import com.nothing.widgets.spotify.SpotifySyncWorker
import java.util.concurrent.TimeUnit

class NothingWidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupPeriodicWidgetSync()
    }

    private fun setupPeriodicWidgetSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SpotifySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SpotifyLikedWidgetSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
