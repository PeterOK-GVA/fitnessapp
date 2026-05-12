package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.plates.PlateCalculator

/**
 * Pure single-exercise progression engine, v1.
 *
 * Algorithm:
 * 1. If there is no prior session (`lastResults` empty), keep the current prescription unchanged —
 *    the Coach lets the athlete establish a baseline.
 * 2. Otherwise, evaluate the completion gate and the worst subjective-load feedback.
 * 3. If the athlete failed the gate or flagged the load as too heavy, hold the prescription and
 *    reset the success streak — no deload yet (5b).
 * 4. Else pick the stimulus:
 *    - VOLUME if reps are below the working ceiling (`workingRangeMaxReps`).
 *    - INTENSITY once the ceiling is hit, with reps reset to the floor.
 * 5. Apply the magnitude — small or large — based on the feedback signal, then snap the resulting
 *    load to what the user's inventory can actually load on the bar.
 */
object CoachEngine {

    fun decideNext(
        state: ProgressionState,
        lastResults: List<CompletedSet>,
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
        val worstFeedback = worstFeedback(lastResults)

        if (gate == SetCompletion.MISSED || worstFeedback in HOLD_FEEDBACK) {
            return hold(state, gate, worstFeedback)
        }

        return if (state.currentTargetReps < state.workingRangeMaxReps) {
            volumeBump(state, policy, worstFeedback, gate)
        } else {
            intensityBump(state, policy, worstFeedback, inventory, gate)
        }
    }

    private fun hold(
        state: ProgressionState,
        gate: SetCompletion,
        feedback: SubjectiveLoad?,
    ): CoachDecision {
        val newState = state.copy(consecutiveSuccesses = 0)
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
        val baseStep = policy.volumeStep
        val bonus = if (feedback in LARGE_JUMP_FEEDBACK) 1 else 0
        val proposedReps = (state.currentTargetReps + baseStep + bonus)
            .coerceAtMost(state.workingRangeMaxReps)
        val newState = state.copy(
            currentTargetReps = proposedReps,
            lastStimulus = Stimulus.VOLUME,
            volumeStreak = state.volumeStreak + 1,
            consecutiveSuccesses = state.consecutiveSuccesses + 1,
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
        policy: CoachPolicy,
        feedback: SubjectiveLoad?,
        inventory: EquipmentInventory,
        gate: SetCompletion,
    ): CoachDecision {
        val step = if (feedback in LARGE_JUMP_FEEDBACK) {
            policy.intensityLargeStepKg
        } else {
            policy.intensitySmallStepKg
        }
        val targetLoad = state.currentLoadKg + step
        val snapResult = PlateCalculator.snap(targetLoad, inventory)
        val snappedLoad = snapResult.achievableKg
        val newReps = state.workingRangeMinReps
        val newState = state.copy(
            currentLoadKg = snappedLoad,
            currentTargetReps = newReps,
            lastStimulus = Stimulus.INTENSITY,
            volumeStreak = 0,
            consecutiveSuccesses = state.consecutiveSuccesses + 1,
        )
        val delta = snappedLoad - state.currentLoadKg
        val deltaText = if (delta >= 0.0) "+${formatKg(delta)}" else formatKg(delta)
        return CoachDecision(
            prescription = Prescription(newReps, snappedLoad),
            newState = newState,
            stimulusChanged = Stimulus.INTENSITY,
            gate = gate,
            rationale = "$deltaText (now ${formatKg(snappedLoad)} × $newReps, reset to rep floor)",
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

    private val HOLD_FEEDBACK = setOf(SubjectiveLoad.TOO_HEAVY, SubjectiveLoad.MUCH_TOO_HEAVY)
    private val LARGE_JUMP_FEEDBACK = setOf(SubjectiveLoad.TOO_LIGHT, SubjectiveLoad.MUCH_TOO_LIGHT)

    private fun formatKg(kg: Double): String =
        if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "$kg kg"
}
