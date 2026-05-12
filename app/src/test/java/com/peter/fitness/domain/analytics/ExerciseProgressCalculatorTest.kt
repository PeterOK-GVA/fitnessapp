package com.peter.fitness.domain.analytics

import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class ExerciseProgressCalculatorTest {

    private val baseTime: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val backSquat = ExerciseId("back-squat")
    private val benchPress = ExerciseId("bench-press")

    @Test
    fun `progressFor groups completed sets by session and computes best e1RM per session`() {
        val sessions = listOf(
            session("s1", baseTime),
            session("s2", baseTime.plusSeconds(3 * SECONDS_PER_DAY)),
        )
        val sets = listOf(
            set("a", "s1", backSquat, 80.0, 5),
            set("b", "s1", backSquat, 90.0, 3),
            set("c", "s2", backSquat, 95.0, 5),
            set("d", "s2", backSquat, 100.0, 3),
        )

        val points = ExerciseProgressCalculator.progressFor(backSquat, sessions, sets)

        points.size shouldBe 2
        // s1: 90 * (1 + 3/30) = 99; 80 * (1 + 5/30) = 93.33 -> best is 99
        points[0].bestE1RM shouldBe (90.0 * (1.0 + 3.0 / 30.0)).plusOrMinus(1e-9)
        // s2: 95 * (1 + 5/30) = 110.83; 100 * (1 + 3/30) = 110 -> best is 110.83
        points[1].bestE1RM shouldBe (95.0 * (1.0 + 5.0 / 30.0)).plusOrMinus(1e-9)
    }

    @Test
    fun `progressFor is sorted oldest to newest by session start time`() {
        val sessions = listOf(
            session("s3", baseTime.plusSeconds(6 * SECONDS_PER_DAY)),
            session("s1", baseTime),
            session("s2", baseTime.plusSeconds(3 * SECONDS_PER_DAY)),
        )
        val sets = listOf(
            set("a", "s1", backSquat, 80.0, 5),
            set("b", "s2", backSquat, 85.0, 5),
            set("c", "s3", backSquat, 90.0, 5),
        )

        val ids = ExerciseProgressCalculator.progressFor(backSquat, sessions, sets).map { it.sessionId.value }

        ids shouldContainExactly listOf("s1", "s2", "s3")
    }

    @Test
    fun `progressFor filters out other exercises`() {
        val sessions = listOf(session("s1", baseTime))
        val sets = listOf(
            set("a", "s1", backSquat, 90.0, 5),
            set("b", "s1", benchPress, 60.0, 5),
        )
        val points = ExerciseProgressCalculator.progressFor(backSquat, sessions, sets)
        points.single().heaviestSet.loadKg shouldBe 90.0
    }

    @Test
    fun `progressFor skips sets without performed load or completed reps`() {
        val sessions = listOf(session("s1", baseTime))
        val sets = listOf(
            set("a", "s1", backSquat, 80.0, 5),
            SetEntry(
                id = SetEntryId("b"),
                sessionId = SessionId("s1"),
                exerciseId = backSquat,
                ordinal = 1,
                targetReps = 5,
                targetLoadKg = 90.0,
                completedReps = null,
                performedLoadKg = null,
                subjectiveLoad = null,
                techniqueRating = null,
                createdAt = baseTime,
            ),
        )
        ExerciseProgressCalculator.progressFor(backSquat, sessions, sets).single().setCount shouldBe 1
    }

    @Test
    fun `progressFor returns empty when no session matches`() {
        ExerciseProgressCalculator.progressFor(backSquat, emptyList(), emptyList()) shouldBe emptyList()
    }

    @Test
    fun `historyFor returns one entry per completed set sorted newest first`() {
        val sessions = listOf(session("s1", baseTime), session("s2", baseTime.plusSeconds(SECONDS_PER_DAY)))
        val sets = listOf(
            set("a", "s1", backSquat, 80.0, 5, performedAt = baseTime),
            set("b", "s2", backSquat, 85.0, 5, performedAt = baseTime.plusSeconds(SECONDS_PER_DAY)),
        )
        val history = ExerciseProgressCalculator.historyFor(backSquat, sessions, sets)
        history.map { it.setId } shouldContainExactly listOf("b", "a")
        history[0].estimatedE1RM shouldBe (85.0 * (1.0 + 5.0 / 30.0)).plusOrMinus(1e-9)
    }

    @Test
    fun `historyFor filters out sets from other exercises`() {
        val sessions = listOf(session("s1", baseTime))
        val sets = listOf(
            set("a", "s1", backSquat, 80.0, 5),
            set("b", "s1", benchPress, 60.0, 5),
        )
        ExerciseProgressCalculator.historyFor(backSquat, sessions, sets).size shouldBe 1
    }

    private fun session(id: String, startedAt: Instant): Session = Session(
        id = SessionId(id),
        startedAt = startedAt,
        endedAt = null,
        focus = SessionFocus.FREE_LOG,
        notes = null,
    )

    private fun set(
        id: String,
        sessionId: String,
        exerciseId: ExerciseId,
        loadKg: Double,
        reps: Int,
        performedAt: Instant = baseTime,
    ): SetEntry = SetEntry(
        id = SetEntryId(id),
        sessionId = SessionId(sessionId),
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

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}
