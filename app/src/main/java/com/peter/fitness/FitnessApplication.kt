package com.peter.fitness

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FitnessApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService<NotificationManager>() ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST_TIMER,
                "Rest timer",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Countdown for between-set rest periods"
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BACKUP,
                "Backup",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Drive backup status"
                setShowBadge(false)
            },
        )
    }

    companion object {
        const val CHANNEL_REST_TIMER = "rest_timer"
        const val CHANNEL_BACKUP = "backup"
    }
}
