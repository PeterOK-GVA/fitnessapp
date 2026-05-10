package com.peter.fitness.feature.calculator

import com.peter.fitness.domain.plates.SnapResult

data class PlateCalculatorUiState(
    val targetKg: String = "",
    val isLoading: Boolean = true,
    val barKg: Double = 0.0,
    val result: SnapResult? = null,
    val error: String? = null,
)
