package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating

/**
 * Focused view of a logged set that the Coach engine consumes when deciding the next prescription.
 * Decoupled from `SetEntry` so the engine stays free of storage concerns.
 */
data class CompletedSet(
    val targetReps: Int,
    val targetLoadKg: Double,
    val completedReps: Int,
    val performedLoadKg: Double,
    val subjectiveLoad: SubjectiveLoad?,
    val techniqueRating: TechniqueRating?,
) {
    init {
        require(targetReps >= 0) { "targetReps must be non-negative" }
        require(completedReps >= 0) { "completedReps must be non-negative" }
        require(targetLoadKg >= 0.0) { "targetLoadKg must be non-negative" }
        require(performedLoadKg >= 0.0) { "performedLoadKg must be non-negative" }
    }
}
