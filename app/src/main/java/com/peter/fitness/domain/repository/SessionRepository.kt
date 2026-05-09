package com.peter.fitness.domain.repository

import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAll(): Flow<List<Session>>

    suspend fun findById(id: SessionId): Session?

    suspend fun create(session: Session)

    suspend fun update(session: Session)

    suspend fun delete(id: SessionId)

    fun observeSetEntries(sessionId: SessionId): Flow<List<SetEntry>>

    suspend fun addSetEntry(setEntry: SetEntry)

    suspend fun updateSetEntry(setEntry: SetEntry)

    suspend fun deleteSetEntry(id: SetEntryId)
}
