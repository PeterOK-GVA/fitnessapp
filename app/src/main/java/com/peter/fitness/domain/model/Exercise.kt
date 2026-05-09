package com.peter.fitness.domain.model

import java.time.Instant

data class Exercise(
    val id: ExerciseId,
    val name: String,
    val movementPattern: MovementPattern,
    val loadType: LoadType,
    val techniqueDemand: TechniqueDemand,
    val conditioningSuitability: ConditioningSuitability,
    val requiresRack: Boolean = false,
    val requiresBench: Boolean = false,
    val requiresPullUpBar: Boolean = false,
    val barWeightAware: Boolean = true,
    val supportsTempo: Boolean = true,
    val createdAt: Instant,
)
