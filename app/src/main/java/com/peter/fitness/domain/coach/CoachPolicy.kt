package com.peter.fitness.domain.coach

/**
 * Tunable knobs for the Coach engine. The defaults are inference-grade — Freeletics never published
 * exact magnitudes — so we encode them in one place and label them as such. All v1 magnitudes are
 * the same regardless of exercise (no upper/lower/accessory distinction yet); 5b layers that in.
 */
data class CoachPolicy(
    val volumeStep: Int = DEFAULT_VOLUME_STEP,
    val intensitySmallStepKg: Double = DEFAULT_INTENSITY_SMALL_STEP_KG,
    val intensityLargeStepKg: Double = DEFAULT_INTENSITY_LARGE_STEP_KG,
) {
    init {
        require(volumeStep > 0) { "volumeStep must be positive" }
        require(intensitySmallStepKg > 0.0) { "intensitySmallStepKg must be positive" }
        require(intensityLargeStepKg >= intensitySmallStepKg) {
            "intensityLargeStepKg must be >= intensitySmallStepKg"
        }
    }

    companion object {
        const val DEFAULT_VOLUME_STEP = 1
        const val DEFAULT_INTENSITY_SMALL_STEP_KG = 2.5
        const val DEFAULT_INTENSITY_LARGE_STEP_KG = 5.0

        val DEFAULT = CoachPolicy()
    }
}
