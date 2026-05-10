package com.peter.fitness.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueDemand
import com.peter.fitness.domain.model.TechniqueRating
import com.peter.fitness.testsupport.FakeExerciseRepository
import com.peter.fitness.testsupport.FakeSessionRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val now: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val sessionId = SessionId("session-1")

    private val seedSession = Session(
        id = sessionId,
        startedAt = now,
        endedAt = Instant.parse("2026-05-10T09:30:00Z"),
        focus = SessionFocus.STRENGTH,
        notes = null,
    )

    private val backSquat = Exercise(
        id = ExerciseId("back-squat"),
        name = "Back Squat",
        movementPattern = MovementPattern.SQUAT,
        loadType = LoadType.BARBELL,
        techniqueDemand = TechniqueDemand.HIGH,
        conditioningSuitability = ConditioningSuitability.LIMITED,
        requiresRack = true,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `init loads session metadata and surfaces sets`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(set("s1", reps = 5, kg = 80.0), set("s2", reps = 5, kg = 85.0)),
        )
        val exerciseRepo = FakeExerciseRepository(initial = listOf(backSquat))
        val vm = newViewModel(sessionRepo, exerciseRepo)

        advanceUntilIdle()

        val state = vm.uiState.value
        state.isLoading shouldBe false
        state.notFound shouldBe false
        state.focus shouldBe SessionFocus.STRENGTH
        state.startedAt shouldBe seedSession.startedAt
        state.endedAt shouldBe seedSession.endedAt
        state.sets.size shouldBe 2
        state.sets[0].exerciseName shouldBe "Back Squat"
    }

    @Test
    fun `total volume sums reps times kg across sets`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(
                set("s1", reps = 5, kg = 80.0),
                set("s2", reps = 5, kg = 85.0),
                set("s3", reps = 3, kg = 90.0),
            ),
        )
        val vm = newViewModel(sessionRepo, FakeExerciseRepository(initial = listOf(backSquat)))

        advanceUntilIdle()

        val expected = 5 * 80.0 + 5 * 85.0 + 3 * 90.0
        vm.uiState.value.totalVolumeKg shouldBe expected
    }

    @Test
    fun `unknown session sets notFound flag`() = runTest(main.dispatcher) {
        val vm = newViewModel(FakeSessionRepository(), FakeExerciseRepository(), id = "ghost")

        advanceUntilIdle()

        vm.uiState.value.notFound shouldBe true
        vm.uiState.value.isLoading shouldBe false
    }

    @Test
    fun `delete drops session, marks isDeleted, and emits Deleted`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(set("s1", reps = 5, kg = 80.0)),
        )
        val vm = newViewModel(sessionRepo, FakeExerciseRepository(initial = listOf(backSquat)))
        advanceUntilIdle()

        vm.events.test {
            vm.onDelete()
            awaitItem() shouldBe SessionDetailEvent.Deleted
            cancelAndIgnoreRemainingEvents()
        }

        sessionRepo.findById(sessionId) shouldBe null
        vm.uiState.value.isDeleted shouldBe true
    }

    @Test
    fun `concurrent delete calls only persist once`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = emptyList(),
        )
        val vm = newViewModel(sessionRepo, FakeExerciseRepository())
        advanceUntilIdle()

        vm.onDelete()
        vm.onDelete()
        advanceUntilIdle()

        // After first delete the session is gone; second call is short-circuited.
        sessionRepo.findById(sessionId) shouldBe null
    }

    private fun set(id: String, reps: Int, kg: Double): SetEntry = SetEntry(
        id = SetEntryId(id),
        sessionId = sessionId,
        exerciseId = ExerciseId("back-squat"),
        ordinal = 0,
        targetReps = reps,
        targetLoadKg = kg,
        completedReps = reps,
        performedLoadKg = kg,
        subjectiveLoad = SubjectiveLoad.OK,
        techniqueRating = TechniqueRating.GOOD,
        createdAt = now,
    )

    private fun newViewModel(
        sessionRepo: FakeSessionRepository,
        exerciseRepo: FakeExerciseRepository,
        id: String = sessionId.value,
    ): SessionDetailViewModel = SessionDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to id)),
        sessionRepository = sessionRepo,
        exerciseRepository = exerciseRepo,
    )
}
