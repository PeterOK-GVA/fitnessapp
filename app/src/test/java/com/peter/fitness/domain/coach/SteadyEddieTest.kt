package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Golden-master for "Steady Eddie": an athlete who always completes the prescribed reps, always
 * reports OK feedback, and uses a full inventory. The snapshot below locks in the v1 engine's
 * volume-before-intensity behaviour across 8 consecutive sessions. Any intentional policy change
 * has to re-record this list, which surfaces the impact in code review.
 */
class SteadyEddieTest {

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

    @Test
    fun `Steady Eddie progresses via volume-before-intensity across 8 sessions`() {
        var state = ProgressionState(
            currentLoadKg = 60.0,
            currentTargetReps = 5,
            workingRangeMinReps = 5,
            workingRangeMaxReps = 8,
        )
        val trail = mutableListOf<String>()

        repeat(SESSIONS) { sessionIndex ->
            val results = if (sessionIndex == 0) {
                emptyList()
            } else {
                List(SETS_PER_SESSION) {
                    CompletedSet(
                        targetReps = state.currentTargetReps,
                        targetLoadKg = state.currentLoadKg,
                        completedReps = state.currentTargetReps,
                        performedLoadKg = state.currentLoadKg,
                        subjectiveLoad = SubjectiveLoad.OK,
                        techniqueRating = TechniqueRating.GOOD,
                    )
                }
            }
            val decision = CoachEngine.decideNext(state, results, CoachPolicy.DEFAULT, inventory)
            trail += "${decision.prescription.targetReps}x${formatKg(decision.prescription.targetLoadKg)}"
            state = decision.newState
        }

        trail shouldContainExactly listOf(
            "5x60",
            "6x60",
            "7x60",
            "8x60",
            "5x62.5",
            "6x62.5",
            "7x62.5",
            "8x62.5",
        )
    }

    private fun formatKg(kg: Double): String =
        if (kg == kg.toLong().toDouble()) "${kg.toLong()}" else "$kg"

    private companion object {
        const val SESSIONS = 8
        const val SETS_PER_SESSION = 3
    }
}
