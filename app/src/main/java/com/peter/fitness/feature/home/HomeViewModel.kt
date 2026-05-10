package com.peter.fitness.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.core.ids.IdFactory
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.repository.SessionRepository
import com.peter.fitness.domain.usecase.StartRestTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
    private val idFactory: IdFactory,
    private val startRestTimer: StartRestTimerUseCase,
) : ViewModel() {

    private val _events = Channel<HomeEvent>(capacity = Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun onStartWorkout() {
        viewModelScope.launch {
            val newId = idFactory.newSessionId()
            sessionRepository.create(
                Session(
                    id = newId,
                    startedAt = clock.instant(),
                    endedAt = null,
                    focus = SessionFocus.FREE_LOG,
                    notes = null,
                ),
            )
            _events.send(HomeEvent.SessionStarted(newId.value))
        }
    }

    fun onStartTestRest() {
        viewModelScope.launch {
            startRestTimer(durationSeconds = TEST_REST_SECONDS, label = "Test rest")
        }
    }

    private companion object {
        const val TEST_REST_SECONDS = 60L
    }
}

sealed interface HomeEvent {
    data class SessionStarted(val sessionId: String) : HomeEvent
}
