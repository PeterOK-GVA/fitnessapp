package com.peter.fitness.domain.coach

/**
 * Per-exercise progression state held by the Coach. Captures the working range, the most recently
 * prescribed load and rep target, and bookkeeping for the volume-before-intensity rule. The engine
 * never mutates `ProgressionState`; `decideNext` returns the next state alongside the next
 * prescription.
 */
data class ProgressionState(
    val currentLoadKg: Double,
    val currentTargetReps: Int,
    val workingRangeMinReps: Int,
    val workingRangeMaxReps: Int,
    val lastStimulus: Stimulus? = null,
    val volumeStreak: Int = 0,
    val consecutiveSuccesses: Int = 0,
) {
    init {
        require(currentLoadKg >= 0.0) { "currentLoadKg must be non-negative" }
        require(currentTargetReps in workingRangeMinReps..workingRangeMaxReps) {
            "currentTargetReps must lie within [$workingRangeMinReps, $workingRangeMaxReps]"
        }
        require(workingRangeMinReps > 0) { "workingRangeMinReps must be positive" }
        require(workingRangeMaxReps >= workingRangeMinReps) {
            "workingRangeMaxReps must be >= workingRangeMinReps"
        }
        require(volumeStreak >= 0) { "volumeStreak must be non-negative" }
        require(consecutiveSuccesses >= 0) { "consecutiveSuccesses must be non-negative" }
    }
}
