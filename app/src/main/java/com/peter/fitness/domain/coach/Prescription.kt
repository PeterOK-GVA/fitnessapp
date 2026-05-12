package com.peter.fitness.domain.coach

data class Prescription(
    val targetReps: Int,
    val targetLoadKg: Double,
) {
    init {
        require(targetReps > 0) { "targetReps must be positive" }
        require(targetLoadKg >= 0.0) { "targetLoadKg must be non-negative" }
    }
}
