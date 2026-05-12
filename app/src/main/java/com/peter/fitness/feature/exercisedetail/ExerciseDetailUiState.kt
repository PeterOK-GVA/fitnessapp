package com.peter.fitness.feature.exercisedetail

import com.peter.fitness.domain.analytics.BestE1RM
import com.peter.fitness.domain.analytics.ExerciseHistoryEntry
import com.peter.fitness.domain.analytics.HeaviestSet
import com.peter.fitness.domain.analytics.ProgressPoint
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand

data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val exerciseId: String = "",
    val exerciseName: String = "",
    val movementPattern: MovementPattern = MovementPattern.SQUAT,
    val techniqueDemand: TechniqueDemand = TechniqueDemand.MODERATE,
    val totalSets: Int = 0,
    val heaviestSet: HeaviestSet? = null,
    val bestE1RM: BestE1RM? = null,
    val progress: List<ProgressPoint> = emptyList(),
    val history: List<ExerciseHistoryEntry> = emptyList(),
)
