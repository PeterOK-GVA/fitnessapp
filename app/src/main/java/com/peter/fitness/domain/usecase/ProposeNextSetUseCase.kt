package com.peter.fitness.domain.usecase

import com.peter.fitness.domain.coach.CoachEngine
import com.peter.fitness.domain.coach.CoachPolicy
import com.peter.fitness.domain.coach.CoachProposal
import com.peter.fitness.domain.coach.CompletedSet
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import com.peter.fitness.domain.repository.ProgressionStateRepository
import com.peter.fitness.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Produces the Coach's proposed next prescription for an exercise. Returns null when there is no
 * persisted [com.peter.fitness.domain.coach.ProgressionState] yet — i.e. the very first time the
 * athlete trains an exercise; the caller bootstraps state from the first logged set instead.
 *
 * The "last results" handed to the engine come from the most recent *other* session that contains
 * the exercise, so adding multiple sets within the current session does not feed back into the
 * decision.
 */
class ProposeNextSetUseCase @Inject constructor(
    private val progressionStateRepository: ProgressionStateRepository,
    private val sessionRepository: SessionRepository,
    private val equipmentRepository: EquipmentInventoryRepository,
) {
    suspend operator fun invoke(
        exerciseId: ExerciseId,
        currentSessionId: SessionId,
        policy: CoachPolicy = CoachPolicy.DEFAULT,
    ): CoachProposal? {
        val state = progressionStateRepository.find(exerciseId) ?: return null
        val inventory = equipmentRepository.current()
        val lastResults = lastResultsFor(exerciseId, currentSessionId)
        val decision = CoachEngine.decideNext(state, lastResults, policy, inventory)
        return CoachProposal(
            prescription = decision.prescription,
            rationale = decision.rationale,
            newState = decision.newState,
        )
    }

    private suspend fun lastResultsFor(
        exerciseId: ExerciseId,
        currentSessionId: SessionId,
    ): List<CompletedSet> {
        val startedAtBySession = sessionRepository.observeAll().first()
            .associate { it.id to it.startedAt }
        val mostRecent = sessionRepository.observeAllSets().first()
            .filter { it.exerciseId == exerciseId && it.sessionId != currentSessionId }
            .groupBy { it.sessionId }
            .maxByOrNull { (sessionId, _) ->
                startedAtBySession[sessionId] ?: Instant.EPOCH
            }
            ?: return emptyList()
        return mostRecent.value.mapNotNull { it.toCompletedOrNull() }
    }

    private fun SetEntry.toCompletedOrNull(): CompletedSet? {
        val reps = completedReps ?: return null
        val load = performedLoadKg ?: return null
        return CompletedSet(
            targetReps = targetReps,
            targetLoadKg = targetLoadKg,
            completedReps = reps,
            performedLoadKg = load,
            subjectiveLoad = subjectiveLoad,
            techniqueRating = techniqueRating,
        )
    }
}
