package com.peter.fitness.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peter.fitness.core.ui.theme.FitnessTheme

@Composable
fun HomeRoute(
    onEquipmentClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onSessionStarted: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.SessionStarted -> onSessionStarted(event.sessionId)
            }
        }
    }
    HomeScreen(
        onEquipmentClick = onEquipmentClick,
        onCalculatorClick = onCalculatorClick,
        onExercisesClick = onExercisesClick,
        onStartWorkoutClick = viewModel::onStartWorkout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEquipmentClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Fitness") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeCard(
                title = "Start workout",
                subtitle = "Begin a manual session and log sets",
                onClick = onStartWorkoutClick,
            )
            HomeCard(
                title = "Equipment",
                subtitle = "Configure your bar, plates, rack, bench, pull-up bar",
                onClick = onEquipmentClick,
            )
            HomeCard(
                title = "Plate calculator",
                subtitle = "Round any target weight to your inventory",
                onClick = onCalculatorClick,
            )
            HomeCard(
                title = "Exercises",
                subtitle = "Browse the barbell catalogue",
                onClick = onExercisesClick,
            )
            HomeCard(
                title = "History",
                subtitle = "Coming in Phase 1.6",
                onClick = {},
                enabled = false,
            )
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FitnessTheme {
        HomeScreen(
            onEquipmentClick = {},
            onCalculatorClick = {},
            onExercisesClick = {},
            onStartWorkoutClick = {},
        )
    }
}
