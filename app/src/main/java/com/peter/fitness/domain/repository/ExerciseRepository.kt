package com.peter.fitness.domain.repository

import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>

    suspend fun findById(id: ExerciseId): Exercise?

    suspend fun count(): Int

    suspend fun upsertAll(exercises: List<Exercise>)
}
