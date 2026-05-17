package com.peter.fitness.domain.coach

import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Golden-master for "Crash-and-burn Cara": a lower-body squatter who builds for a few sessions
 * then stalls hard with consecutive much-too-heavy feedback, tripping the deload. Locks in the
 * v2 deload behaviour. The feedback script is fixed; the prescription trail is the snapshot.
 */
class CrashAndBurnCaraTest {

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

    private fun results(state: ProgressionState, feedback: SubjectiveLoad, completed: Int) =
        List(SETS) {
            CompletedSet(
                targetReps = state.currentTargetReps,
                targetLoadKg = state.currentLoadKg,
                completedReps = completed,
                performedLoadKg = state.currentLoadKg,
                subjectiveLoad = feedback,
                techniqueRating = TechniqueRating.GOOD,
            )
        }

    @Test
    fun `Cara builds then deloads on consecutive much-too-heavy`() {
        var state = ProgressionState(
            currentLoadKg = 100.0,
            currentTargetReps = 8,
            workingRangeMinReps = 5,
            workingRangeMaxReps = 8,
        )
        // Script of how the *previous* session went, fed into each decision.
        val feedbackScript = listOf(
            null, // session 0: no history
            SubjectiveLoad.OK, // 1: at ceiling, OK -> intensity bump
            SubjectiveLoad.MUCH_TOO_HEAVY, // 2: first MTH -> hold, counter +2
            SubjectiveLoad.MUCH_TOO_HEAVY, // 3: second consecutive MTH -> deload
            SubjectiveLoad.OK, // 4: recovering, below ceiling -> volume
        )
        val trail = mutableListOf<String>()
        val deloadFlags = mutableListOf<Boolean>()

        feedbackScript.forEach { feedback ->
            val lastResults = if (feedback == null) {
                emptyList()
            } else {
                val completed = if (feedback == SubjectiveLoad.MUCH_TOO_HEAVY) {
                    state.currentTargetReps
                } else {
                    state.currentTargetReps
                }
                results(state, feedback, completed)
            }
            val decision = CoachEngine.decideNext(
                state,
                lastResults,
                ExerciseClass.LOWER_COMPOUND,
                CoachPolicy.DEFAULT,
                inventory,
            )
            trail += "${decision.prescription.targetReps}x${fmt(decision.prescription.targetLoadKg)}"
            deloadFlags += decision.deloaded
            state = decision.newState
        }

        trail shouldContainExactly listOf(
            "8x100", // 0: keep (first session)
            "5x105", // 1: ceiling + OK -> +5 kg lower-body, reset reps
            "5x105", // 2: MTH -> hold
            "5x95", // 3: 2nd consecutive MTH -> deload 105 * 0.9 = 94.5 -> snaps to 95
            "6x95", // 4: OK below ceiling -> volume bump
        )
        deloadFlags shouldContainExactly listOf(false, false, false, true, false)
        state.deloadCounter shouldBe 0
    }

    private fun fmt(kg: Double): String =
        if (kg == kg.toLong().toDouble()) "${kg.toLong()}" else "$kg"

    private companion object {
        const val SETS = 3
    }
}
