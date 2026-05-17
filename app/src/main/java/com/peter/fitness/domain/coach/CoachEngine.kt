package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.plates.PlateCalculator

/**
 * Pure single-exercise progression engine, v2.
 *
 * Decision order:
 * 1. No prior session (`lastResults` empty) → keep the current prescription (establish a baseline).
 * 2. Update deload bookkeeping from the gate + worst subjective-load feedback:
 *    - weighted fatigue counter (MUCH_TOO_HEAVY +2, TOO_HEAVY +1, MISSED +1),
 *    - consecutive MUCH_TOO_HEAVY and consecutive MISSED tallies,
 *    - a clean success (gate not MISSED and feedback not too-heavy) clears all of them.
 * 3. Deload if the counter hits the threshold, or 2 consecutive MUCH_TOO_HEAVY, or 3 consecutive
 *    MISSED → drop load by `deloadDropFraction`, reps back to the floor, counters cleared.
 * 4. Else if the session failed the gate or flagged too-heavy → hold, carrying the counters.
 * 5. Else apply the volume-before-intensity ladder: +reps below the rep ceiling, +load (scaled by
 *    [ExerciseClass]) once the ceiling is hit with reps reset to the floor. Feedback of TOO_LIGHT /
 *    MUCH_TOO_LIGHT escalates to the large step / +2 reps.
 *
 * Every load the engine emits is snapped to what the athlete's inventory can actually load.
 */
object CoachEngine {

    fun decideNext(
        state: ProgressionState,
        lastResults: List<CompletedSet>,
        exerciseClass: ExerciseClass,
        policy: CoachPolicy,
        inventory: EquipmentInventory,
    ): CoachDecision {
        if (lastResults.isEmpty()) {
            return CoachDecision(
                prescription = Prescription(state.currentTargetReps, state.currentLoadKg),
                newState = state,
                stimulusChanged = null,
                gate = SetCompletion.MET,
                rationale = "First session at ${formatKg(state.currentLoadKg)} × ${state.currentTargetReps}",
            )
        }

        val gate = CompletionGate.evaluate(lastResults)
        val feedback = worstFeedback(lastResults)
        val success = gate != SetCompletion.MISSED && feedback !in HOLD_FEEDBACK

        val consecutiveMisses = if (gate == SetCompletion.MISSED) state.consecutiveMisses + 1 else 0
        val consecutiveMuchTooHeavy =
            if (feedback == SubjectiveLoad.MUCH_TOO_HEAVY) state.consecutiveMuchTooHeavy + 1 else 0
        val deloadCounter = if (success) 0 else state.deloadCounter + signalWeight(gate, feedback)

        val counters = Counters(deloadCounter, consecutiveMisses, consecutiveMuchTooHeavy)
        val shouldDeload = deloadCounter >= policy.deloadThreshold ||
            consecutiveMuchTooHeavy >= CONSECUTIVE_MUCH_TOO_HEAVY_TRIGGER ||
            consecutiveMisses >= CONSECUTIVE_MISS_TRIGGER

        return when {
            shouldDeload -> deload(state, policy, inventory, gate)
            !success -> hold(state, gate, feedback, counters)
            state.currentTargetReps < state.workingRangeMaxReps ->
                volumeBump(state, policy, feedback, gate)
            else -> intensityBump(state, exerciseClass, policy, feedback, inventory, gate)
        }
    }

    private data class Counters(
        val deloadCounter: Int,
        val consecutiveMisses: Int,
        val consecutiveMuchTooHeavy: Int,
    )

    private fun deload(
        state: ProgressionState,
        policy: CoachPolicy,
        inventory: EquipmentInventory,
        gate: SetCompletion,
    ): CoachDecision {
        val target = state.currentLoadKg * (1.0 - policy.deloadDropFraction)
        val snapped = PlateCalculator.snap(target, inventory).achievableKg
        val newState = state.copy(
            currentLoadKg = snapped,
            currentTargetReps = state.workingRangeMinReps,
            lastStimulus = Stimulus.INTENSITY,
            volumeStreak = 0,
            consecutiveSuccesses = 0,
            deloadCounter = 0,
            consecutiveMisses = 0,
            consecutiveMuchTooHeavy = 0,
        )
        return CoachDecision(
            prescription = Prescription(state.workingRangeMinReps, snapped),
            newState = newState,
            stimulusChanged = Stimulus.INTENSITY,
            gate = gate,
            deloaded = true,
            rationale = "Deload to ${formatKg(snapped)} × ${state.workingRangeMinReps} " +
                "(−${(policy.deloadDropFraction * PERCENT).toInt()}%, counters cleared)",
        )
    }

