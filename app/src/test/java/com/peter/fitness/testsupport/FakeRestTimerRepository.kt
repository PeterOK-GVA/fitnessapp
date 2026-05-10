package com.peter.fitness.testsupport

import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.repository.RestTimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRestTimerRepository(initial: RestTimer? = null) : RestTimerRepository {

    private val _state = MutableStateFlow(initial)

    var startCount: Int = 0
        private set
    var cancelCount: Int = 0
        private set

    override fun observeActive(): Flow<RestTimer?> = _state.asStateFlow()

    override suspend fun current(): RestTimer? = _state.value

    override suspend fun start(timer: RestTimer) {
        _state.value = timer
        startCount += 1
    }

    override suspend fun cancel() {
        _state.value = null
        cancelCount += 1
    }
}
