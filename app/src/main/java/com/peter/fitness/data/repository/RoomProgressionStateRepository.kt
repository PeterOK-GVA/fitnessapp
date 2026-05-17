package com.peter.fitness.data.repository

import com.peter.fitness.core.coroutines.IoDispatcher
import com.peter.fitness.data.db.dao.ProgressionStateDao
import com.peter.fitness.data.db.mapper.toDomain
import com.peter.fitness.data.db.mapper.toEntity
import com.peter.fitness.domain.coach.ProgressionState
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.repository.ProgressionStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

class RoomProgressionStateRepository @Inject constructor(
    private val dao: ProgressionStateDao,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProgressionStateRepository {

    override suspend fun find(exerciseId: ExerciseId): ProgressionState? =
        withContext(ioDispatcher) { dao.find(exerciseId.value)?.toDomain() }

    override suspend fun upsert(exerciseId: ExerciseId, state: ProgressionState) {
        withContext(ioDispatcher) {
            dao.upsert(state.toEntity(exerciseId, clock.instant()))
        }
    }

    override suspend fun delete(exerciseId: ExerciseId) {
        withContext(ioDispatcher) { dao.delete(exerciseId.value) }
    }
}
