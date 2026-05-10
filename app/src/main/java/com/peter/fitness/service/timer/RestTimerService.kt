package com.peter.fitness.service.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.repository.RestTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject lateinit var repository: RestTimerRepository

    @Inject lateinit var clock: Clock

    private val scope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
    private var observerJob: Job? = null
    private var tickJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            RestTimerNotifications.ONGOING_NOTIFICATION_ID,
            placeholderNotification().build(),
        )
        if (observerJob == null) {
            observerJob = scope.launch { observeRepository() }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        tickJob?.cancel()
        observerJob?.cancel()
        releaseWakeLock()
        scope.cancel()
    }

    private suspend fun observeRepository() {
        repository.observeActive()
            .distinctUntilChanged()
            .collect { timer ->
                tickJob?.cancel()
                if (timer == null) {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }
                acquireWakeLock(timer)
                tickJob = scope.launch { tick(timer) }
            }
    }

    private suspend fun tick(timer: RestTimer) {
        while (true) {
            val now = clock.instant()
            if (timer.isExpired(now)) {
                onTimerExpired(timer)
                return
            }
            updateOngoing(timer, now)
            delay(TICK_INTERVAL_MS)
        }
    }

    private suspend fun onTimerExpired(timer: RestTimer) {
        RestTimerNotifications.postExpired(this, timer)
        repository.cancel()
    }

    private fun updateOngoing(timer: RestTimer, now: Instant) {
        val builder = RestTimerNotifications.ongoing(this, timer, now)
        getSystemService<android.app.NotificationManager>()
            ?.notify(RestTimerNotifications.ONGOING_NOTIFICATION_ID, builder.build())
    }

    private fun placeholderNotification(): NotificationCompat.Builder =
        RestTimerNotifications.ongoing(
            this,
            RestTimer(
                startedAt = clock.instant(),
                durationSeconds = 1,
                label = "Starting…",
            ),
            clock.instant(),
        )

    private fun acquireWakeLock(timer: RestTimer) {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService<PowerManager>() ?: return
        val timeoutMs = timer.durationSeconds * MILLIS_PER_SECOND + WAKE_LOCK_BUFFER_MS
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(timeoutMs)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
        const val WAKE_LOCK_BUFFER_MS = 5_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val WAKE_LOCK_TAG = "fitness:rest_timer"
    }
}
