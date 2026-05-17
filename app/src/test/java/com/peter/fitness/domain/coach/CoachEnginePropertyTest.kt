package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import com.peter.fitness.domain.plates.PlateCalculator
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Property-based invariants for the Coach engine. These complement the example/golden tests by
 * asserting properties hold across thousands of random states.
 */
class CoachEnginePropertyTest {

    private val inventory = EquipmentInventory(
        barKg = 20.0,
        hasRack = true,
        hasBench = true,
        hasPullUpBar = false,
        plates = listOf(
            PlatePair(25.0, 4),
            PlatePair(20.0, 4),
            PlatePair(10.0, 4),
            PlatePair(5.0, 4),
            PlatePair(2.5, 4),
            PlatePair(1.25, 2),
        ),
        updatedAt = Instant.EPOCH,
    )

    private val arbState: Arb<ProgressionState> = arbitrary {
        val minReps = Arb.int(3..6).bind()
        val maxReps = minReps + Arb.int(0..6).bind()
        val reps = Arb.int(minReps..maxReps).bind()
        // Start from a load the inventory can actually load. A volume bump carries this load
        // through unchanged by design (one variable at a time), so an off-grid starting load
        // would surface as a "phantom" the engine never had a chance to snap.
        val load = PlateCalculator.snap(Arb.int(20..200).bind().toDouble(), inventory).achievableKg
        ProgressionState(
            currentLoadKg = load,
            currentTargetReps = reps,
            workingRangeMinReps = minReps,
            workingRangeMaxReps = maxReps,
        )
    }

    private val arbClass: Arb<ExerciseClass> = Arb.element(ExerciseClass.entries)

    private fun successSets(state: ProgressionState): List<CompletedSet> = List(3) {
        CompletedSet(
            targetReps = state.currentTargetReps,
            targetLoadKg = state.currentLoadKg,
            completedReps = state.currentTargetReps,
            performedLoadKg = state.currentLoadKg,
            subjectiveLoad = SubjectiveLoad.OK,
            techniqueRating = TechniqueRating.GOOD,
        )
    }

    @Test
    fun `volume-before-intensity - load never rises while reps are below the ceiling`() = runTest {
        checkAll(arbState, arbClass) { state, exerciseClass ->
            val decision = CoachEngine.decideNext(
                state,
                successSets(state),
                exerciseClass,
                CoachPolicy.DEFAULT,
                inventory,
            )
            if (state.currentTargetReps < state.workingRangeMaxReps) {
                decision.prescription.targetLoadKg shouldBe state.currentLoadKg
                decision.stimulusChanged shouldBe Stimulus.VOLUME
            }
        }
    }

    @Test
    fun `no phantom load - every prescribed load is reachable by the inventory`() = runTest {
        checkAll(arbState, arbClass) { state, exerciseClass ->
            val decision = CoachEngine.decideNext(
                state,
                successSets(state),
                exerciseClass,
                CoachPolicy.DEFAULT,
                inventory,
            )
            val resnapped = PlateCalculator.snap(decision.prescription.targetLoadKg, inventory).achievableKg
            resnapped shouldBe decision.prescription.targetLoadKg
        }
    }

    @Test
    fun `monotonic regression - a much-too-heavy-triggered deload never raises the load`() = runTest {
        checkAll(arbState, arbClass) { baseState, exerciseClass ->
            // Prime so a single MUCH_TOO_HEAVY trips the consecutive trigger.
            val primed = baseState.copy(consecutiveMuchTooHeavy = 1)
            val heavySets = List(3) {
                CompletedSet(
                    targetReps = primed.currentTargetReps,
                    targetLoadKg = primed.currentLoadKg,
                    completedReps = primed.currentTargetReps,
                    performedLoadKg = primed.currentLoadKg,
                    subjectiveLoad = SubjectiveLoad.MUCH_TOO_HEAVY,
                    techniqueRating = TechniqueRating.GOOD,
                )
            }
            val decision = CoachEngine.decideNext(
                primed,
                heavySets,
                exerciseClass,
                CoachPolicy.DEFAULT,
                inventory,
            )
            decision.deloaded shouldBe true
            decision.prescription.targetLoadKg shouldBeLessThanOrEqualTo primed.currentLoadKg
        }
    }
}
