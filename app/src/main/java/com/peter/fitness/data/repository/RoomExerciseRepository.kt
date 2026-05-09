package com.peter.fitness.data.repository

import com.peter.fitness.core.coroutines.IoDispatcher
import com.peter.fitness.data.db.dao.ExerciseDao
import com.peter.fitness.data.db.entity.ExerciseEntity
import com.peter.fitness.data.db.mapper.toDomain
import com.peter.fitness.data.db.mapper.toEntity
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.repository.ExerciseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ExerciseRepository {

    override fun observeAll(): Flow<List<Exercise>> =
        dao.observeAll()
            .map { entities -> entities.map(ExerciseEntity::toDomain) }
            .flowOn(ioDispatcher)

    override suspend fun findById(id: ExerciseId): Exercise? =
        withContext(ioDispatcher) { dao.findById(id.value)?.toDomain() }

    override suspend fun count(): Int =
        withContext(ioDispatcher) { dao.count() }

    override suspend fun upsertAll(exercises: List<Exercise>) {
        withContext(ioDispatcher) { dao.upsertAll(exercises.map(Exercise::toEntity)) }
    }
}
