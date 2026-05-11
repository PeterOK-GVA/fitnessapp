package com.peter.fitness.feature.prs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.analytics.PrCalculator
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PrsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<PrsUiState> =
        combine(
            sessionRepository.observeAllSets(),
            exerciseRepository.observeAll(),
        ) { sets, exercises ->
            PrsUiState(
                isLoading = false,
                summaries = PrCalculator.summaries(sets, exercises),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PrsUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
