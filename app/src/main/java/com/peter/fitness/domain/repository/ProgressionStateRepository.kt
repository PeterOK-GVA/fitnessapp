package com.peter.fitness.domain.repository

import com.peter.fitness.domain.coach.ProgressionState
import com.peter.fitness.domain.model.ExerciseId

interface ProgressionStateRepository {
    suspend fun find(exerciseId: ExerciseId): ProgressionState?

    suspend fun upsert(exerciseId: ExerciseId, state: ProgressionState)

    suspend fun delete(exerciseId: ExerciseId)
}
