package com.peter.fitness.feature.session.logset

import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating

data class LogSetUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val notFound: Boolean = false,
    val exerciseId: String = "",
    val exerciseName: String = "",
    val reps: String = "",
    val loadKg: String = "",
    val subjectiveLoad: SubjectiveLoad? = null,
    val techniqueRating: TechniqueRating? = null,
    val repsError: String? = null,
    val loadKgError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val plateHint: PlateHintUi? = null,
)

data class PlateHintUi(
    val achievableText: String,
    val perSideText: String,
    val isOnTarget: Boolean,
)

sealed interface LogSetEvent {
    data object Saved : LogSetEvent
    data object Deleted : LogSetEvent
    data object Cancelled : LogSetEvent
}
