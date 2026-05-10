package com.peter.fitness.domain.plates

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import kotlin.math.abs

data class PlateLoad(
    val denominationKg: Double,
    val countPerSide: Int,
) {
    init {
        require(denominationKg > 0.0) { "denominationKg must be positive" }
        require(countPerSide >= 0) { "countPerSide must be non-negative" }
    }

    val totalKg: Double get() = denominationKg * countPerSide * 2.0
}

data class SnapResult(
    val targetKg: Double,
    val achievableKg: Double,
    val barKg: Double,
    val perSide: List<PlateLoad>,
) {
    val totalPlatesKg: Double get() = perSide.sumOf { it.totalKg }
    val deviationKg: Double get() = achievableKg - targetKg
}

/**
 * Pure plate-rounding for a barbell. Given a target load and an EquipmentInventory, finds the
 * achievable load (using the bar plus a symmetric per-side plate stack) closest to the target,
 * and returns that load along with the plate breakdown.
 *
 * Tie-breaking: when two achievable loads are equidistant from target, the lower load is chosen.
 *
 * Algorithm: bottom-up DP over plate denominations. For each denomination (heaviest first), it
 * tracks the best (fewest-plate) stack that achieves each per-side load. The state space stays
 * manageable for realistic inventories (typically <200 distinct loads).
 */
object PlateCalculator {

    fun snap(targetKg: Double, inventory: EquipmentInventory): SnapResult {
        val bar = inventory.barKg
        if (targetKg <= bar) {
            return SnapResult(
                targetKg = targetKg,
                achievableKg = bar,
                barKg = bar,
                perSide = emptyList(),
            )
        }
        val perSideTarget = (targetKg - bar) / 2.0
        val plates = inventory.plates.filter { it.pairCount > 0 }
        val (closestPerSide, stack) = bestPerSideStack(perSideTarget, plates)
        return SnapResult(
            targetKg = targetKg,
            achievableKg = bar + 2.0 * closestPerSide,
            barKg = bar,
            perSide = stack,
        )
    }

    private fun bestPerSideStack(
        target: Double,
        plates: List<PlatePair>,
    ): Pair<Double, List<PlateLoad>> {
        if (plates.isEmpty()) return 0.0 to emptyList()

        var states: Map<Double, List<PlateLoad>> = mapOf(0.0 to emptyList())
        for (pair in plates.sortedByDescending { it.denominationKg }) {
            states = expandStates(states, pair)
        }

        val (load, stack) = states.entries
            .sortedWith(compareBy({ abs(it.key - target) }, { it.key }))
            .first()
        return load to stack
    }

    private fun expandStates(
        states: Map<Double, List<PlateLoad>>,
        pair: PlatePair,
    ): Map<Double, List<PlateLoad>> {
        val next = HashMap<Double, List<PlateLoad>>()
        for ((load, stack) in states) {
            for (n in 0..pair.pairCount) {
                val newLoad = load + n * pair.denominationKg
                val newStack = if (n > 0) stack + PlateLoad(pair.denominationKg, n) else stack
                val existing = next[newLoad]
                if (isBetter(newStack, existing)) {
                    next[newLoad] = newStack
                }
            }
        }
        return next
    }

    /**
     * Compares two stacks reaching the same per-side load. Prefers fewer total plates per side;
     * on ties, prefers the stack whose heaviest plate is larger (more ergonomic to load).
     */
    private fun isBetter(candidate: List<PlateLoad>, current: List<PlateLoad>?): Boolean {
        if (current == null) return true
        val candidateCount = candidate.sumOf { it.countPerSide }
        val currentCount = current.sumOf { it.countPerSide }
        if (candidateCount != currentCount) return candidateCount < currentCount
        val candidateMax = candidate.maxOfOrNull { it.denominationKg } ?: 0.0
        val currentMax = current.maxOfOrNull { it.denominationKg } ?: 0.0
        return candidateMax > currentMax
    }
}
