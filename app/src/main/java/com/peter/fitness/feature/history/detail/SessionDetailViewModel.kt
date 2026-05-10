package com.peter.fitness.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val sessionId: SessionId = SessionId(
        requireNotNull(savedStateHandle.get<String>(KEY_SESSION_ID)) {
            "Missing $KEY_SESSION_ID in SavedStateHandle"
        },
    )

    private val _uiState = MutableStateFlow(SessionDetailUiState(sessionId = sessionId.value))
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<SessionDetailEvent>(capacity = Channel.BUFFERED)
    val events: Flow<SessionDetailEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.findById(sessionId)
            if (session == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    focus = session.focus,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                )
            }
            sessionRepository.observeSetEntries(sessionId).collect { entries ->
                refreshSets(entries)
            }
        }
    }

    private suspend fun refreshSets(entries: List<SetEntry>) {
        val byExercise = entries
            .map { it.exerciseId }
            .distinct()
            .associateWith { id: ExerciseId ->
                exerciseRepository.findById(id)?.name ?: "Exercise"
            }
        val sets = entries.map { entry ->
            val reps = entry.completedReps ?: entry.targetReps
            val load = entry.performedLoadKg ?: entry.targetLoadKg
            DetailSetUi(
                id = entry.id.value,
                ordinal = entry.ordinal,
                exerciseName = byExercise[entry.exerciseId] ?: "Exercise",
                reps = reps,
                loadKg = load,
                subjectiveLoad = entry.subjectiveLoad,
                techniqueRating = entry.techniqueRating,
            )
        }
        val totalVolume = sets.sumOf { it.reps * it.loadKg }
        _uiState.update { it.copy(sets = sets, totalVolumeKg = totalVolume) }
    }

    fun onDelete() {
        if (_uiState.value.isDeleting || _uiState.value.isDeleted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            sessionRepository.delete(sessionId)
            _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
            _events.send(SessionDetailEvent.Deleted)
        }
    }

    private companion object {
        const val KEY_SESSION_ID = "sessionId"
    }
}
