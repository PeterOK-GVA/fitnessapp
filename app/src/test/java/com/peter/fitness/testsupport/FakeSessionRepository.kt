package com.peter.fitness.testsupport

import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionRepository(
    initialSessions: List<Session> = emptyList(),
    initialSets: List<SetEntry> = emptyList(),
) : SessionRepository {

    private val _sessions = MutableStateFlow(initialSessions)
    private val _sets = MutableStateFlow(initialSets)

    var createCount: Int = 0
        private set
    var updateCount: Int = 0
        private set

    val createdSessions: List<Session>
        get() = _sessions.value

    override fun observeAll(): Flow<List<Session>> = _sessions.asStateFlow()

    override suspend fun findById(id: SessionId): Session? =
        _sessions.value.firstOrNull { it.id == id }

    override suspend fun create(session: Session) {
        _sessions.value = _sessions.value + session
        createCount += 1
    }

    override suspend fun update(session: Session) {
        _sessions.value = _sessions.value.map { if (it.id == session.id) session else it }
        updateCount += 1
    }

    override suspend fun delete(id: SessionId) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
    }

    override fun observeSetEntries(sessionId: SessionId): Flow<List<SetEntry>> =
        _sets.map { all -> all.filter { it.sessionId == sessionId } }

    override suspend fun addSetEntry(setEntry: SetEntry) {
        _sets.value = _sets.value + setEntry
    }

    override suspend fun updateSetEntry(setEntry: SetEntry) {
        _sets.value = _sets.value.map { if (it.id == setEntry.id) setEntry else it }
    }

    override suspend fun deleteSetEntry(id: SetEntryId) {
        _sets.value = _sets.value.filterNot { it.id == id }
    }
}
