package com.peter.fitness.data.db.mapper

import com.peter.fitness.data.db.entity.ExerciseEntity
import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand
import java.time.Instant

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id.value,
    name = name,
    movementPattern = movementPattern.name,
    loadType = loadType.name,
    techniqueDemand = techniqueDemand.name,
    conditioningSuitability = conditioningSuitability.name,
    requiresRack = requiresRack,
    requiresBench = requiresBench,
    requiresPullUpBar = requiresPullUpBar,
    barWeightAware = barWeightAware,
    supportsTempo = supportsTempo,
    createdAt = createdAt.toEpochMilli(),
)

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = ExerciseId(id),
    name = name,
    movementPattern = MovementPattern.valueOf(movementPattern),
    loadType = LoadType.valueOf(loadType),
    techniqueDemand = TechniqueDemand.valueOf(techniqueDemand),
    conditioningSuitability = ConditioningSuitability.valueOf(conditioningSuitability),
    requiresRack = requiresRack,
    requiresBench = requiresBench,
    requiresPullUpBar = requiresPullUpBar,
    barWeightAware = barWeightAware,
    supportsTempo = supportsTempo,
    createdAt = Instant.ofEpochMilli(createdAt),
)
