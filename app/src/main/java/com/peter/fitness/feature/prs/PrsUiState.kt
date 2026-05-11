package com.peter.fitness.feature.prs

import com.peter.fitness.domain.analytics.PrSummary

data class PrsUiState(
    val isLoading: Boolean = true,
    val summaries: List<PrSummary> = emptyList(),
)
