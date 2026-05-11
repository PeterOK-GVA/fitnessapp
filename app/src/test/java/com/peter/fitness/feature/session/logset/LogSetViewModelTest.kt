package com.peter.fitness.feature.session.logset

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.peter.fitness.core.ids.IdFactory
import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueDemand
import com.peter.fitness.domain.model.TechniqueRating
import com.peter.fitness.domain.usecase.StartRestTimerUseCase
import com.peter.fitness.testsupport.FakeEquipmentInventoryRepository
import com.peter.fitness.testsupport.FakeExerciseRepository
import com.peter.fitness.testsupport.FakeRestTimerRepository
import com.peter.fitness.testsupport.FakeRestTimerServiceController
import com.peter.fitness.testsupport.FakeSessionRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.nulls.shouldBeNull
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
class LogSetViewModelTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val now: Instant = Instant.parse("2026-05-10T08:00:00Z")
    private val sessionId = SessionId("session-1")
    private val exerciseId = ExerciseId("back-squat")

    private val seedSession = Session(
        id = sessionId,
        startedAt = now,
        endedAt = null,
        focus = SessionFocus.FREE_LOG,
        notes = null,
    )

    private val seedExercise = Exercise(
        id = exerciseId,
        name = "Back Squat",
        movementPattern = MovementPattern.SQUAT,
        loadType = LoadType.BARBELL,
        techniqueDemand = TechniqueDemand.HIGH,
        conditioningSuitability = ConditioningSuitability.LIMITED,
        requiresRack = true,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `add mode loads exercise name and starts with empty form`() = runTest(main.dispatcher) {
        val vm = newViewModel(addModeSavedState())
        advanceUntilIdle()

        val state = vm.uiState.value
        state.isLoading shouldBe false
        state.isEditMode shouldBe false
        state.exerciseId shouldBe exerciseId.value
        state.exerciseName shouldBe "Back Squat"
        state.reps shouldBe ""
        state.loadKg shouldBe ""
    }

    @Test
    fun `save with invalid reps shows error and does not persist`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val vm = newViewModel(addModeSavedState(), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.onRepsChange("0")
        vm.onLoadKgChange("80")
        vm.onSave()
        advanceUntilIdle()

        vm.uiState.value.repsError.shouldNotBeNull()
        sessionRepo.observeSetEntries(sessionId).test {
            awaitItem() shouldBe emptyList()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valid add saves new SetEntry and emits Saved event`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val vm = newViewModel(addModeSavedState(), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onRepsChange("5")
            vm.onLoadKgChange("80")
            vm.onSubjectiveLoadChange(SubjectiveLoad.OK)
            vm.onTechniqueRatingChange(TechniqueRating.GOOD)
            vm.onSave()

            awaitItem() shouldBe LogSetEvent.Saved
            cancelAndIgnoreRemainingEvents()
        }

        sessionRepo.observeSetEntries(sessionId).test {
            val entries = awaitItem()
            entries.size shouldBe 1
            val saved = entries.single()
            saved.targetReps shouldBe 5
            saved.targetLoadKg shouldBe 80.0
            saved.completedReps shouldBe 5
            saved.performedLoadKg shouldBe 80.0
            saved.subjectiveLoad shouldBe SubjectiveLoad.OK
            saved.techniqueRating shouldBe TechniqueRating.GOOD
            saved.ordinal shouldBe 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `add mode appends with next ordinal`() = runTest(main.dispatcher) {
        val existing = SetEntry(
            id = SetEntryId("existing-1"),
            sessionId = sessionId,
            exerciseId = exerciseId,
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 5,
            performedLoadKg = 80.0,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
            createdAt = now,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(existing),
        )
        val vm = newViewModel(addModeSavedState(), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.onRepsChange("5")
        vm.onLoadKgChange("85")
        vm.onSave()
        advanceUntilIdle()

        sessionRepo.observeSetEntries(sessionId).test {
            val entries = awaitItem()
            entries.size shouldBe 2
            entries.map { it.ordinal }.sorted() shouldBe listOf(0, 1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit mode loads existing values into form`() = runTest(main.dispatcher) {
        val existing = SetEntry(
            id = SetEntryId("set-1"),
            sessionId = sessionId,
            exerciseId = exerciseId,
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 4,
            performedLoadKg = 75.0,
            subjectiveLoad = SubjectiveLoad.TOO_HEAVY,
            techniqueRating = TechniqueRating.OK,
            createdAt = now,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(existing),
        )
        val vm = newViewModel(editModeSavedState("set-1"), sessionRepo = sessionRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.isLoading shouldBe false
        state.isEditMode shouldBe true
        state.exerciseName shouldBe "Back Squat"
        state.reps shouldBe "4"
        state.loadKg shouldBe "75"
        state.subjectiveLoad shouldBe SubjectiveLoad.TOO_HEAVY
        state.techniqueRating shouldBe TechniqueRating.OK
    }

    @Test
    fun `edit mode save updates existing entry and emits Saved`() = runTest(main.dispatcher) {
        val existing = SetEntry(
            id = SetEntryId("set-1"),
            sessionId = sessionId,
            exerciseId = exerciseId,
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 5,
            performedLoadKg = 80.0,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
            createdAt = now,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(existing),
        )
        val vm = newViewModel(editModeSavedState("set-1"), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onRepsChange("3")
            vm.onLoadKgChange("85")
            vm.onSubjectiveLoadChange(SubjectiveLoad.MUCH_TOO_HEAVY)
            vm.onSave()

            awaitItem() shouldBe LogSetEvent.Saved
            cancelAndIgnoreRemainingEvents()
        }

        sessionRepo.observeSetEntries(sessionId).test {
            val entries = awaitItem()
            entries.size shouldBe 1
            val updated = entries.single()
            updated.id shouldBe SetEntryId("set-1")
            updated.completedReps shouldBe 3
            updated.performedLoadKg shouldBe 85.0
            updated.subjectiveLoad shouldBe SubjectiveLoad.MUCH_TOO_HEAVY
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit mode delete removes entry and emits Deleted`() = runTest(main.dispatcher) {
        val existing = SetEntry(
            id = SetEntryId("set-1"),
            sessionId = sessionId,
            exerciseId = exerciseId,
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 5,
            performedLoadKg = 80.0,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
            createdAt = now,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(existing),
        )
        val vm = newViewModel(editModeSavedState("set-1"), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onDelete()
            awaitItem() shouldBe LogSetEvent.Deleted
            cancelAndIgnoreRemainingEvents()
        }

        sessionRepo.observeSetEntries(sessionId).test {
            awaitItem() shouldBe emptyList()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit mode with missing entry sets notFound`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val vm = newViewModel(editModeSavedState("ghost"), sessionRepo = sessionRepo)
        advanceUntilIdle()

        vm.uiState.value.notFound shouldBe true
        vm.uiState.value.isLoading shouldBe false
    }

    @Test
    fun `add mode does not pre-populate errors`() = runTest(main.dispatcher) {
        val vm = newViewModel(addModeSavedState())
        advanceUntilIdle()

        vm.uiState.value.repsError.shouldBeNull()
        vm.uiState.value.loadKgError.shouldBeNull()
    }

    @Test
    fun `valid load surfaces an on-target plate hint`() = runTest(main.dispatcher) {
        val vm = newViewModel(addModeSavedState())
        advanceUntilIdle()

        vm.onLoadKgChange("80")
        val hint = vm.uiState.value.plateHint
        hint.shouldNotBeNull()
        hint.isOnTarget shouldBe true
        hint.achievableText shouldBe "Snaps to 80 kg"
        hint.perSideText shouldBe "Per side: 1 × 20 kg + 1 × 10 kg"
    }

    @Test
    fun `unreachable load surfaces an off-target hint with deviation`() = runTest(main.dispatcher) {
        val limitedInventory = EquipmentInventory(
            barKg = 20.0,
            hasRack = true,
            hasBench = true,
            hasPullUpBar = false,
            plates = listOf(PlatePair(20.0, 2), PlatePair(10.0, 2)),
            updatedAt = now,
        )
        val vm = newViewModel(
            addModeSavedState(),
            equipmentRepo = FakeEquipmentInventoryRepository(limitedInventory),
        )
        advanceUntilIdle()

        vm.onLoadKgChange("85")
        val hint = vm.uiState.value.plateHint
        hint.shouldNotBeNull()
        hint.isOnTarget shouldBe false
        hint.achievableText shouldBe "Snaps to 80 kg (-5 kg)"
    }

    @Test
    fun `bar-weight load shows Bar only hint`() = runTest(main.dispatcher) {
        val vm = newViewModel(addModeSavedState())
        advanceUntilIdle()

        vm.onLoadKgChange("20")
        val hint = vm.uiState.value.plateHint
        hint.shouldNotBeNull()
        hint.isOnTarget shouldBe true
        hint.perSideText shouldBe "Bar only"
    }

    @Test
    fun `empty load does not surface a hint`() = runTest(main.dispatcher) {
        val vm = newViewModel(addModeSavedState())
        advanceUntilIdle()

        vm.uiState.value.plateHint.shouldBeNull()
    }

    @Test
    fun `onSaveAndRest persists set and starts a rest timer with the new set id`() = runTest(main.dispatcher) {
        val sessionRepo = FakeSessionRepository(initialSessions = listOf(seedSession))
        val restRepo = FakeRestTimerRepository()
        val controller = FakeRestTimerServiceController()
        val vm = newViewModel(
            addModeSavedState(),
            sessionRepo = sessionRepo,
            restRepo = restRepo,
            controller = controller,
        )
        advanceUntilIdle()

        vm.onRepsChange("5")
        vm.onLoadKgChange("80")
        vm.onSaveAndRest()
        advanceUntilIdle()

        sessionRepo.observeSetEntries(sessionId).test {
            awaitItem().size shouldBe 1
            cancelAndIgnoreRemainingEvents()
        }
        val timer = restRepo.current()
        timer.shouldNotBeNull()
        timer.sessionId shouldBe sessionId
        timer.setEntryId shouldBe SetEntryId("new-set-id")
        controller.startCount shouldBe 1
    }

    @Test
    fun `onSaveAndRest validates same as onSave and does not start timer on invalid input`() =
        runTest(main.dispatcher) {
            val restRepo = FakeRestTimerRepository()
            val controller = FakeRestTimerServiceController()
            val vm = newViewModel(
                addModeSavedState(),
                restRepo = restRepo,
                controller = controller,
            )
            advanceUntilIdle()

            vm.onRepsChange("0")
            vm.onLoadKgChange("80")
            vm.onSaveAndRest()
            advanceUntilIdle()

            vm.uiState.value.repsError.shouldNotBeNull()
            restRepo.current().shouldBeNull()
            controller.startCount shouldBe 0
        }

    @Test
    fun `edit mode hint is computed from prefilled load`() = runTest(main.dispatcher) {
        val existing = SetEntry(
            id = SetEntryId("set-1"),
            sessionId = sessionId,
            exerciseId = exerciseId,
            ordinal = 0,
            targetReps = 5,
            targetLoadKg = 80.0,
            completedReps = 5,
            performedLoadKg = 80.0,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
            createdAt = now,
        )
        val sessionRepo = FakeSessionRepository(
            initialSessions = listOf(seedSession),
            initialSets = listOf(existing),
        )
        val vm = newViewModel(editModeSavedState("set-1"), sessionRepo = sessionRepo)
        advanceUntilIdle()

        val hint = vm.uiState.value.plateHint
        hint.shouldNotBeNull()
        hint.isOnTarget shouldBe true
        hint.perSideText shouldBe "Per side: 1 × 20 kg + 1 × 10 kg"
    }

    private fun addModeSavedState(): SavedStateHandle = SavedStateHandle(
        mapOf("sessionId" to sessionId.value, "exerciseId" to exerciseId.value),
    )

    private fun editModeSavedState(setEntryId: String): SavedStateHandle = SavedStateHandle(
        mapOf("sessionId" to sessionId.value, "setEntryId" to setEntryId),
    )

    private fun defaultInventory(): EquipmentInventory = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = listOf(
            PlatePair(20.0, 4),
            PlatePair(15.0, 2),
            PlatePair(10.0, 4),
            PlatePair(5.0, 4),
            PlatePair(2.5, 4),
            PlatePair(1.25, 2),
        ),
        updatedAt = now,
    )

    private fun newViewModel(
        savedStateHandle: SavedStateHandle,
        sessionRepo: FakeSessionRepository = FakeSessionRepository(initialSessions = listOf(seedSession)),
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(initial = listOf(seedExercise)),
        equipmentRepo: FakeEquipmentInventoryRepository = FakeEquipmentInventoryRepository(defaultInventory()),
        idFactory: IdFactory = FixedIdFactory(SetEntryId("new-set-id")),
        restRepo: FakeRestTimerRepository = FakeRestTimerRepository(),
        controller: FakeRestTimerServiceController = FakeRestTimerServiceController(),
    ): LogSetViewModel = LogSetViewModel(
        savedStateHandle = savedStateHandle,
        sessionRepository = sessionRepo,
        exerciseRepository = exerciseRepo,
        equipmentRepository = equipmentRepo,
        idFactory = idFactory,
        clock = Clock.fixed(now, ZoneOffset.UTC),
        startRestTimer = StartRestTimerUseCase(
            repository = restRepo,
            controller = controller,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        ),
    )

    private class FixedIdFactory(private val setEntryId: SetEntryId) : IdFactory {
        override fun newSessionId(): SessionId = SessionId("unused")
        override fun newSetEntryId(): SetEntryId = setEntryId
    }
}
