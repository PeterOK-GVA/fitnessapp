package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class CoachEngineTest {

    private val standardInventory = EquipmentInventory(
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

    private fun startingState(
        loadKg: Double = 80.0,
        reps: Int = 5,
        minReps: Int = 5,
        maxReps: Int = 8,
    ): ProgressionState = ProgressionState(
        currentLoadKg = loadKg,
        currentTargetReps = reps,
        workingRangeMinReps = minReps,
        workingRangeMaxReps = maxReps,
    )

    private fun set(
        target: Int,
        completed: Int,
        load: Double,
        feedback: SubjectiveLoad? = SubjectiveLoad.OK,
    ): CompletedSet = CompletedSet(
        targetReps = target,
        targetLoadKg = load,
        completedReps = completed,
        performedLoadKg = load,
        subjectiveLoad = feedback,
        techniqueRating = TechniqueRating.GOOD,
    )

    @Test
    fun `first session keeps prescription unchanged`() {
        val state = startingState()
        val decision = CoachEngine.decideNext(state, emptyList(), CoachPolicy.DEFAULT, standardInventory)
        decision.prescription shouldBe Prescription(targetReps = 5, targetLoadKg = 80.0)
        decision.newState shouldBe state
        decision.stimulusChanged shouldBe null
    }

    @Test
    fun `met + OK below ceiling adds one rep (volume)`() {
        val state = startingState(reps = 5)
        val results = List(3) { set(target = 5, completed = 5, load = 80.0) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.stimulusChanged shouldBe Stimulus.VOLUME
        decision.prescription.targetReps shouldBe 6
        decision.prescription.targetLoadKg shouldBe 80.0
        decision.newState.volumeStreak shouldBe 1
    }

    @Test
    fun `met + OK at ceiling bumps intensity and resets reps to floor`() {
        val state = startingState(reps = 8)
        val results = List(3) { set(target = 8, completed = 8, load = 80.0) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.stimulusChanged shouldBe Stimulus.INTENSITY
        decision.prescription.targetReps shouldBe 5
        decision.prescription.targetLoadKg shouldBe 82.5
        decision.newState.volumeStreak shouldBe 0
    }

    @Test
    fun `too-light feedback escalates to large intensity step at the ceiling`() {
        val state = startingState(reps = 8)
        val results = List(3) { set(target = 8, completed = 8, load = 80.0, feedback = SubjectiveLoad.TOO_LIGHT) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.prescription.targetLoadKg shouldBe 85.0
    }

    @Test
    fun `too-light feedback below ceiling bumps two reps`() {
        val state = startingState(reps = 5)
        val results = List(3) { set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.TOO_LIGHT) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.stimulusChanged shouldBe Stimulus.VOLUME
        decision.prescription.targetReps shouldBe 7
    }

    @Test
    fun `too-heavy feedback holds the prescription and resets the success streak`() {
        val state = startingState(reps = 5).copy(consecutiveSuccesses = 3)
        val results = List(3) { set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.TOO_HEAVY) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.stimulusChanged shouldBe null
        decision.prescription shouldBe Prescription(5, 80.0)
        decision.newState.consecutiveSuccesses shouldBe 0
    }

    @Test
    fun `missing the rep target on any set holds even if feedback is OK`() {
        val state = startingState(reps = 5)
        val results = listOf(
            set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.OK),
            set(target = 5, completed = 3, load = 80.0, feedback = SubjectiveLoad.OK),
            set(target = 5, completed = 2, load = 80.0, feedback = SubjectiveLoad.OK),
        )
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, standardInventory)
        decision.stimulusChanged shouldBe null
        decision.gate shouldBe SetCompletion.MISSED
    }

    @Test
    fun `worst feedback across the session is selected`() {
        val results = listOf(
            set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.OK),
            set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.TOO_LIGHT),
            set(target = 5, completed = 5, load = 80.0, feedback = SubjectiveLoad.MUCH_TOO_HEAVY),
        )
        CoachEngine.worstFeedback(results) shouldBe SubjectiveLoad.MUCH_TOO_HEAVY
    }

    @Test
    fun `intensity bump snaps target load to the inventory's reachable set`() {
        val sparseInventory = standardInventory.copy(
            plates = listOf(PlatePair(20.0, 2), PlatePair(10.0, 2), PlatePair(5.0, 2)),
        )
        val state = startingState(loadKg = 80.0, reps = 8)
        val results = List(3) { set(target = 8, completed = 8, load = 80.0, feedback = SubjectiveLoad.TOO_LIGHT) }
        val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, sparseInventory)
        // Target was 85; with no 2.5kg or 1.25kg plates, nearest is 80 or 90. Snap should pick one.
        val achievable = decision.prescription.targetLoadKg
        (achievable == 80.0 || achievable == 90.0) shouldBe true
    }
}
