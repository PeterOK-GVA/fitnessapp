package com.peter.fitness.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.plates.PlateCalculator
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlateCalculatorViewModel @Inject constructor(
    equipmentRepository: EquipmentInventoryRepository,
) : ViewModel() {

    private val targetInput = MutableStateFlow("")

    val uiState: StateFlow<PlateCalculatorUiState> =
        combine(targetInput, equipmentRepository.observeCurrent()) { target, inventory ->
            val trimmed = target.trim()
            val parsed = trimmed.toDoubleOrNull()
            val error = when {
                trimmed.isEmpty() -> null
                parsed == null -> "Enter a number"
                parsed < 0.0 -> "Must be non-negative"
                else -> null
            }
            val result = if (parsed != null && parsed >= 0.0) {
                PlateCalculator.snap(parsed, inventory)
            } else {
                null
            }
            PlateCalculatorUiState(
                targetKg = target,
                isLoading = false,
                barKg = inventory.barKg,
                result = result,
                error = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PlateCalculatorUiState(),
        )

    fun onTargetChange(value: String) {
        targetInput.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
