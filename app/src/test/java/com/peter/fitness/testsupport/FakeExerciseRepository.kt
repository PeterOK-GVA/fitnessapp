package com.peter.fitness.testsupport

import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeExerciseRepository(
    initial: List<Exercise> = emptyList(),
) : ExerciseRepository {

    private val _exercises = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Exercise>> = _exercises.asStateFlow()

    override suspend fun findById(id: ExerciseId): Exercise? =
        _exercises.value.firstOrNull { it.id == id }

    override suspend fun count(): Int = _exercises.value.size

    override suspend fun upsertAll(exercises: List<Exercise>) {
        val byId = (_exercises.value.associateBy { it.id } + exercises.associateBy { it.id }).values
        _exercises.value = byId.toList()
    }
}
