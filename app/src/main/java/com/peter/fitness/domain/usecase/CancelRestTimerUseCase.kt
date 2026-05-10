package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.repository.RestTimerRepository
import com.peter.fitness.service.timer.RestTimerServiceController
import javax.inject.Inject

class CancelRestTimerUseCase @Inject constructor(
    private val repository: RestTimerRepository,
    private val controller: RestTimerServiceController,
) {
    suspend operator fun invoke() {
        repository.cancel()
        controller.stop()
    }
}
