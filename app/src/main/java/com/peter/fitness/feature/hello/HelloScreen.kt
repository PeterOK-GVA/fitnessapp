package com.peter.fitness.feature.hello

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme

@Composable
fun HelloRoute(viewModel: HelloViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HelloScreen(state = state, onTap = viewModel::onTap)
}

@Composable
fun HelloScreen(state: HelloUiState, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Phase 1.1 — data layer",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Exercises in catalogue: ${state.exerciseCount}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Bar: ${state.barKg} kg",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (state.plateDenominationsKg.isEmpty()) {
                "Plates: none configured"
            } else {
                "Plates: ${state.plateDenominationsKg.joinToString(", ") { "$it kg" }}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Taps: ${state.tapCount}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onTap) {
            Text("Tap me")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelloScreenPreview() {
    FitnessTheme {
        HelloScreen(
            state = HelloUiState(
                tapCount = 3,
                exerciseCount = 15,
                plateDenominationsKg = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
                barKg = 20.0,
            ),
            onTap = {},
        )
    }
}
