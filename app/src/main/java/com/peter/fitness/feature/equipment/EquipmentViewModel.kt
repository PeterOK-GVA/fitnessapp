package com.peter.fitness.feature.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.PlatePair
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EquipmentViewModel @Inject constructor(
    private val equipmentRepository: EquipmentInventoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipmentUiState())
    val uiState: StateFlow<EquipmentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = equipmentRepository.current()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    barKg = current.barKg.toCleanString(),
                    hasRack = current.hasRack,
                    hasBench = current.hasBench,
                    hasPullUpBar = current.hasPullUpBar,
                    plates = current.plates.map { plate ->
                        PlateRowUi(
                            key = UUID.randomUUID().toString(),
                            denominationKg = plate.denominationKg.toCleanString(),
                            pairCount = plate.pairCount.toString(),
                        )
                    },
                )
            }
        }
    }

    fun onBarKgChange(value: String) {
        _uiState.update {
            it.copy(barKg = value, isDirty = true, justSaved = false, barKgError = null)
        }
    }

    fun onHasRackChange(value: Boolean) {
        _uiState.update { it.copy(hasRack = value, isDirty = true, justSaved = false) }
    }

    fun onHasBenchChange(value: Boolean) {
        _uiState.update { it.copy(hasBench = value, isDirty = true, justSaved = false) }
    }

    fun onHasPullUpBarChange(value: Boolean) {
        _uiState.update { it.copy(hasPullUpBar = value, isDirty = true, justSaved = false) }
    }

    fun onPlateDenominationChange(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                plates = state.plates.map { row ->
                    if (row.key == key) row.copy(denominationKg = value) else row
                },
                isDirty = true,
                justSaved = false,
                plateErrors = state.plateErrors - key,
            )
        }
    }

    fun onPlateCountChange(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                plates = state.plates.map { row ->
                    if (row.key == key) row.copy(pairCount = value) else row
                },
                isDirty = true,
                justSaved = false,
                plateErrors = state.plateErrors - key,
            )
        }
    }

    fun onAddPlate() {
        _uiState.update { state ->
            state.copy(
                plates = state.plates + PlateRowUi(
                    key = UUID.randomUUID().toString(),
                    denominationKg = "",
                    pairCount = "",
                ),
                isDirty = true,
                justSaved = false,
            )
        }
    }

    fun onRemovePlate(key: String) {
        _uiState.update { state ->
            state.copy(
                plates = state.plates.filterNot { it.key == key },
                isDirty = true,
                justSaved = false,
                plateErrors = state.plateErrors - key,
            )
        }
    }

    fun onSave() {
        val state = _uiState.value
        val (inventory, errors) = state.toValidatedInventory()
        if (inventory == null) {
            _uiState.update {
                it.copy(barKgError = errors.barKgError, plateErrors = errors.plateErrors)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, barKgError = null, plateErrors = emptyMap())
            }
            equipmentRepository.update(inventory)
            _uiState.update { it.copy(isSaving = false, isDirty = false, justSaved = true) }
        }
    }

    fun onSavedAcknowledged() {
        _uiState.update { it.copy(justSaved = false) }
    }

    private data class ValidationErrors(
        val barKgError: String?,
        val plateErrors: Map<String, String>,
    )

    private fun EquipmentUiState.toValidatedInventory(): Pair<EquipmentInventory?, ValidationErrors> {
        val parsedBar = barKg.trim().toDoubleOrNull()
        val barError = when {
            parsedBar == null -> "Enter a number"
            parsedBar < 0.0 -> "Bar weight must be non-negative"
            else -> null
        }
        val plateErrors = mutableMapOf<String, String>()
        val parsedPlates = mutableListOf<PlatePair>()
        plates.forEach { row ->
            val denom = row.denominationKg.trim().toDoubleOrNull()
            val count = row.pairCount.trim().toIntOrNull()
            when {
                denom == null || count == null -> {
                    plateErrors[row.key] = "Enter numbers"
                }
                denom <= 0.0 -> plateErrors[row.key] = "Plate weight must be positive"
                count < 0 -> plateErrors[row.key] = "Pair count must be non-negative"
                else -> parsedPlates += PlatePair(denominationKg = denom, pairCount = count)
            }
        }
        if (barError != null || plateErrors.isNotEmpty() || parsedBar == null) {
            return null to ValidationErrors(barError, plateErrors)
        }
        return EquipmentInventory(
            barKg = parsedBar,
            hasRack = hasRack,
            hasBench = hasBench,
            hasPullUpBar = hasPullUpBar,
            plates = parsedPlates,
            updatedAt = Instant.now(),
        ) to ValidationErrors(null, emptyMap())
    }
}

private fun Double.toCleanString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
