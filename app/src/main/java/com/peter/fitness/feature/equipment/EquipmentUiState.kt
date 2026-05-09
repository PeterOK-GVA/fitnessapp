package com.peter.fitness.feature.equipment

data class EquipmentUiState(
    val isLoading: Boolean = true,
    val barKg: String = "",
    val hasRack: Boolean = false,
    val hasBench: Boolean = false,
    val hasPullUpBar: Boolean = false,
    val plates: List<PlateRowUi> = emptyList(),
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false,
    val barKgError: String? = null,
    val plateErrors: Map<String, String> = emptyMap(),
)

data class PlateRowUi(
    val key: String,
    val denominationKg: String,
    val pairCount: String,
)
