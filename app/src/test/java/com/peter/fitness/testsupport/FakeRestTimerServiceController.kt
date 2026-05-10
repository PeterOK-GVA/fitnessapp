package com.peter.fitness.testsupport

import com.peter.fitness.service.timer.RestTimerServiceController

class FakeRestTimerServiceController : RestTimerServiceController {
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set

    override fun start() {
        startCount += 1
    }

    override fun stop() {
        stopCount += 1
    }
}
