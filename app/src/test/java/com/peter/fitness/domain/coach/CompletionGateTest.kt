package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CompletionGateTest {

    @Test
    fun `empty results returns MET`() {
        CompletionGate.evaluate(emptyList()) shouldBe SetCompletion.MET
    }

    @Test
    fun `all sets meeting target returns MET`() {
        val results = List(3) { completed(targetReps = 5, completedReps = 5) }
        CompletionGate.evaluate(results) shouldBe SetCompletion.MET
    }

    @Test
    fun `a single set short by one rep returns MARGINAL`() {
        val results = listOf(
            completed(targetReps = 5, completedReps = 5),
            completed(targetReps = 5, completedReps = 5),
            completed(targetReps = 5, completedReps = 4),
        )
        CompletionGate.evaluate(results) shouldBe SetCompletion.MARGINAL
    }

    @Test
    fun `multiple sets short returns MISSED`() {
        val results = listOf(
            completed(targetReps = 5, completedReps = 4),
            completed(targetReps = 5, completedReps = 3),
        )
        CompletionGate.evaluate(results) shouldBe SetCompletion.MISSED
    }

    @Test
    fun `one set short by three reps returns MISSED not MARGINAL`() {
        val results = listOf(
            completed(targetReps = 5, completedReps = 5),
            completed(targetReps = 5, completedReps = 5),
            completed(targetReps = 5, completedReps = 2),
        )
        CompletionGate.evaluate(results) shouldBe SetCompletion.MISSED
    }

    private fun completed(targetReps: Int, completedReps: Int): CompletedSet = CompletedSet(
        targetReps = targetReps,
        targetLoadKg = 80.0,
        completedReps = completedReps,
        performedLoadKg = 80.0,
        subjectiveLoad = SubjectiveLoad.OK,
        techniqueRating = TechniqueRating.GOOD,
    )
}
