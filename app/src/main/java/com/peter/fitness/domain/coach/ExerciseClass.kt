package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern

/**
 * Coarse training-load classification used to scale intensity jumps. Lower-body barbell compounds
 * tolerate bigger absolute weight steps than upper-body ones; everything non-barbell (or explosive
 * / carry / core) is treated as an accessory with the smallest steps.
 */
enum class ExerciseClass { LOWER_COMPOUND, UPPER_COMPOUND, ACCESSORY }

object ExerciseClassifier {

    fun classify(movementPattern: MovementPattern, loadType: LoadType): ExerciseClass = when {
        loadType != LoadType.BARBELL -> ExerciseClass.ACCESSORY
        movementPattern in LOWER -> ExerciseClass.LOWER_COMPOUND
        movementPattern in UPPER -> ExerciseClass.UPPER_COMPOUND
        else -> ExerciseClass.ACCESSORY
    }

    private val LOWER = setOf(
        MovementPattern.SQUAT,
        MovementPattern.HIP_HINGE,
        MovementPattern.LUNGE,
    )

    private val UPPER = setOf(
        MovementPattern.HORIZONTAL_PUSH,
        MovementPattern.HORIZONTAL_PULL,
        MovementPattern.VERTICAL_PUSH,
        MovementPattern.VERTICAL_PULL,
    )
}
