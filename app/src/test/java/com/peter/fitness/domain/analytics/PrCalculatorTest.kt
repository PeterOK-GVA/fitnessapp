package com.peter.fitness.domain.analytics

import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.model.TechniqueDemand
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class PrCalculatorTest {

    private val now: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val backSquat = exercise("back-squat", "Back Squat")
    private val benchPress = exercise("bench-press", "Bench Press")

    @Test
    fun `epley estimate for a single returns slightly above the lifted load`() {
        PrCalculator.epleyE1RM(loadKg = 100.0, reps = 1) shouldBe (100.0 + 100.0 / 30.0).plusOrMinus(1e-9)
    }

    @Test
    fun `epley estimate for five reps matches the textbook formula`() {
        PrCalculator.epleyE1RM(loadKg = 100.0, reps = 5) shouldBe (100.0 * (1.0 + 5.0 / 30.0)).plusOrMinus(1e-9)
    }

    @Test
    fun `summaries are empty when no completed sets exist`() {
        PrCalculator.summaries(sets = emptyList(), exercises = listOf(backSquat)) shouldBe emptyList()
    }

    @Test
    fun `summaries pick the heaviest load regardless of rep count`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 80.0, reps = 5, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 100.0, reps = 3, performedAt = now.plusSeconds(60)),
            completedSet("c", backSquat.id, loadKg = 95.0, reps = 5, performedAt = now.plusSeconds(120)),
        )
        val summary = PrCalculator.summaries(sets, listOf(backSquat)).single()
        summary.totalSets shouldBe 3
        summary.heaviestSet.shouldNotBeNull()
        summary.heaviestSet!!.loadKg shouldBe 100.0
        summary.heaviestSet!!.reps shouldBe 3
    }

    @Test
    fun `summaries pick the best estimated 1RM across all completed sets`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 100.0, reps = 1, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 95.0, reps = 5, performedAt = now.plusSeconds(60)),
        )
        val summary = PrCalculator.summaries(sets, listOf(backSquat)).single()
        // 95 * (1 + 5/30) = 110.83; 100 * (1 + 1/30) = 103.33 — the multi-rep wins.
        summary.bestE1RM.shouldNotBeNull()
        summary.bestE1RM!!.sourceLoadKg shouldBe 95.0
        summary.bestE1RM!!.sourceReps shouldBe 5
        summary.bestE1RM!!.estimatedKg shouldBe (95.0 * (1.0 + 5.0 / 30.0)).plusOrMinus(1e-9)
    }

    @Test
    fun `summaries exclude sets without performed load or completed reps`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 80.0, reps = 5, performedAt = now),
            SetEntry(
                id = SetEntryId("b"),
                sessionId = SessionId("session-1"),
                exerciseId = backSquat.id,
                ordinal = 1,
                targetReps = 5,
                targetLoadKg = 80.0,
                completedReps = null,
                performedLoadKg = null,
                subjectiveLoad = null,
                techniqueRating = null,
                createdAt = now.plusSeconds(60),
            ),
        )
        val summary = PrCalculator.summaries(sets, listOf(backSquat)).single()
        summary.totalSets shouldBe 1
    }

    @Test
    fun `summaries exclude sets with zero or negative reps`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 80.0, reps = 0, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 80.0, reps = 5, performedAt = now.plusSeconds(60)),
        )
        val summary = PrCalculator.summaries(sets, listOf(backSquat)).single()
        summary.totalSets shouldBe 1
        summary.heaviestSet?.reps shouldBe 5
    }

    @Test
    fun `summaries are sorted by totalSets descending`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 80.0, reps = 5, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 85.0, reps = 5, performedAt = now.plusSeconds(60)),
            completedSet("c", backSquat.id, loadKg = 90.0, reps = 5, performedAt = now.plusSeconds(120)),
            completedSet("d", benchPress.id, loadKg = 60.0, reps = 5, performedAt = now.plusSeconds(180)),
        )
        val ids = PrCalculator.summaries(sets, listOf(backSquat, benchPress)).map { it.exerciseId.value }
        ids shouldContainExactly listOf("back-squat", "bench-press")
    }

    @Test
    fun `summaries skip exercises absent from the catalogue`() {
        val ghost = ExerciseId("ghost")
        val sets = listOf(
            completedSet("a", ghost, loadKg = 80.0, reps = 5, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 90.0, reps = 5, performedAt = now),
        )
        val summaries = PrCalculator.summaries(sets, listOf(backSquat))
        summaries.size shouldBe 1
        summaries.single().exerciseId shouldBe backSquat.id
    }

    @Test
    fun `heaviest and best can come from different sets`() {
        val sets = listOf(
            completedSet("a", backSquat.id, loadKg = 100.0, reps = 1, performedAt = now),
            completedSet("b", backSquat.id, loadKg = 95.0, reps = 5, performedAt = now.plusSeconds(60)),
        )
        val summary = PrCalculator.summaries(sets, listOf(backSquat)).single()
        summary.heaviestSet?.loadKg shouldBe 100.0
        summary.bestE1RM?.sourceLoadKg shouldBe 95.0
    }

    @Test
    fun `summaries with no matching sets surface null heaviest and best`() {
        // No sets at all -> no summary returned (already covered).
        // But if an exercise has zero completed sets but other exercises do, that exercise is absent.
        val sets = listOf(
            completedSet("a", benchPress.id, loadKg = 60.0, reps = 5, performedAt = now),
        )
        val summaries = PrCalculator.summaries(sets, listOf(backSquat, benchPress))
        summaries.find { it.exerciseId == backSquat.id }.shouldBeNull()
        summaries.find { it.exerciseId == benchPress.id }.shouldNotBeNull()
    }

    private fun exercise(id: String, name: String): Exercise = Exercise(
        id = ExerciseId(id),
        name = name,
        movementPattern = MovementPattern.SQUAT,
        loadType = LoadType.BARBELL,
        techniqueDemand = TechniqueDemand.HIGH,
        conditioningSuitability = ConditioningSuitability.LIMITED,
        createdAt = Instant.EPOCH,
    )

    private fun completedSet(
        id: String,
        exerciseId: ExerciseId,
        loadKg: Double,
        reps: Int,
        performedAt: Instant,
    ): SetEntry = SetEntry(
        id = SetEntryId(id),
        sessionId = SessionId("session-1"),
        exerciseId = exerciseId,
        ordinal = 0,
        targetReps = reps,
        targetLoadKg = loadKg,
        completedReps = reps,
        performedLoadKg = loadKg,
        subjectiveLoad = null,
        techniqueRating = null,
        createdAt = performedAt,
    )
}
