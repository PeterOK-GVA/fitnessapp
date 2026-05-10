package com.peter.fitness.data.repository

import com.peter.fitness.core.coroutines.IoDispatcher
import com.peter.fitness.data.db.dao.RestTimerDao
import com.peter.fitness.data.db.mapper.toDomain
import com.peter.fitness.data.db.mapper.toEntity
import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.repository.RestTimerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomRestTimerRepository @Inject constructor(
    private val dao: RestTimerDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RestTimerRepository {

    override fun observeActive(): Flow<RestTimer?> =
        dao.observeActive()
            .map { entity -> entity?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun current(): RestTimer? =
        withContext(ioDispatcher) { dao.current()?.toDomain() }

    override suspend fun start(timer: RestTimer) {
        withContext(ioDispatcher) { dao.upsert(timer.toEntity()) }
    }

    override suspend fun cancel() {
        withContext(ioDispatcher) { dao.clear() }
    }
}
