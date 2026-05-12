package com.peter.fitness.domain.analytics

import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.Session
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import java.time.Instant

data class ProgressPoint(
    val sessionId: SessionId,
    val sessionStartedAt: Instant,
    val bestE1RM: Double,
    val heaviestSet: HeaviestSet,
    val setCount: Int,
)

data class ExerciseHistoryEntry(
    val setId: String,
    val sessionId: SessionId,
    val sessionStartedAt: Instant,
    val loadKg: Double,
    val reps: Int,
    val estimatedE1RM: Double,
    val performedAt: Instant,
)

object ExerciseProgressCalculator {

    /**
     * For a given exercise, return one [ProgressPoint] per session that contains at least one
     * completed set of that exercise. Each point summarises the session's best Epley e1RM and the
     * heaviest set lifted. Result is sorted oldest → newest by session start time.
     */
    fun progressFor(
        exerciseId: ExerciseId,
        sessions: List<Session>,
        sets: List<SetEntry>,
    ): List<ProgressPoint> {
        val sessionsById = sessions.associateBy { it.id }
        return sets.asSequence()
            .filter { it.exerciseId == exerciseId }
            .mapNotNull { it.toAnalysed(exerciseId) }
            .groupBy { it.sessionId }
            .mapNotNull { (sessionId, analysedSets) ->
                val session = sessionsById[sessionId] ?: return@mapNotNull null
                val bestE1RM = analysedSets.maxOf { PrCalculator.epleyE1RM(it.loadKg, it.reps) }
                val heaviest = analysedSets.maxByOrNull { it.loadKg } ?: return@mapNotNull null
                ProgressPoint(
                    sessionId = sessionId,
                    sessionStartedAt = session.startedAt,
                    bestE1RM = bestE1RM,
                    heaviestSet = HeaviestSet(
                        loadKg = heaviest.loadKg,
                        reps = heaviest.reps,
                        performedAt = heaviest.performedAt,
                    ),
                    setCount = analysedSets.size,
                )
            }
            .sortedBy { it.sessionStartedAt }
    }

    /**
     * Flat list of every completed set for the exercise, newest-first, with per-set e1RM and
     * session metadata folded in. Useful for the history table on the detail screen.
     */
    fun historyFor(
        exerciseId: ExerciseId,
        sessions: List<Session>,
        sets: List<SetEntry>,
    ): List<ExerciseHistoryEntry> {
        val sessionsById = sessions.associateBy { it.id }
        return sets.asSequence()
            .filter { it.exerciseId == exerciseId }
            .mapNotNull { entry ->
                val analysed = entry.toAnalysed(exerciseId) ?: return@mapNotNull null
                val session = sessionsById[analysed.sessionId] ?: return@mapNotNull null
                ExerciseHistoryEntry(
                    setId = entry.id.value,
                    sessionId = analysed.sessionId,
                    sessionStartedAt = session.startedAt,
                    loadKg = analysed.loadKg,
                    reps = analysed.reps,
                    estimatedE1RM = PrCalculator.epleyE1RM(analysed.loadKg, analysed.reps),
                    performedAt = analysed.performedAt,
                )
            }
            .sortedByDescending { it.performedAt }
            .toList()
    }

    private data class AnalysedSet(
        val sessionId: SessionId,
        val loadKg: Double,
        val reps: Int,
        val performedAt: Instant,
    )

    private fun SetEntry.toAnalysed(target: ExerciseId): AnalysedSet? {
        if (exerciseId != target) return null
        val load = performedLoadKg ?: return null
        val reps = completedReps?.takeIf { it > 0 } ?: return null
        return AnalysedSet(
            sessionId = sessionId,
            loadKg = load,
            reps = reps,
            performedAt = createdAt,
        )
    }
}
