package com.nothing.widgets.spotify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SpotifySyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!SpotifyAuthManager.isConnected(appContext)) {
            return Result.success()
        }

        return try {
            val track = SpotifyRepository.getMostRecentlyLikedSong(appContext)
            if (track != null) {
                SpotifyLikedWidgetProvider.updateAllWidgets(appContext)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
