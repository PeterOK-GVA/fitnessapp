package com.peter.fitness.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.peter.fitness.domain.model.SessionFocus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onBack = onBack, onSessionClick = onSessionClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History") },
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
            state.sessions.isEmpty() -> EmptyState(Modifier.padding(padding))
            else -> SessionList(
                sessions = state.sessions,
                onSessionClick = onSessionClick,
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No sessions yet. Start a workout to see it here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionList(
    sessions: List<HistorySessionUi>,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sessions, key = { it.id }) { session ->
            SessionCard(session = session, onClick = { onSessionClick(session.id) })
        }
    }
}

@Composable
private fun SessionCard(session: HistorySessionUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(session.focus.label(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatStartedAt(session.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${session.setCount} ${if (session.setCount == 1) "set" else "sets"}" +
                    if (session.endedAt == null) " · in progress" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun SessionFocus.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

internal fun formatStartedAt(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val local = LocalDateTime.ofInstant(instant, zone)
    val today = LocalDate.now(zone)
    val date = local.toLocalDate()
    return when (date) {
        today -> "Today, ${timeFormatter.format(local)}"
        today.minusDays(1) -> "Yesterday, ${timeFormatter.format(local)}"
        else -> dateFormatter.format(local)
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    FitnessTheme {
        HistoryScreen(
            state = HistoryUiState(
                isLoading = false,
                sessions = listOf(
                    HistorySessionUi(
                        id = "a",
                        focus = SessionFocus.FREE_LOG,
                        startedAt = Instant.parse("2026-05-10T08:00:00Z"),
                        endedAt = Instant.parse("2026-05-10T09:15:00Z"),
                        setCount = 8,
                    ),
                    HistorySessionUi(
                        id = "b",
                        focus = SessionFocus.STRENGTH,
                        startedAt = Instant.parse("2026-05-08T17:00:00Z"),
                        endedAt = null,
                        setCount = 3,
                    ),
                ),
            ),
            onBack = {},
            onSessionClick = {},
        )
    }
}
