package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.ProgressionStateEntity
import com.peter.fitness.domain.coach.ProgressionState
import com.peter.fitness.domain.coach.Stimulus
import com.peter.fitness.domain.model.ExerciseId
import java.time.Instant

fun ProgressionStateEntity.toDomain(): ProgressionState = ProgressionState(
    currentLoadKg = currentLoadKg,
    currentTargetReps = currentTargetReps,
    workingRangeMinReps = workingRangeMinReps,
    workingRangeMaxReps = workingRangeMaxReps,
    lastStimulus = lastStimulus?.let(Stimulus::valueOf),
    volumeStreak = volumeStreak,
    consecutiveSuccesses = consecutiveSuccesses,
)

fun ProgressionState.toEntity(exerciseId: ExerciseId, updatedAt: Instant): ProgressionStateEntity =
    ProgressionStateEntity(
        exerciseId = exerciseId.value,
        currentLoadKg = currentLoadKg,
        currentTargetReps = currentTargetReps,
        workingRangeMinReps = workingRangeMinReps,
        workingRangeMaxReps = workingRangeMaxReps,
        lastStimulus = lastStimulus?.name,
        volumeStreak = volumeStreak,
        consecutiveSuccesses = consecutiveSuccesses,
        updatedAt = updatedAt.toEpochMilli(),
    )
