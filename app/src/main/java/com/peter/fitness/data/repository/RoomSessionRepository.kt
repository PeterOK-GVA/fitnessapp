package com.peter.fitness.data.repository

import com.peter.fitness.core.coroutines.IoDispatcher
import com.peter.fitness.data.db.dao.SessionDao
import com.peter.fitness.data.db.entity.SessionEntity
import com.peter.fitness.data.db.entity.SetEntryEntity
import com.peter.fitness.data.db.mapper.toDomain
import com.peter.fitness.data.db.mapper.toEntity
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoomSessionRepository @Inject constructor(
    private val dao: SessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SessionRepository {

    override fun observeAll(): Flow<List<Session>> =
        dao.observeAll()
            .map { entities -> entities.map(SessionEntity::toDomain) }
            .flowOn(ioDispatcher)

    override suspend fun findById(id: SessionId): Session? =
        withContext(ioDispatcher) { dao.findById(id.value)?.toDomain() }

    override suspend fun create(session: Session) {
        withContext(ioDispatcher) { dao.insert(session.toEntity()) }
    }

    override suspend fun update(session: Session) {
        withContext(ioDispatcher) { dao.update(session.toEntity()) }
    }

    override suspend fun delete(id: SessionId) {
        withContext(ioDispatcher) { dao.deleteById(id.value) }
    }

    override fun observeSetEntries(sessionId: SessionId): Flow<List<SetEntry>> =
        dao.observeSetEntries(sessionId.value)
            .map { entities -> entities.map(SetEntryEntity::toDomain) }
            .flowOn(ioDispatcher)

    override fun observeAllSets(): Flow<List<SetEntry>> =
        dao.observeAllSets()
            .map { entities -> entities.map(SetEntryEntity::toDomain) }
            .flowOn(ioDispatcher)

    override suspend fun addSetEntry(setEntry: SetEntry) {
        withContext(ioDispatcher) { dao.insertSetEntry(setEntry.toEntity()) }
    }

    override suspend fun updateSetEntry(setEntry: SetEntry) {
        withContext(ioDispatcher) { dao.updateSetEntry(setEntry.toEntity()) }
    }

    override suspend fun deleteSetEntry(id: SetEntryId) {
        withContext(ioDispatcher) { dao.deleteSetEntryById(id.value) }
    }
}
