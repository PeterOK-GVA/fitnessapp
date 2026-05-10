package com.peter.fitness.feature.history.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionDetailRoute(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SessionDetailEvent.Deleted -> onBack()
            }
        }
    }
    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()
    }
    SessionDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteClick = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Session") },
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
            else -> Body(
                state = state,
                onDeleteClick = onDeleteClick,
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
private fun Body(
    state: SessionDetailUiState,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Header(state)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (state.sets.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f, fill = true))
        } else {
            SetsList(sets = state.sets, modifier = Modifier.weight(1f, fill = true))
        }
        OutlinedButton(
            onClick = onDeleteClick,
            enabled = !state.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text(if (state.isDeleting) "Deleting…" else "Delete session")
        }
    }
}

@Composable
private fun Header(state: SessionDetailUiState) {
    Column {
        Text(state.focus.label(), style = MaterialTheme.typography.titleMedium)
        val started = state.startedAt?.let(::formatTime) ?: "—"
        val ended = state.endedAt?.let(::formatTime) ?: "in progress"
        val duration = if (state.startedAt != null && state.endedAt != null) {
            formatDuration(Duration.between(state.startedAt, state.endedAt))
        } else {
            null
        }
        Text(
            text = "$started → $ended" + (duration?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${state.sets.size} ${if (state.sets.size == 1) "set" else "sets"} · " +
                "Total volume: ${formatKg(state.totalVolumeKg)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No sets logged in this session.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetsList(sets: List<DetailSetUi>, modifier: Modifier = Modifier) {
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
private fun SetRow(set: DetailSetUi) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${set.ordinal + 1}. ${set.exerciseName}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${set.reps} × ${formatKg(set.loadKg)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            val feedback = listOfNotNull(
                set.subjectiveLoad?.label(),
                set.techniqueRating?.label()?.let { "Technique: $it" },
            ).joinToString(" · ")
            if (feedback.isNotBlank()) {
                Text(
                    feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun SessionFocus.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private fun SubjectiveLoad.label(): String = when (this) {
    SubjectiveLoad.MUCH_TOO_LIGHT -> "Much too light"
    SubjectiveLoad.TOO_LIGHT -> "Too light"
    SubjectiveLoad.OK -> "OK"
    SubjectiveLoad.TOO_HEAVY -> "Too heavy"
    SubjectiveLoad.MUCH_TOO_HEAVY -> "Much too heavy"
}

private fun TechniqueRating.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(instant: Instant): String =
    timeFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))

private fun formatKg(kg: Double): String =
    if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "$kg kg"

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Preview(showBackground = true)
@Composable
private fun SessionDetailScreenPreview() {
    FitnessTheme {
        SessionDetailScreen(
            state = SessionDetailUiState(
                isLoading = false,
                sessionId = "a",
                focus = SessionFocus.FREE_LOG,
                startedAt = Instant.parse("2026-05-10T08:00:00Z"),
                endedAt = Instant.parse("2026-05-10T09:15:00Z"),
                sets = listOf(
                    DetailSetUi("1", 0, "Back Squat", 5, 80.0, SubjectiveLoad.OK, TechniqueRating.GOOD),
                    DetailSetUi("2", 1, "Bench Press", 5, 60.0, SubjectiveLoad.TOO_HEAVY, TechniqueRating.OK),
                ),
                totalVolumeKg = 700.0,
            ),
            onBack = {},
            onDeleteClick = {},
        )
    }
}
