package com.peter.fitness.feature.session

import androidx.lifecycle.SavedStateHandle
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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val fixedInstant: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val finishInstant: Instant = Instant.parse("2026-05-10T09:30:00Z")

    private val seedSession = Session(
        id = SessionId("session-1"),
        startedAt = fixedInstant,
        endedAt = null,
        focus = SessionFocus.FREE_LOG,
        notes = null,
    )

    private val seedExercise = Exercise(
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
    fun `init loads session metadata`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val exerciseRepo = FakeExerciseRepository(initial = listOf(seedExercise))
        val vm = newViewModel(sessionRepo, exerciseRepo, sessionId = "session-1")

        advanceUntilIdle()

        val state = vm.uiState.value
        state.isLoading shouldBe false
        state.sessionMissing shouldBe false
        state.startedAt shouldBe fixedInstant
        state.endedAt shouldBe null
        state.focus shouldBe SessionFocus.FREE_LOG
    }

    @Test
    fun `init with missing session sets sessionMissing flag`() = runTest(main.dispatcher) {
        val vm = newViewModel(
            FakeSessionRepository(),
            FakeExerciseRepository(),
            sessionId = "ghost",
        )

        advanceUntilIdle()

        vm.uiState.value.sessionMissing shouldBe true
        vm.uiState.value.isLoading shouldBe false
    }

    @Test
    fun `set entries are surfaced with exercise names`() = runTest(main.dispatcher) {
        val set = SetEntry(
            id = SetEntryId("set-1"),
            sessionId = SessionId("session-1"),
            exerciseId = ExerciseId("back-squat"),
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 5,
            performedLoadKg = 80.0,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
            createdAt = fixedInstant,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(set),
        )
        val exerciseRepo = FakeExerciseRepository(initial = listOf(seedExercise))
        val vm = newViewModel(sessionRepo, exerciseRepo, sessionId = "session-1")

        advanceUntilIdle()

        val sets = vm.uiState.value.sets
        sets.size shouldBe 1
        sets[0].exerciseName shouldBe "Back Squat"
        sets[0].targetReps shouldBe 5
        sets[0].targetLoadKg shouldBe 80.0
    }

    @Test
    fun `finishSession persists endedAt and marks finished`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val exerciseRepo = FakeExerciseRepository()
        val vm = newViewModel(
            sessionRepo,
            exerciseRepo,
            sessionId = "session-1",
            clock = Clock.fixed(finishInstant, ZoneOffset.UTC),
        )

        advanceUntilIdle()
        vm.onFinishSession()
        advanceUntilIdle()

        sessionRepo.updateCount shouldBe 1
        val persisted = sessionRepo.findById(SessionId("session-1"))
        persisted.shouldNotBeNull()
        persisted.endedAt shouldBe finishInstant
        vm.uiState.value.isFinished shouldBe true
        vm.uiState.value.endedAt shouldBe finishInstant
    }

    @Test
    fun `finishSession ignored when already finishing`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val vm = newViewModel(
            sessionRepo,
            FakeExerciseRepository(),
            sessionId = "session-1",
            clock = Clock.fixed(finishInstant, ZoneOffset.UTC),
        )

        advanceUntilIdle()
        vm.onFinishSession()
        vm.onFinishSession()
        advanceUntilIdle()

        sessionRepo.updateCount shouldBe 1
    }

    private fun newViewModel(
        sessionRepo: FakeSessionRepository,
        exerciseRepo: FakeExerciseRepository,
        sessionId: String,
        clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
    ): ActiveSessionViewModel {
        val handle = SavedStateHandle(mapOf("sessionId" to sessionId))
        return ActiveSessionViewModel(
            savedStateHandle = handle,
            sessionRepository = sessionRepo,
            exerciseRepository = exerciseRepo,
            clock = clock,
        )
    }
}
