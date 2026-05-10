package com.peter.fitness.feature.exercises

import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand

data class ExerciseListUiState(
    val isLoading: Boolean = true,
    val exercises: List<ExerciseListItemUi> = emptyList(),
    val availableFilters: List<MovementPattern> = emptyList(),
    val selectedFilter: MovementPattern? = null,
)

data class ExerciseListItemUi(
    val id: ExerciseId,
    val name: String,
    val movementPattern: MovementPattern,
    val techniqueDemand: TechniqueDemand,
    val requiresRack: Boolean,
    val requiresBench: Boolean,
    val requiresPullUpBar: Boolean,
)
