package com.peter.fitness.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.RestTimerRepository
import com.peter.fitness.domain.repository.SessionRepository
import com.peter.fitness.domain.usecase.CancelRestTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val restTimerRepository: RestTimerRepository,
    private val cancelRestTimer: CancelRestTimerUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val sessionId: SessionId = SessionId(
        requireNotNull(savedStateHandle.get<String>(SESSION_ID_KEY)) {
            "Missing $SESSION_ID_KEY in SavedStateHandle"
        },
    )

    private val _uiState = MutableStateFlow(ActiveSessionUiState(sessionId = sessionId.value))
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.findById(sessionId)
            if (session == null) {
                _uiState.update { it.copy(isLoading = false, sessionMissing = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    focus = session.focus,
                )
            }
            observeSets()
        }
        observeRestTimer()
    }

    private fun observeRestTimer() {
        viewModelScope.launch {
            restTimerRepository.observeActive().collect { timer ->
                _uiState.update { it.copy(activeRestTimer = timer) }
            }
        }
    }

    fun onCancelRestTimer() {
        viewModelScope.launch { cancelRestTimer() }
    }

    private fun observeSets() {
        viewModelScope.launch {
            sessionRepository.observeSetEntries(sessionId).collect { entries ->
                val byExercise = entries
                    .map { it.exerciseId }
                    .distinct()
                    .associateWith { id -> exerciseRepository.findById(id)?.name ?: "Exercise" }
                _uiState.update { current ->
                    current.copy(
                        sets = entries.map { entry ->
                            SetEntryUi(
                                id = entry.id.value,
                                ordinal = entry.ordinal,
                                exerciseName = byExercise[entry.exerciseId] ?: "Exercise",
                                targetReps = entry.targetReps,
                                targetLoadKg = entry.targetLoadKg,
                                completedReps = entry.completedReps,
                                performedLoadKg = entry.performedLoadKg,
                            )
                        },
                    )
                }
            }
        }
    }

    private companion object {
        // Must match the property name on ActiveSessionDestination so Compose Navigation
        // populates the SavedStateHandle under this key.
        const val SESSION_ID_KEY = "sessionId"
    }

    fun onFinishSession() {
        val current = _uiState.value
        if (current.isFinishing || current.isFinished || current.startedAt == null) return
        // Atomically flip to isFinishing so a second synchronous call returns early.
        if (!_uiState.compareAndSet(current, current.copy(isFinishing = true))) return
        viewModelScope.launch {
            val session = sessionRepository.findById(sessionId) ?: run {
                _uiState.update { it.copy(isFinishing = false, sessionMissing = true) }
                return@launch
            }
            val finishedAt = clock.instant()
            sessionRepository.update(session.copy(endedAt = finishedAt))
            _uiState.update {
                it.copy(
                    isFinishing = false,
                    isFinished = true,
                    endedAt = finishedAt,
                )
            }
        }
    }
}
