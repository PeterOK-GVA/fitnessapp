package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.repository.RestTimerRepository
import com.peter.fitness.service.timer.RestTimerServiceController
import java.time.Clock
import javax.inject.Inject

class StartRestTimerUseCase @Inject constructor(
    private val repository: RestTimerRepository,
    private val controller: RestTimerServiceController,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        durationSeconds: Long,
        sessionId: SessionId? = null,
        setEntryId: SetEntryId? = null,
        label: String? = null,
    ) {
        repository.start(
            RestTimer(
                startedAt = clock.instant(),
                durationSeconds = durationSeconds,
                sessionId = sessionId,
                setEntryId = setEntryId,
                label = label,
            ),
        )
        controller.start()
    }
}
