package com.peter.fitness.domain.model

import java.time.Instant

data class SetEntry(
    val id: SetEntryId,
    val sessionId: SessionId,
    val exerciseId: ExerciseId,
    val ordinal: Int,
    val targetReps: Int,
    val targetLoadKg: Double,
    val completedReps: Int? = null,
    val performedLoadKg: Double? = null,
    val subjectiveLoad: SubjectiveLoad? = null,
    val techniqueRating: TechniqueRating? = null,
    val createdAt: Instant,
) {
    val isComplete: Boolean
        get() = completedReps != null && performedLoadKg != null
}
