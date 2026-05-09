package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.SessionEntity
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import java.time.Instant

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id.value,
    startedAt = startedAt.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    focus = focus.name,
    notes = notes,
)

fun SessionEntity.toDomain(): Session = Session(
    id = SessionId(id),
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    focus = SessionFocus.valueOf(focus),
    notes = notes,
)
