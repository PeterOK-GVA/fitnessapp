package com.peter.fitness.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme
import com.peter.fitness.domain.model.SessionFocus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActiveSessionRoute(
    onFinished: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isFinished, state.sessionMissing) {
        if (state.isFinished || state.sessionMissing) onFinished()
    }
    ActiveSessionScreen(
        state = state,
        onFinishClick = viewModel::onFinishSession,
        onBack = onFinished,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
    onFinishClick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Active session") },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Loading(Modifier.padding(padding))
            state.sessionMissing -> MissingSession(Modifier.padding(padding), onBack)
            else -> Body(
                state = state,
                onFinishClick = onFinishClick,
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
private fun MissingSession(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Session not found", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun Body(
    state: ActiveSessionUiState,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        SessionHeader(state)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (state.sets.isEmpty()) {
            EmptySetsHint(modifier = Modifier.weight(1f, fill = true))
        } else {
            SetsList(sets = state.sets, modifier = Modifier.weight(1f, fill = true))
        }
        Button(
            onClick = onFinishClick,
            enabled = !state.isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(if (state.isFinishing) "Finishing…" else "Finish session")
        }
    }
}

@Composable
private fun SessionHeader(state: ActiveSessionUiState) {
    Column {
        Text(state.focus.label(), style = MaterialTheme.typography.titleMedium)
        val started = state.startedAt?.let(::formatTime) ?: "—"
        Text(
            "Started $started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySetsHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No sets logged yet. Set logging arrives in Phase 1.5b.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetsList(sets: List<SetEntryUi>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sets, key = { it.id }) { set ->
            SetRow(set)
        }
    }
}

@Composable
private fun SetRow(set: SetEntryUi) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${set.ordinal + 1}. ${set.exerciseName}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            "${set.targetReps} × ${formatKg(set.targetLoadKg)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

internal fun SessionFocus.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(instant: Instant): String =
    timeFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))

private fun formatKg(kg: Double): String =
    if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "$kg kg"

@Preview(showBackground = true)
@Composable
private fun ActiveSessionScreenPreview() {
    FitnessTheme {
        ActiveSessionScreen(
            state = ActiveSessionUiState(
                isLoading = false,
                sessionId = "abc",
                startedAt = Instant.parse("2026-05-10T08:00:00Z"),
                endedAt = null,
                focus = SessionFocus.FREE_LOG,
                sets = emptyList(),
            ),
            onFinishClick = {},
            onBack = {},
        )
    }
}
