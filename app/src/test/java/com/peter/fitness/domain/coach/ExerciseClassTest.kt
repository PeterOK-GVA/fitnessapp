package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExerciseClassTest {

    @Test
    fun `barbell squat hinge and lunge are lower compounds`() {
        ExerciseClassifier.classify(MovementPattern.SQUAT, LoadType.BARBELL) shouldBe
            ExerciseClass.LOWER_COMPOUND
        ExerciseClassifier.classify(MovementPattern.HIP_HINGE, LoadType.BARBELL) shouldBe
            ExerciseClass.LOWER_COMPOUND
        ExerciseClassifier.classify(MovementPattern.LUNGE, LoadType.BARBELL) shouldBe
            ExerciseClass.LOWER_COMPOUND
    }

    @Test
    fun `barbell presses and rows are upper compounds`() {
        ExerciseClassifier.classify(MovementPattern.HORIZONTAL_PUSH, LoadType.BARBELL) shouldBe
            ExerciseClass.UPPER_COMPOUND
        ExerciseClassifier.classify(MovementPattern.HORIZONTAL_PULL, LoadType.BARBELL) shouldBe
            ExerciseClass.UPPER_COMPOUND
        ExerciseClassifier.classify(MovementPattern.VERTICAL_PUSH, LoadType.BARBELL) shouldBe
            ExerciseClass.UPPER_COMPOUND
        ExerciseClassifier.classify(MovementPattern.VERTICAL_PULL, LoadType.BARBELL) shouldBe
            ExerciseClass.UPPER_COMPOUND
    }

    @Test
    fun `barbell explosive and core movements fall back to accessory`() {
        ExerciseClassifier.classify(MovementPattern.EXPLOSIVE, LoadType.BARBELL) shouldBe
            ExerciseClass.ACCESSORY
        ExerciseClassifier.classify(MovementPattern.CORE_ROTATION, LoadType.BARBELL) shouldBe
            ExerciseClass.ACCESSORY
        ExerciseClassifier.classify(MovementPattern.CARRY, LoadType.BARBELL) shouldBe
            ExerciseClass.ACCESSORY
    }

    @Test
    fun `non-barbell load is always an accessory regardless of pattern`() {
        ExerciseClassifier.classify(MovementPattern.SQUAT, LoadType.DUMBBELL) shouldBe
            ExerciseClass.ACCESSORY
        ExerciseClassifier.classify(MovementPattern.HORIZONTAL_PUSH, LoadType.KETTLEBELL) shouldBe
            ExerciseClass.ACCESSORY
        ExerciseClassifier.classify(MovementPattern.HIP_HINGE, LoadType.WEIGHT_PLATE) shouldBe
            ExerciseClass.ACCESSORY
    }
}
