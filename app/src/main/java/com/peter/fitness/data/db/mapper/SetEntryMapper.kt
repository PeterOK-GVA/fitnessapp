package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.SetEntryEntity
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import java.time.Instant

fun SetEntry.toEntity(): SetEntryEntity = SetEntryEntity(
    id = id.value,
    sessionId = sessionId.value,
    exerciseId = exerciseId.value,
    ordinal = ordinal,
    targetReps = targetReps,
    targetLoadKg = targetLoadKg,
    completedReps = completedReps,
    performedLoadKg = performedLoadKg,
    subjectiveLoad = subjectiveLoad?.name,
    techniqueRating = techniqueRating?.name,
    createdAt = createdAt.toEpochMilli(),
)

fun SetEntryEntity.toDomain(): SetEntry = SetEntry(
    id = SetEntryId(id),
    sessionId = SessionId(sessionId),
    exerciseId = ExerciseId(exerciseId),
    ordinal = ordinal,
    targetReps = targetReps,
    targetLoadKg = targetLoadKg,
    completedReps = completedReps,
    performedLoadKg = performedLoadKg,
    subjectiveLoad = subjectiveLoad?.let(SubjectiveLoad::valueOf),
    techniqueRating = techniqueRating?.let(TechniqueRating::valueOf),
    createdAt = Instant.ofEpochMilli(createdAt),
)
