package com.peter.fitness.feature.history

import com.peter.fitness.domain.model.SessionFocus
import java.time.Instant

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<HistorySessionUi> = emptyList(),
)

data class HistorySessionUi(
    val id: String,
    val focus: SessionFocus,
    val startedAt: Instant,
    val endedAt: Instant?,
    val setCount: Int,
)
