package com.peter.fitness.service.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over starting/stopping the foreground rest-timer service so use cases stay
 * Android-free and unit-testable.
 */
interface RestTimerServiceController {
    fun start()
    fun stop()
}

@Singleton
class AndroidRestTimerServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) : RestTimerServiceController {

    override fun start() {
        val intent = Intent(context, RestTimerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        context.stopService(Intent(context, RestTimerService::class.java))
    }
}
