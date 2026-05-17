package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.coach.ProgressionState
import com.peter.fitness.domain.coach.Stimulus
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
import com.peter.fitness.testsupport.FakeEquipmentInventoryRepository
import com.peter.fitness.testsupport.FakeExerciseRepository
import com.peter.fitness.testsupport.FakeProgressionStateRepository
import com.peter.fitness.testsupport.FakeSessionRepository
import com.peter.fitness.testsupport.MainDispatcherExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProposeNextSetUseCaseTest {

    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val exerciseId = ExerciseId("back-squat")
    private val currentSession = SessionId("current")

    private val inventory = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = listOf(
            PlatePair(20.0, 4),
            PlatePair(10.0, 4),
            PlatePair(5.0, 4),
            PlatePair(2.5, 4),
            PlatePair(1.25, 2),
        ),
        updatedAt = Instant.EPOCH,
    )

    private fun state(reps: Int = 5, load: Double = 80.0) = ProgressionState(
        currentLoadKg = load,
        currentTargetReps = reps,
        workingRangeMinReps = 5,
        workingRangeMaxReps = 8,
    )

    private fun session(id: String, startedAt: Instant) =
        Session(SessionId(id), startedAt, endedAt = null, focus = SessionFocus.FREE_LOG, notes = null)

    private fun set(
        id: String,
        sessionId: String,
        exId: ExerciseId = exerciseId,
        reps: Int,
        load: Double,
        feedback: SubjectiveLoad? = SubjectiveLoad.OK,
    ) = SetEntry(
        id = SetEntryId(id),
        sessionId = SessionId(sessionId),
        exerciseId = exId,
        ordinal = 0,
        targetReps = reps,
        targetLoadKg = load,
        completedReps = reps,
        performedLoadKg = load,
        subjectiveLoad = feedback,
        techniqueRating = TechniqueRating.GOOD,
        createdAt = Instant.EPOCH,
    )

    private val backSquat = Exercise(
        id = exerciseId,
        name = "Back Squat",
        movementPattern = MovementPattern.SQUAT,
        loadType = LoadType.BARBELL,
        techniqueDemand = TechniqueDemand.HIGH,
        conditioningSuitability = ConditioningSuitability.LIMITED,
        createdAt = Instant.EPOCH,
    )

    private fun useCase(
        progression: FakeProgressionStateRepository,
        sessions: FakeSessionRepository,
    ) = ProposeNextSetUseCase(
        progressionStateRepository = progression,
        sessionRepository = sessions,
        exerciseRepository = FakeExerciseRepository(initial = listOf(backSquat)),
        equipmentRepository = FakeEquipmentInventoryRepository(inventory),
    )

    @Test
    fun `returns null when no progression state exists`() = runTest(main.dispatcher) {
        val proposal = useCase(
            FakeProgressionStateRepository(),
            FakeSessionRepository(),
        )(exerciseId, currentSession)
        proposal.shouldBeNull()
    }

    @Test
    fun `with state but no prior sessions keeps the current prescription`() = runTest(main.dispatcher) {
        val proposal = useCase(
            FakeProgressionStateRepository(mapOf(exerciseId to state(reps = 5, load = 80.0))),
            FakeSessionRepository(),
        )(exerciseId, currentSession)

        proposal.shouldNotBeNull()
        proposal.prescription.targetReps shouldBe 5
        proposal.prescription.targetLoadKg shouldBe 80.0
    }

    @Test
    fun `prior OK session below the ceiling proposes a volume bump`() = runTest(main.dispatcher) {
        val sessions = FakeSessionRepository(
            initialSessions = listOf(session("prev", Instant.parse("2026-05-10T08:00:00Z"))),
            initialSets = List(3) { i ->
                set("p$i", "prev", reps = 5, load = 80.0, feedback = SubjectiveLoad.OK)
            },
        )
        val proposal = useCase(
            FakeProgressionStateRepository(mapOf(exerciseId to state(reps = 5, load = 80.0))),
            sessions,
        )(exerciseId, currentSession)

        proposal.shouldNotBeNull()
        proposal.prescription.targetReps shouldBe 6
        proposal.prescription.targetLoadKg shouldBe 80.0
        proposal.newState.lastStimulus shouldBe Stimulus.VOLUME
    }

    @Test
    fun `sets logged in the current session are ignored when deciding`() = runTest(main.dispatcher) {
        val sessions = FakeSessionRepository(
            initialSessions = listOf(
                session("prev", Instant.parse("2026-05-10T08:00:00Z")),
                session(currentSession.value, Instant.parse("2026-05-12T08:00:00Z")),
            ),
            initialSets = List(3) { i -> set("p$i", "prev", reps = 5, load = 80.0) } +
                // A heavy set already logged in the current session must NOT affect the decision.
                set("c0", currentSession.value, reps = 8, load = 120.0, feedback = SubjectiveLoad.MUCH_TOO_HEAVY),
        )
        val proposal = useCase(
            FakeProgressionStateRepository(mapOf(exerciseId to state(reps = 5, load = 80.0))),
            sessions,
        )(exerciseId, currentSession)

        proposal.shouldNotBeNull()
        // Decision is driven by "prev" (OK at 5x80) → volume bump, not the current session's heavy set.
        proposal.prescription.targetReps shouldBe 6
        proposal.prescription.targetLoadKg shouldBe 80.0
    }

    @Test
    fun `the most recent prior session is the one fed to the engine`() = runTest(main.dispatcher) {
        val sessions = FakeSessionRepository(
            initialSessions = listOf(
                session("old", Instant.parse("2026-05-01T08:00:00Z")),
                session("recent", Instant.parse("2026-05-10T08:00:00Z")),
            ),
            initialSets = List(3) { i -> set("o$i", "old", reps = 8, load = 80.0, feedback = SubjectiveLoad.OK) } +
                List(3) { i -> set("r$i", "recent", reps = 5, load = 80.0, feedback = SubjectiveLoad.OK) },
        )
        val proposal = useCase(
            FakeProgressionStateRepository(mapOf(exerciseId to state(reps = 5, load = 80.0))),
            sessions,
        )(exerciseId, currentSession)

        proposal.shouldNotBeNull()
        // Recent session was 5x80 OK below ceiling → volume bump to 6, not the intensity reset that
        // the older 8x80 session would have triggered.
        proposal.prescription.targetReps shouldBe 6
        proposal.prescription.targetLoadKg shouldBe 80.0
    }
}
