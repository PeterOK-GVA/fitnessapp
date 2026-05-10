package com.peter.fitness.core.ids

import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntryId
import java.util.UUID
import javax.inject.Inject

interface IdFactory {
    fun newSessionId(): SessionId
    fun newSetEntryId(): SetEntryId
}

class RandomIdFactory @Inject constructor() : IdFactory {
    override fun newSessionId(): SessionId = SessionId(UUID.randomUUID().toString())
    override fun newSetEntryId(): SetEntryId = SetEntryId(UUID.randomUUID().toString())
}
