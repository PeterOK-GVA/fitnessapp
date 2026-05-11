package com.peter.fitness.feature.prs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.peter.fitness.domain.analytics.BestE1RM
import com.peter.fitness.domain.analytics.HeaviestSet
import com.peter.fitness.domain.analytics.PrSummary
import com.peter.fitness.domain.model.ExerciseId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PrsRoute(
    onBack: () -> Unit,
    viewModel: PrsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PrsScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrsScreen(state: PrsUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PRs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Loading(Modifier.padding(padding))
            state.summaries.isEmpty() -> Empty(Modifier.padding(padding))
            else -> PrsList(
                summaries = state.summaries,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Empty(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No sets logged yet — your PRs will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrsList(summaries: List<PrSummary>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(summaries, key = { it.exerciseId.value }) { summary ->
            SummaryCard(summary)
        }
    }
}

@Composable
private fun SummaryCard(summary: PrSummary) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(summary.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.totalSets} ${if (summary.totalSets == 1) "set" else "sets"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            summary.heaviestSet?.let { HeaviestRow(it) }
            Spacer(Modifier.height(4.dp))
            summary.bestE1RM?.let { E1RMRow(it) }
        }
    }
}

@Composable
private fun HeaviestRow(set: HeaviestSet) {
    Column {
        Text(
            "Heaviest: ${formatKg(set.loadKg)} × ${set.reps}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "on ${formatDate(set.performedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun E1RMRow(e1rm: BestE1RM) {
    Column {
        Text(
            "Best e1RM: ${formatKg(e1rm.estimatedKg)} (Epley)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "from ${formatKg(e1rm.sourceLoadKg)} × ${e1rm.sourceReps} on ${formatDate(e1rm.performedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    dateFormatter.format(LocalDate.ofInstant(instant, zone))

private fun formatKg(kg: Double): String {
    val rounded = (kg * KG_DISPLAY_MULTIPLIER).toLong() / KG_DISPLAY_MULTIPLIER.toDouble()
    return if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()} kg" else "$rounded kg"
}

private const val KG_DISPLAY_MULTIPLIER = 10.0

@Preview(showBackground = true)
@Composable
private fun PrsScreenPreview() {
    FitnessTheme {
        val now = Instant.parse("2026-05-10T08:00:00Z")
        PrsScreen(
            state = PrsUiState(
                isLoading = false,
                summaries = listOf(
                    PrSummary(
                        exerciseId = ExerciseId("back-squat"),
                        exerciseName = "Back Squat",
                        totalSets = 24,
                        heaviestSet = HeaviestSet(loadKg = 100.0, reps = 5, performedAt = now),
                        bestE1RM = BestE1RM(
                            estimatedKg = 116.66666666666667,
                            sourceLoadKg = 100.0,
                            sourceReps = 5,
                            performedAt = now,
                        ),
                    ),
                    PrSummary(
                        exerciseId = ExerciseId("bench-press"),
                        exerciseName = "Bench Press",
                        totalSets = 12,
                        heaviestSet = HeaviestSet(loadKg = 80.0, reps = 1, performedAt = now),
                        bestE1RM = BestE1RM(
                            estimatedKg = 82.67,
                            sourceLoadKg = 80.0,
                            sourceReps = 1,
                            performedAt = now,
                        ),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
