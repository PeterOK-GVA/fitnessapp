package com.peter.fitness.testsupport

import com.peter.fitness.service.timer.RestTimerAlarmScheduler

class FakeRestTimerAlarmScheduler : RestTimerAlarmScheduler {

    var lastScheduledAtEpochMs: Long? = null
        private set
    var scheduleCount: Int = 0
        private set
    var cancelCount: Int = 0
        private set

    override fun schedule(triggerAtEpochMs: Long) {
        lastScheduledAtEpochMs = triggerAtEpochMs
        scheduleCount += 1
    }

    override fun cancel() {
        cancelCount += 1
    }
}
