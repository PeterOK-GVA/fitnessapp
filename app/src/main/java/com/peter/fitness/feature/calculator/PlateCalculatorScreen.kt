package com.peter.fitness.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme
import com.peter.fitness.domain.plates.PlateLoad
import com.peter.fitness.domain.plates.SnapResult
import kotlin.math.abs

@Composable
fun PlateCalculatorRoute(
    onBack: () -> Unit,
    viewModel: PlateCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlateCalculatorScreen(
        state = state,
        onBack = onBack,
        onTargetChange = viewModel::onTargetChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorScreen(
    state: PlateCalculatorUiState,
    onBack: () -> Unit,
    onTargetChange: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Plate calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.targetKg,
                onValueChange = onTargetChange,
                label = { Text("Target weight (kg)") },
                supportingText = { state.error?.let { Text(it) } },
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Bar: ${formatKg(state.barKg)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            state.result?.let { ResultCard(it) }
        }
    }
}

@Composable
private fun ResultCard(result: SnapResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Achievable: ${formatKg(result.achievableKg)}",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (abs(result.deviationKg) >= DEVIATION_DISPLAY_THRESHOLD_KG) {
                val sign = if (result.deviationKg > 0) "+" else ""
                Text(
                    text = "Off target by $sign${formatKg(result.deviationKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (result.perSide.isEmpty()) {
                Text("No plates needed", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "Per side: ${result.perSide.joinToString(" + ") { formatPlateLoad(it) }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val DEVIATION_DISPLAY_THRESHOLD_KG = 0.01

private fun formatKg(kg: Double): String {
    val asLong = kg.toLong()
    return if (kg == asLong.toDouble()) "$asLong kg" else "%.2f kg".format(kg)
}

private fun formatPlateLoad(plate: PlateLoad): String =
    "${plate.countPerSide} × ${formatKg(plate.denominationKg).removeSuffix(" kg")} kg"

@Preview(showBackground = true)
@Composable
private fun PlateCalculatorScreenPreview() {
    FitnessTheme {
        PlateCalculatorScreen(
            state = PlateCalculatorUiState(
                targetKg = "100",
                isLoading = false,
                barKg = 20.0,
                result = SnapResult(
                    targetKg = 100.0,
                    achievableKg = 100.0,
                    barKg = 20.0,
                    perSide = listOf(PlateLoad(20.0, 2)),
                ),
            ),
            onBack = {},
            onTargetChange = {},
        )
    }
}
