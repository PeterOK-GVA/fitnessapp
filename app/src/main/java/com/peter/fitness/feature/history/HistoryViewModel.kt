package com.peter.fitness.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        combine(
            sessionRepository.observeAll(),
            sessionRepository.observeAllSets(),
        ) { sessions, sets -> buildState(sessions, sets) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = HistoryUiState(),
            )

    private fun buildState(sessions: List<Session>, sets: List<SetEntry>): HistoryUiState {
        val countsBySession = sets.groupingBy { it.sessionId.value }.eachCount()
        return HistoryUiState(
            isLoading = false,
            sessions = sessions
                .sortedByDescending { it.startedAt }
                .map { session ->
                    HistorySessionUi(
                        id = session.id.value,
                        focus = session.focus,
                        startedAt = session.startedAt,
                        endedAt = session.endedAt,
                        setCount = countsBySession[session.id.value] ?: 0,
                    )
                },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
