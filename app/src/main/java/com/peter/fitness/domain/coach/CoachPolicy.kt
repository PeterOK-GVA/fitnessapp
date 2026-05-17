package com.peter.fitness.domain.coach

/**
 * Tunable knobs for the Coach engine. Defaults are inference-grade — Freeletics never published
 * exact magnitudes — so they live in one place and are labelled as such. v2 differentiates
 * intensity jumps by [ExerciseClass] and adds deload parameters.
 */
data class CoachPolicy(
    val volumeStep: Int = DEFAULT_VOLUME_STEP,
    val lowerSmallKg: Double = DEFAULT_LOWER_SMALL_KG,
    val lowerLargeKg: Double = DEFAULT_LOWER_LARGE_KG,
    val upperSmallKg: Double = DEFAULT_UPPER_SMALL_KG,
    val upperLargeKg: Double = DEFAULT_UPPER_LARGE_KG,
    val accessorySmallKg: Double = DEFAULT_ACCESSORY_SMALL_KG,
    val accessoryLargeKg: Double = DEFAULT_ACCESSORY_LARGE_KG,
    val deloadThreshold: Int = DEFAULT_DELOAD_THRESHOLD,
    val deloadDropFraction: Double = DEFAULT_DELOAD_DROP_FRACTION,
) {
    init {
        require(volumeStep > 0) { "volumeStep must be positive" }
        require(lowerLargeKg >= lowerSmallKg) { "lowerLargeKg must be >= lowerSmallKg" }
        require(upperLargeKg >= upperSmallKg) { "upperLargeKg must be >= upperSmallKg" }
        require(accessoryLargeKg >= accessorySmallKg) { "accessoryLargeKg must be >= accessorySmallKg" }
        require(deloadThreshold > 0) { "deloadThreshold must be positive" }
        require(deloadDropFraction > 0.0 && deloadDropFraction < 1.0) {
            "deloadDropFraction must be in (0, 1)"
        }
    }

    fun intensityStepKg(exerciseClass: ExerciseClass, large: Boolean): Double = when (exerciseClass) {
        ExerciseClass.LOWER_COMPOUND -> if (large) lowerLargeKg else lowerSmallKg
        ExerciseClass.UPPER_COMPOUND -> if (large) upperLargeKg else upperSmallKg
        ExerciseClass.ACCESSORY -> if (large) accessoryLargeKg else accessorySmallKg
    }

    companion object {
        const val DEFAULT_VOLUME_STEP = 1
        const val DEFAULT_LOWER_SMALL_KG = 5.0
        const val DEFAULT_LOWER_LARGE_KG = 10.0
        const val DEFAULT_UPPER_SMALL_KG = 2.5
        const val DEFAULT_UPPER_LARGE_KG = 5.0
        const val DEFAULT_ACCESSORY_SMALL_KG = 1.0
        const val DEFAULT_ACCESSORY_LARGE_KG = 2.0
        const val DEFAULT_DELOAD_THRESHOLD = 5
        const val DEFAULT_DELOAD_DROP_FRACTION = 0.10

        val DEFAULT = CoachPolicy()
    }
}
