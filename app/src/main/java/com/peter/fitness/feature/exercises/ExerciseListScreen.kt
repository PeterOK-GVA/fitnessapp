package com.peter.fitness.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand

@Composable
fun ExerciseListRoute(
    onBack: () -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseListScreen(
        state = state,
        onBack = onBack,
        onFilterSelected = viewModel::onFilterSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    state: ExerciseListUiState,
    onBack: () -> Unit,
    onFilterSelected: (MovementPattern?) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.exercises.isEmpty() && state.selectedFilter == null -> EmptyBox(Modifier.padding(padding))
            else -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                FilterRow(
                    available = state.availableFilters,
                    selected = state.selectedFilter,
                    onFilterSelected = onFilterSelected,
                )
                HorizontalDivider()
                ExerciseList(state.exercises)
            }
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No exercises in catalogue", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilterRow(
    available: List<MovementPattern>,
    selected: MovementPattern?,
    onFilterSelected: (MovementPattern?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All") },
            )
        }
        items(available, key = { it.name }) { pattern ->
            FilterChip(
                selected = selected == pattern,
                onClick = { onFilterSelected(if (selected == pattern) null else pattern) },
                label = { Text(pattern.label()) },
            )
        }
    }
}

@Composable
private fun ExerciseList(items: List<ExerciseListItemUi>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id.value }) { item ->
            ExerciseRow(item)
        }
    }
}

@Composable
private fun ExerciseRow(item: ExerciseListItemUi) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = item.movementPattern.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TinyChip(item.techniqueDemand.label() + " technique")
            if (item.requiresRack) TinyChip("Rack")
            if (item.requiresBench) TinyChip("Bench")
            if (item.requiresPullUpBar) TinyChip("Pull-up bar")
        }
    }
}

@Composable
private fun TinyChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(),
    )
}

internal fun MovementPattern.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

internal fun TechniqueDemand.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

@Preview(showBackground = true)
@Composable
private fun ExerciseListScreenPreview() {
    FitnessTheme {
        ExerciseListScreen(
            state = ExerciseListUiState(
                isLoading = false,
                exercises = listOf(
                    ExerciseListItemUi(
                        id = ExerciseId("back-squat"),
                        name = "Back Squat",
                        movementPattern = MovementPattern.SQUAT,
                        techniqueDemand = TechniqueDemand.HIGH,
                        requiresRack = true,
                        requiresBench = false,
                        requiresPullUpBar = false,
                    ),
                    ExerciseListItemUi(
                        id = ExerciseId("bent-row"),
                        name = "Bent Row",
                        movementPattern = MovementPattern.HORIZONTAL_PULL,
                        techniqueDemand = TechniqueDemand.MODERATE,
                        requiresRack = false,
                        requiresBench = false,
                        requiresPullUpBar = false,
                    ),
                ),
                availableFilters = listOf(
                    MovementPattern.SQUAT,
                    MovementPattern.HIP_HINGE,
                    MovementPattern.HORIZONTAL_PULL,
                ),
                selectedFilter = null,
            ),
            onBack = {},
            onFilterSelected = {},
        )
    }
}
