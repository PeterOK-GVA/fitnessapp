package com.peter.fitness.service.timer

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.peter.fitness.FitnessApplication
import com.peter.fitness.domain.model.RestTimer
import java.time.Instant

internal object RestTimerNotifications {

    const val ONGOING_NOTIFICATION_ID = 1001
    const val EXPIRED_NOTIFICATION_ID = 1002

    fun ongoing(context: Context, timer: RestTimer, now: Instant): NotificationCompat.Builder {
        val remaining = timer.remainingSeconds(now)
        return NotificationCompat.Builder(context, FitnessApplication.CHANNEL_REST_TIMER)
            .setContentTitle(timer.label ?: "Rest")
            .setContentText(formatRemaining(remaining))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setUsesChronometer(false)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
    }

    fun expired(context: Context, timer: RestTimer): NotificationCompat.Builder =
        NotificationCompat.Builder(context, FitnessApplication.CHANNEL_REST_TIMER)
            .setContentTitle("Rest over")
            .setContentText(timer.label ?: "Time's up")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

    fun postExpired(context: Context, timer: RestTimer) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.notify(EXPIRED_NOTIFICATION_ID, expired(context, timer).build())
    }

    private fun formatRemaining(seconds: Long): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE
        return "%d:%02d".format(minutes, secs)
    }

    private const val SECONDS_PER_MINUTE = 60L
}
