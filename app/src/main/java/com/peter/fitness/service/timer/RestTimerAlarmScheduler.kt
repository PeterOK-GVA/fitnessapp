package com.peter.fitness.service.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over scheduling/cancelling a one-shot AlarmManager wakeup that acts as a
 * belt-and-braces backup for the foreground rest-timer service. If the service is killed by an
 * aggressive OEM battery manager, the alarm fires anyway and the receiver posts the "Rest over"
 * notification.
 */
interface RestTimerAlarmScheduler {
    fun schedule(triggerAtEpochMs: Long)
    fun cancel()
}

@Singleton
class AndroidRestTimerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : RestTimerAlarmScheduler {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService<AlarmManager>()

    private val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, RestTimerAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun schedule(triggerAtEpochMs: Long) {
        val am = alarmManager ?: return
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntent)
            } else {
                // Fall back to inexact; user has to grant SCHEDULE_EXACT_ALARM in Settings for the
                // tight backup. Inexact will still fire eventually under doze.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntent)
            }
        } catch (securityException: SecurityException) {
            // Exact-alarm permission revoked at runtime; fall back to inexact and surface in logcat
            // so the OEM-killer diagnostic can correlate.
            Log.w(TAG, "Exact alarm denied, falling back to inexact", securityException)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntent)
        }
    }

    override fun cancel() {
        alarmManager?.cancel(pendingIntent)
    }

    private companion object {
        const val REQUEST_CODE = 1001
        const val TAG = "RestTimerAlarm"
    }
}
