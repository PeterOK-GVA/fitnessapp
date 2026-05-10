package com.peter.fitness.feature.history.detail

import com.peter.fitness.domain.model.SessionFocus
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import java.time.Instant

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val sessionId: String = "",
    val focus: SessionFocus = SessionFocus.FREE_LOG,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val sets: List<DetailSetUi> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
)

data class DetailSetUi(
    val id: String,
    val ordinal: Int,
    val exerciseName: String,
    val reps: Int,
    val loadKg: Double,
    val subjectiveLoad: SubjectiveLoad?,
    val techniqueRating: TechniqueRating?,
)

sealed interface SessionDetailEvent {
    data object Deleted : SessionDetailEvent
}
