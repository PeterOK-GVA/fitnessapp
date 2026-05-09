package com.peter.fitness.feature.hello

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import com.peter.fitness.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HelloUiState(
    val tapCount: Int = 0,
    val exerciseCount: Int = 0,
    val plateDenominationsKg: List<Double> = emptyList(),
    val barKg: Double = 0.0,
)

@HiltViewModel
class HelloViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    equipmentRepository: EquipmentInventoryRepository,
) : ViewModel() {

    private val taps = MutableStateFlow(0)

    val uiState: StateFlow<HelloUiState> =
        combine(
            taps,
            exerciseRepository.observeAll(),
            equipmentRepository.observeCurrent(),
        ) { tapCount, exercises, inventory ->
            HelloUiState(
                tapCount = tapCount,
                exerciseCount = exercises.size,
                plateDenominationsKg = inventory.plates
                    .filter { it.pairCount > 0 }
                    .map { it.denominationKg },
                barKg = inventory.barKg,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HelloUiState(),
        )

    fun onTap() {
        taps.update { it + 1 }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
