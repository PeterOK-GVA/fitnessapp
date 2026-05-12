package com.peter.fitness.domain.coach

data class CoachDecision(
    val prescription: Prescription,
    val newState: ProgressionState,
    val stimulusChanged: Stimulus?,
    val gate: SetCompletion,
    val rationale: String,
)
