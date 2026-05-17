package com.peter.fitness.domain.coach

/**
 * Per-exercise progression state held by the Coach. The engine never mutates it; `decideNext`
 * returns the next state alongside the next prescription. v2 adds deload bookkeeping: a weighted
 * fatigue counter plus consecutive-failure tallies that trip an automatic deload.
 */
data class ProgressionState(
    val currentLoadKg: Double,
    val currentTargetReps: Int,
    val workingRangeMinReps: Int,
    val workingRangeMaxReps: Int,
    val lastStimulus: Stimulus? = null,
    val volumeStreak: Int = 0,
    val consecutiveSuccesses: Int = 0,
    val deloadCounter: Int = 0,
    val consecutiveMisses: Int = 0,
    val consecutiveMuchTooHeavy: Int = 0,
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
        require(deloadCounter >= 0) { "deloadCounter must be non-negative" }
        require(consecutiveMisses >= 0) { "consecutiveMisses must be non-negative" }
        require(consecutiveMuchTooHeavy >= 0) { "consecutiveMuchTooHeavy must be non-negative" }
    }
}
