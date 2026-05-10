package com.peter.fitness.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<MovementPattern?>(null)

    val uiState: StateFlow<ExerciseListUiState> =
        combine(
            exerciseRepository.observeAll(),
            selectedFilter,
        ) { exercises, filter -> exercises.toUiState(filter) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ExerciseListUiState(),
            )

    fun onFilterSelected(pattern: MovementPattern?) {
        selectedFilter.value = pattern
    }

    private fun List<Exercise>.toUiState(filter: MovementPattern?): ExerciseListUiState {
        val items = map { it.toListItem() }
        val filtered = if (filter == null) items else items.filter { it.movementPattern == filter }
        val availableFilters = items
            .map { it.movementPattern }
            .distinct()
            .sortedBy { it.ordinal }
        return ExerciseListUiState(
            isLoading = false,
            exercises = filtered.sortedBy { it.name },
            availableFilters = availableFilters,
            selectedFilter = filter,
        )
    }

    private fun Exercise.toListItem(): ExerciseListItemUi = ExerciseListItemUi(
        id = id,
        name = name,
        movementPattern = movementPattern,
        techniqueDemand = techniqueDemand,
        requiresRack = requiresRack,
        requiresBench = requiresBench,
        requiresPullUpBar = requiresPullUpBar,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
