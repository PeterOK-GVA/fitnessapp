package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.ActiveRestTimerEntity
import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntryId
import java.time.Instant

fun RestTimer.toEntity(id: String = ActiveRestTimerEntity.ROW_ID): ActiveRestTimerEntity =
    ActiveRestTimerEntity(
        id = id,
        startedAt = startedAt.toEpochMilli(),
        durationSeconds = durationSeconds,
        sessionId = sessionId?.value,
        setEntryId = setEntryId?.value,
        label = label,
    )

fun ActiveRestTimerEntity.toDomain(): RestTimer = RestTimer(
    startedAt = Instant.ofEpochMilli(startedAt),
    durationSeconds = durationSeconds,
    sessionId = sessionId?.let(::SessionId),
    setEntryId = setEntryId?.let(::SetEntryId),
    label = label,
)
