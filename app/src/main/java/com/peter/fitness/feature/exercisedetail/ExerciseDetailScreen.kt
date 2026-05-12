package com.peter.fitness.feature.exercisedetail

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme
import com.peter.fitness.domain.analytics.BestE1RM
import com.peter.fitness.domain.analytics.ExerciseHistoryEntry
import com.peter.fitness.domain.analytics.HeaviestSet
import com.peter.fitness.domain.analytics.ProgressPoint
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.TechniqueDemand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()
    }
    ExerciseDetailScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(state: ExerciseDetailUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.isLoading) "Exercise" else state.exerciseName) },
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
            state.totalSets == 0 -> Empty(state, Modifier.padding(padding))
            else -> Body(state, Modifier.padding(padding))
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
private fun Empty(state: ExerciseDetailUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.exerciseName, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "No sets logged yet for this exercise.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Body(state: ExerciseDetailUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(state) }
        item { Stats(state) }
        if (state.progress.size >= MIN_POINTS_FOR_CHART) {
            item { ChartCard(state.progress) }
        }
        item { Text("History", style = MaterialTheme.typography.titleMedium) }
        items(state.history, key = { it.setId }) { entry -> HistoryRow(entry) }
    }
}

@Composable
private fun Header(state: ExerciseDetailUiState) {
    Text(
        text = "${state.movementPattern.label()} · ${state.techniqueDemand.label()} technique",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Stats(state: ExerciseDetailUiState) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${state.totalSets} ${if (state.totalSets == 1) "set" else "sets"} across " +
                    "${state.progress.size} ${if (state.progress.size == 1) "session" else "sessions"}",
                style = MaterialTheme.typography.titleSmall,
            )
            state.heaviestSet?.let { set ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Heaviest: ${formatKg(set.loadKg)} × ${set.reps} on ${formatDate(set.performedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.bestE1RM?.let { e ->
                Text(
                    "Best e1RM: ${formatKg(e.estimatedKg)} (Epley) from " +
                        "${formatKg(e.sourceLoadKg)} × ${e.sourceReps} on ${formatDate(e.performedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ChartCard(progress: List<ProgressPoint>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Estimated 1RM trend", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            E1RMChart(
                points = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT.dp),
            )
            Spacer(Modifier.height(8.dp))
            val min = progress.minOf { it.bestE1RM }
            val max = progress.maxOf { it.bestE1RM }
            Text(
                "${formatKg(min)} → ${formatKg(max)} across ${progress.size} sessions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun E1RMChart(points: List<ProgressPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < MIN_POINTS_FOR_CHART) return@Canvas
        val values = points.map { it.bestE1RM }
        val maxValue = values.max()
        val minValue = values.min()
        val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val padding = size.height * CHART_VERTICAL_PADDING_RATIO
        val plotHeight = size.height - padding * 2f
        val xStep = if (points.size > 1) size.width / (points.size - 1).toFloat() else 0f

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * xStep
            val normalised = (point.bestE1RM - minValue) / range
            val y = padding + plotHeight * (1f - normalised.toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = LINE_STROKE_WIDTH_PX))

        points.forEachIndexed { index, point ->
            val x = index * xStep
            val normalised = (point.bestE1RM - minValue) / range
            val y = padding + plotHeight * (1f - normalised.toFloat())
            drawCircle(color = pointColor, radius = POINT_RADIUS_PX, center = Offset(x, y))
        }
    }
}

@Composable
private fun HistoryRow(entry: ExerciseHistoryEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${entry.reps} × ${formatKg(entry.loadKg)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "e1RM ${formatKg(entry.estimatedE1RM)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatDate(entry.performedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
    }
}

internal fun MovementPattern.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

internal fun TechniqueDemand.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    dateFormatter.format(LocalDate.ofInstant(instant, zone))

private fun formatKg(kg: Double): String {
    val rounded = (kg * KG_ROUND).toLong() / KG_ROUND.toDouble()
    return if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()} kg" else "$rounded kg"
}

private const val KG_ROUND = 10.0
private const val MIN_POINTS_FOR_CHART = 2
private const val CHART_HEIGHT = 180
private const val CHART_VERTICAL_PADDING_RATIO = 0.1f
private const val LINE_STROKE_WIDTH_PX = 4f
private const val POINT_RADIUS_PX = 6f

@Preview(showBackground = true)
@Composable
private fun ExerciseDetailScreenPreview() {
    FitnessTheme {
        val now = Instant.parse("2026-05-10T08:00:00Z")
        val sessionId = SessionId("session-1")
        ExerciseDetailScreen(
            state = ExerciseDetailUiState(
                isLoading = false,
                exerciseName = "Back Squat",
                movementPattern = MovementPattern.SQUAT,
                techniqueDemand = TechniqueDemand.HIGH,
                totalSets = 8,
                heaviestSet = HeaviestSet(100.0, 5, now),
                bestE1RM = BestE1RM(116.67, 100.0, 5, now),
                progress = listOf(
                    ProgressPoint(sessionId, now.minusSeconds(7 * 86400), 105.0, HeaviestSet(90.0, 5, now), 4),
                    ProgressPoint(sessionId, now.minusSeconds(3 * 86400), 110.0, HeaviestSet(95.0, 5, now), 4),
                    ProgressPoint(sessionId, now, 116.67, HeaviestSet(100.0, 5, now), 4),
                ),
                history = listOf(
                    ExerciseHistoryEntry("a", sessionId, now, 100.0, 5, 116.67, now),
                    ExerciseHistoryEntry("b", sessionId, now, 95.0, 5, 110.83, now),
                ),
            ),
            onBack = {},
        )
    }
}
