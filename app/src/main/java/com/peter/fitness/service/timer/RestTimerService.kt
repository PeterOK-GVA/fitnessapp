package com.peter.fitness.service.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RestTimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }
}