    private fun hold(
        state: ProgressionState,
        gate: SetCompletion,
        feedback: SubjectiveLoad?,
        counters: Counters,
    ): CoachDecision {
        val newState = state.copy(
            consecutiveSuccesses = 0,
            deloadCounter = counters.deloadCounter,
            consecutiveMisses = counters.consecutiveMisses,
            consecutiveMuchTooHeavy = counters.consecutiveMuchTooHeavy,
        )
        val reason = when {
            gate == SetCompletion.MISSED -> "last session missed the rep target"
            feedback == SubjectiveLoad.MUCH_TOO_HEAVY -> "last session flagged much too heavy"
            feedback == SubjectiveLoad.TOO_HEAVY -> "last session flagged too heavy"
            else -> "holding"
        }
        return CoachDecision(
            prescription = Prescription(state.currentTargetReps, state.currentLoadKg),
            newState = newState,
            stimulusChanged = null,
            gate = gate,
            rationale = "Holding ${formatKg(state.currentLoadKg)} × ${state.currentTargetReps} — $reason",
        )
    }

    private fun volumeBump(
        state: ProgressionState,
        policy: CoachPolicy,
        feedback: SubjectiveLoad?,
        gate: SetCompletion,
    ): CoachDecision {
        val bonus = if (feedback in LARGE_JUMP_FEEDBACK) 1 else 0
        val proposedReps = (state.currentTargetReps + policy.volumeStep + bonus)
            .coerceAtMost(state.workingRangeMaxReps)
        val newState = state.copy(
            currentTargetReps = proposedReps,
            lastStimulus = Stimulus.VOLUME,
            volumeStreak = state.volumeStreak + 1,
            consecutiveSuccesses = state.consecutiveSuccesses + 1,
            deloadCounter = 0,
            consecutiveMisses = 0,
            consecutiveMuchTooHeavy = 0,
        )
        return CoachDecision(
            prescription = Prescription(proposedReps, state.currentLoadKg),
            newState = newState,
            stimulusChanged = Stimulus.VOLUME,
            gate = gate,
            rationale = "+${proposedReps - state.currentTargetReps} reps " +
                "(now ${formatKg(state.currentLoadKg)} × $proposedReps)",
        )
    }

    private fun intensityBump(
        state: ProgressionState,
        exerciseClass: ExerciseClass,
        policy: CoachPolicy,
        feedback: SubjectiveLoad?,
        inventory: EquipmentInventory,
        gate: SetCompletion,
    ): CoachDecision {
        val large = feedback in LARGE_JUMP_FEEDBACK
        val step = policy.intensityStepKg(exerciseClass, large)
        val target = state.currentLoadKg + step
        val snapped = PlateCalculator.snap(target, inventory).achievableKg
        val newReps = state.workingRangeMinReps
        val newState = state.copy(
            currentLoadKg = snapped,
            currentTargetReps = newReps,
            lastStimulus = Stimulus.INTENSITY,
            volumeStreak = 0,
            consecutiveSuccesses = state.consecutiveSuccesses + 1,
            deloadCounter = 0,
            consecutiveMisses = 0,
            consecutiveMuchTooHeavy = 0,
        )
        val delta = snapped - state.currentLoadKg
        val deltaText = if (delta >= 0.0) "+${formatKg(delta)}" else formatKg(delta)
        return CoachDecision(
            prescription = Prescription(newReps, snapped),
            newState = newState,
            stimulusChanged = Stimulus.INTENSITY,
            gate = gate,
            rationale = "$deltaText (now ${formatKg(snapped)} × $newReps, reset to rep floor)",
        )
    }

    /** Returns the most-cautious feedback across the set list. Null if no feedback was given. */
    internal fun worstFeedback(results: List<CompletedSet>): SubjectiveLoad? {
        val ordered = listOf(
            SubjectiveLoad.MUCH_TOO_HEAVY,
            SubjectiveLoad.TOO_HEAVY,
            SubjectiveLoad.OK,
            SubjectiveLoad.TOO_LIGHT,
            SubjectiveLoad.MUCH_TOO_LIGHT,
        )
        return ordered.firstOrNull { candidate -> results.any { it.subjectiveLoad == candidate } }
    }

    private fun signalWeight(gate: SetCompletion, feedback: SubjectiveLoad?): Int {
        val feedbackWeight = when (feedback) {
            SubjectiveLoad.MUCH_TOO_HEAVY -> MUCH_TOO_HEAVY_WEIGHT
            SubjectiveLoad.TOO_HEAVY -> TOO_HEAVY_WEIGHT
            else -> 0
        }
        val gateWeight = if (gate == SetCompletion.MISSED) MISSED_WEIGHT else 0
        return feedbackWeight + gateWeight
    }

    private val HOLD_FEEDBACK = setOf(SubjectiveLoad.TOO_HEAVY, SubjectiveLoad.MUCH_TOO_HEAVY)
    private val LARGE_JUMP_FEEDBACK = setOf(SubjectiveLoad.TOO_LIGHT, SubjectiveLoad.MUCH_TOO_LIGHT)

    private const val MUCH_TOO_HEAVY_WEIGHT = 2
    private const val TOO_HEAVY_WEIGHT = 1
    private const val MISSED_WEIGHT = 1
    private const val CONSECUTIVE_MUCH_TOO_HEAVY_TRIGGER = 2
    private const val CONSECUTIVE_MISS_TRIGGER = 3
    private const val PERCENT = 100.0

    private fun formatKg(kg: Double): String =
        if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "$kg kg"
}
