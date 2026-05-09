package com.peter.fitness.domain.model

import java.time.Instant

data class EquipmentInventory(
    val barKg: Double,
    val hasRack: Boolean,
    val hasBench: Boolean,
    val hasPullUpBar: Boolean,
    val plates: List<PlatePair>,
    val updatedAt: Instant,
) {
    init {
        require(barKg >= 0.0) { "barKg must be non-negative, was $barKg" }
    }

    /**
     * Smallest plate the user owns. Used for load rounding (the granularity of
     * achievable bar loads is `2 * smallestPlateKg`). Null if no plates owned.
     */
    val smallestPlateKg: Double?
        get() = plates.filter { it.pairCount > 0 }.minOfOrNull { it.denominationKg }

    companion object {
        const val DEFAULT_BAR_KG = 20.0
    }
}

data class PlatePair(
    val denominationKg: Double,
    val pairCount: Int,
) {
    init {
        require(denominationKg > 0.0) { "denominationKg must be positive, was $denominationKg" }
        require(pairCount >= 0) { "pairCount must be non-negative, was $pairCount" }
    }
}
