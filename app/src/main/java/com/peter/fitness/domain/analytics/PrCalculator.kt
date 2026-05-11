package com.peter.fitness.domain.analytics

import com.peter.fitness.domain.model.Exercise
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.SetEntry
import java.time.Instant

data class PrSummary(
    val exerciseId: ExerciseId,
    val exerciseName: String,
    val totalSets: Int,
    val heaviestSet: HeaviestSet?,
    val bestE1RM: BestE1RM?,
)

data class HeaviestSet(
    val loadKg: Double,
    val reps: Int,
    val performedAt: Instant,
)

data class BestE1RM(
    val estimatedKg: Double,
    val sourceLoadKg: Double,
    val sourceReps: Int,
    val performedAt: Instant,
)

object PrCalculator {

    /**
     * Epley one-rep-max estimate: `load × (1 + reps/30)`. For a true single (reps=1) it returns
     * slightly above the lifted load (~1.033×), which is the standard Epley behaviour.
     */
    fun epleyE1RM(loadKg: Double, reps: Int): Double = loadKg * (1.0 + reps / EPLEY_DENOMINATOR)

    /**
     * Compute a PR summary for every exercise that has at least one completed set in `sets`.
     * Sets without `performedLoadKg` or `completedReps` (or with reps < 1) are excluded.
     * Result is sorted by `totalSets` descending — most-trained exercises first.
     */
    fun summaries(sets: List<SetEntry>, exercises: List<Exercise>): List<PrSummary> {
        val exercisesById: Map<ExerciseId, Exercise> = exercises.associateBy { it.id }
        val byExercise: Map<ExerciseId, List<CompletedSet>> = sets
            .mapNotNull { it.toCompleted() }
            .groupBy { it.exerciseId }
        return byExercise.mapNotNull { (exerciseId, completed) ->
            val exercise = exercisesById[exerciseId] ?: return@mapNotNull null
            buildSummary(exercise, completed)
        }.sortedByDescending { it.totalSets }
    }

    private fun buildSummary(exercise: Exercise, completed: List<CompletedSet>): PrSummary {
        val heaviest = completed.maxByOrNull { it.loadKg }?.let { set ->
            HeaviestSet(
                loadKg = set.loadKg,
                reps = set.reps,
                performedAt = set.performedAt,
            )
        }
        val best = completed.maxByOrNull { epleyE1RM(it.loadKg, it.reps) }?.let { set ->
            BestE1RM(
                estimatedKg = epleyE1RM(set.loadKg, set.reps),
                sourceLoadKg = set.loadKg,
                sourceReps = set.reps,
                performedAt = set.performedAt,
            )
        }
        return PrSummary(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            totalSets = completed.size,
            heaviestSet = heaviest,
            bestE1RM = best,
        )
    }

    private data class CompletedSet(
        val exerciseId: ExerciseId,
        val loadKg: Double,
        val reps: Int,
        val performedAt: Instant,
    )

    private fun SetEntry.toCompleted(): CompletedSet? {
        val load = performedLoadKg ?: return null
        val reps = completedReps?.takeIf { it > 0 } ?: return null
        return CompletedSet(
            exerciseId = exerciseId,
            loadKg = load,
            reps = reps,
            performedAt = createdAt,
        )
    }

    private const val EPLEY_DENOMINATOR = 30.0
}
