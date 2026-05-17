package com.peter.fitness.domain.coach

/**
 * What the Coach suggests for the next set of an exercise: the prescription to pre-fill, a
 * human-readable rationale, and the progression state to persist if the athlete accepts (or tweaks)
 * and saves.
 */
data class CoachProposal(
    val prescription: Prescription,
    val rationale: String,
    val newState: ProgressionState,
)
