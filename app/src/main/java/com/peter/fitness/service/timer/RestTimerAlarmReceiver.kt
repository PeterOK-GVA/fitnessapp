package com.peter.fitness.service.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.peter.fitness.domain.repository.RestTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when the backup AlarmManager goes off. If the foreground service already handled expiry
 * the active timer in Room is null and the receiver no-ops; otherwise this is the safety net that
 * posts the "Rest over" notification.
 */
@AndroidEntryPoint
class RestTimerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: RestTimerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val active = repository.current() ?: return@launch
                RestTimerNotifications.postExpired(context.applicationContext, active)
                repository.cancel()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
