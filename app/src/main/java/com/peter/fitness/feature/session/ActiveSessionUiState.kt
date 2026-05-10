package com.peter.fitness.feature.session

import com.peter.fitness.domain.model.SessionFocus
import java.time.Instant

data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val sessionMissing: Boolean = false,
    val sessionId: String = "",
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val focus: SessionFocus = SessionFocus.FREE_LOG,
    val sets: List<SetEntryUi> = emptyList(),
    val isFinishing: Boolean = false,
    val isFinished: Boolean = false,
)

data class SetEntryUi(
    val id: String,
    val ordinal: Int,
    val exerciseName: String,
    val targetReps: Int,
    val targetLoadKg: Double,
    val completedReps: Int?,
    val performedLoadKg: Double?,
)
