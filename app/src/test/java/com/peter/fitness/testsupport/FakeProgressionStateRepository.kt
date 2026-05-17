package com.peter.fitness.testsupport

import com.peter.fitness.domain.coach.ProgressionState
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.repository.ProgressionStateRepository

class FakeProgressionStateRepository(
    initial: Map<ExerciseId, ProgressionState> = emptyMap(),
) : ProgressionStateRepository {

    private val store: MutableMap<ExerciseId, ProgressionState> = initial.toMutableMap()

    var upsertCount: Int = 0
        private set

    override suspend fun find(exerciseId: ExerciseId): ProgressionState? = store[exerciseId]

    override suspend fun upsert(exerciseId: ExerciseId, state: ProgressionState) {
        store[exerciseId] = state
        upsertCount += 1
    }

    override suspend fun delete(exerciseId: ExerciseId) {
        store.remove(exerciseId)
    }
}
