package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.repository.RestTimerRepository
import com.peter.fitness.service.timer.RestTimerAlarmScheduler
import com.peter.fitness.service.timer.RestTimerServiceController
import javax.inject.Inject

class CancelRestTimerUseCase @Inject constructor(
    private val repository: RestTimerRepository,
    private val controller: RestTimerServiceController,
    private val alarmScheduler: RestTimerAlarmScheduler,
) {
    suspend operator fun invoke() {
        repository.cancel()
        controller.stop()
        alarmScheduler.cancel()
    }
}
