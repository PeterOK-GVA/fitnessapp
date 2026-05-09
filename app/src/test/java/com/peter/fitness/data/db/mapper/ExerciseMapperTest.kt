package com.peter.fitness.data.db.mapper

import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class ExerciseMapperTest {

    @Test
    fun `round-trips through entity preserves all fields`() {
        val original = Exercise(
            id = ExerciseId("back-squat"),
            name = "Back Squat",
            movementPattern = MovementPattern.SQUAT,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
            requiresRack = true,
            requiresBench = false,
            requiresPullUpBar = false,
            barWeightAware = true,
            supportsTempo = true,
            createdAt = Instant.ofEpochMilli(1_700_000_000_000L),
        )
        val roundTripped = original.toEntity().toDomain()
        roundTripped shouldBe original
    }

    @Test
    fun `entity stores enum names as strings`() {
        val exercise = Exercise(
            id = ExerciseId("clean-and-jerk"),
            name = "Clean & Jerk",
            movementPattern = MovementPattern.EXPLOSIVE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.VERY_HIGH,
            conditioningSuitability = ConditioningSuitability.GOOD,
            createdAt = Instant.ofEpochMilli(0L),
        )
        val entity = exercise.toEntity()
        entity.movementPattern shouldBe "EXPLOSIVE"
        entity.loadType shouldBe "BARBELL"
        entity.techniqueDemand shouldBe "VERY_HIGH"
        entity.conditioningSuitability shouldBe "GOOD"
    }
}
