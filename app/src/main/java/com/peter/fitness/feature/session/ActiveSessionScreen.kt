package com.peter.fitness.feature.session

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme
import com.peter.fitness.domain.model.RestTimer
import com.peter.fitness.domain.model.SessionFocus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActiveSessionRoute(
    onFinished: () -> Unit,
    onAddSet: (sessionId: String) -> Unit,
    onEditSet: (sessionId: String, setEntryId: String) -> Unit,
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
        onAddSetClick = { onAddSet(state.sessionId) },
        onSetClick = { setId -> onEditSet(state.sessionId, setId) },
        onCancelRestClick = viewModel::onCancelRestTimer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
    onFinishClick: () -> Unit,
    onBack: () -> Unit,
    onAddSetClick: () -> Unit,
    onSetClick: (String) -> Unit,
    onCancelRestClick: () -> Unit,
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
                onAddSetClick = onAddSetClick,
                onSetClick = onSetClick,
                onCancelRestClick = onCancelRestClick,
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
    onAddSetClick: () -> Unit,
    onSetClick: (String) -> Unit,
    onCancelRestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        SessionHeader(state)
        state.activeRestTimer?.let { timer ->
            Spacer(Modifier.height(8.dp))
            RestTimerBar(timer = timer, onCancel = onCancelRestClick)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (state.sets.isEmpty()) {
            EmptySetsHint(modifier = Modifier.weight(1f, fill = true))
        } else {
            SetsList(
                sets = state.sets,
                onSetClick = onSetClick,
                modifier = Modifier.weight(1f, fill = true),
            )
        }
        OutlinedButton(
            onClick = onAddSetClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add set")
        }
        Button(
            onClick = onFinishClick,
            enabled = !state.isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
            "No sets yet. Tap Add set to log your first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetsList(
    sets: List<SetEntryUi>,
    onSetClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sets, key = { it.id }) { set ->
            SetRow(set, onClick = { onSetClick(set.id) })
        }
    }
}

@Composable
private fun SetRow(set: SetEntryUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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

@Composable
private fun RestTimerBar(timer: RestTimer, onCancel: () -> Unit) {
    var remaining by remember(timer) { mutableLongStateOf(timer.remainingSeconds(Instant.now())) }
    LaunchedEffect(timer) {
        while (remaining > 0) {
            remaining = timer.remainingSeconds(Instant.now())
            if (remaining <= 0) break
            delay(REST_BAR_TICK_MS)
        }
    }
    if (remaining > 0) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${timer.label ?: "Rest"} ${formatRestRemaining(remaining)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) {
                    Text("Skip")
                }
            }
        }
    }
}

private const val REST_BAR_TICK_MS = 500L
private const val SECONDS_PER_MINUTE = 60L

private fun formatRestRemaining(seconds: Long): String {
    val minutes = seconds / SECONDS_PER_MINUTE
    val secs = seconds % SECONDS_PER_MINUTE
    return "%d:%02d".format(minutes, secs)
}

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
            onAddSetClick = {},
            onSetClick = {},
            onCancelRestClick = {},
        )
    }
}
