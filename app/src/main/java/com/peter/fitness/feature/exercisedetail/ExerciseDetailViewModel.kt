package com.peter.fitness.feature.exercisedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.analytics.BestE1RM
import com.peter.fitness.domain.analytics.ExerciseProgressCalculator
import com.peter.fitness.domain.analytics.HeaviestSet
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val exerciseId: ExerciseId = ExerciseId(
        requireNotNull(savedStateHandle.get<String>(KEY_EXERCISE_ID)) {
            "Missing $KEY_EXERCISE_ID in SavedStateHandle"
        },
    )

    private val _uiState = MutableStateFlow(ExerciseDetailUiState(exerciseId = exerciseId.value))
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.findById(exerciseId)
            if (exercise == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            observeAnalytics(exercise)
        }
    }

    private suspend fun observeAnalytics(exercise: Exercise) {
        combine(
            sessionRepository.observeAll(),
            sessionRepository.observeAllSets(),
        ) { sessions, sets -> buildState(exercise, sessions, sets) }.collect { state ->
            _uiState.value = state
        }
    }

    private fun buildState(
        exercise: Exercise,
        sessions: List<Session>,
        sets: List<SetEntry>,
    ): ExerciseDetailUiState {
        val progress = ExerciseProgressCalculator.progressFor(exercise.id, sessions, sets)
        val history = ExerciseProgressCalculator.historyFor(exercise.id, sessions, sets)
        val heaviestSet = history.maxByOrNull { it.loadKg }?.let { entry ->
            HeaviestSet(
                loadKg = entry.loadKg,
                reps = entry.reps,
                performedAt = entry.performedAt,
            )
        }
        val bestE1RM = history.maxByOrNull { it.estimatedE1RM }?.let { entry ->
            BestE1RM(
                estimatedKg = entry.estimatedE1RM,
                sourceLoadKg = entry.loadKg,
                sourceReps = entry.reps,
                performedAt = entry.performedAt,
            )
        }
        return ExerciseDetailUiState(
            isLoading = false,
            exerciseId = exercise.id.value,
            exerciseName = exercise.name,
            movementPattern = exercise.movementPattern,
            techniqueDemand = exercise.techniqueDemand,
            totalSets = history.size,
            heaviestSet = heaviestSet,
            bestE1RM = bestE1RM,
            progress = progress,
            history = history,
        )
    }

    private companion object {
        const val KEY_EXERCISE_ID = "exerciseId"
    }
}
