package com.peter.fitness.domain.coach

enum class SetCompletion { MET, MARGINAL, MISSED }

object CompletionGate {

    /**
     * Evaluate how the user actually performed against the prescribed reps. v1 is binary-ish: any
     * miss counts as MISSED unless it's a single set short by one or two reps (MARGINAL). v2 will
     * weight technique and per-set deviation.
     */
    fun evaluate(results: List<CompletedSet>): SetCompletion {
        if (results.isEmpty()) return SetCompletion.MET
        val misses = results.count { it.completedReps < it.targetReps }
        if (misses == 0) return SetCompletion.MET
        val totalShortBy = results.sumOf { (it.targetReps - it.completedReps).coerceAtLeast(0) }
        return when {
            misses == 1 && totalShortBy <= MARGINAL_REP_TOLERANCE -> SetCompletion.MARGINAL
            else -> SetCompletion.MISSED
        }
    }

    private const val MARGINAL_REP_TOLERANCE = 2
}
