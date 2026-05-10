package com.peter.fitness.feature.session.logset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating

@Composable
fun LogSetRoute(
    onDone: () -> Unit,
    viewModel: LogSetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { onDone() }
    }
    LaunchedEffect(state.notFound) {
        if (state.notFound) onDone()
    }
    LogSetScreen(
        state = state,
        onBack = onDone,
        onRepsChange = viewModel::onRepsChange,
        onLoadKgChange = viewModel::onLoadKgChange,
        onSubjectiveLoadChange = viewModel::onSubjectiveLoadChange,
        onTechniqueRatingChange = viewModel::onTechniqueRatingChange,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSetScreen(
    state: LogSetUiState,
    onBack: () -> Unit,
    onRepsChange: (String) -> Unit,
    onLoadKgChange: (String) -> Unit,
    onSubjectiveLoadChange: (SubjectiveLoad?) -> Unit,
    onTechniqueRatingChange: (TechniqueRating?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.isEditMode) "Edit set" else "Log set") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.exerciseName, style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = state.reps,
                onValueChange = onRepsChange,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.repsError != null,
                supportingText = { state.repsError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.loadKg,
                onValueChange = onLoadKgChange,
                label = { Text("Load (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.loadKgError != null,
                supportingText = { state.loadKgError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.plateHint?.let { hint ->
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = hint.achievableText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hint.isOnTarget) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = hint.perSideText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SubjectiveLoadRow(
                value = state.subjectiveLoad,
                onChange = onSubjectiveLoadChange,
            )
            TechniqueRow(
                value = state.techniqueRating,
                onChange = onTechniqueRatingChange,
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = !state.isSaving && !state.isDeleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
            if (state.isEditMode) {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !state.isSaving && !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isDeleting) "Deleting…" else "Delete set")
                }
            }
        }
    }
}

@Composable
private fun SubjectiveLoadRow(
    value: SubjectiveLoad?,
    onChange: (SubjectiveLoad?) -> Unit,
) {
    Column {
        Text("Subjective load", style = MaterialTheme.typography.titleSmall)
        LazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(SubjectiveLoad.entries.toList(), key = { it.name }) { option ->
                FilterChip(
                    selected = value == option,
                    onClick = { onChange(if (value == option) null else option) },
                    label = { Text(option.label()) },
                )
            }
        }
    }
}

@Composable
private fun TechniqueRow(
    value: TechniqueRating?,
    onChange: (TechniqueRating?) -> Unit,
) {
    Column {
        Text("Technique", style = MaterialTheme.typography.titleSmall)
        LazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(TechniqueRating.entries.toList(), key = { it.name }) { option ->
                FilterChip(
                    selected = value == option,
                    onClick = { onChange(if (value == option) null else option) },
                    label = { Text(option.label()) },
                )
            }
        }
    }
}

private fun SubjectiveLoad.label(): String = when (this) {
    SubjectiveLoad.MUCH_TOO_LIGHT -> "Much too light"
    SubjectiveLoad.TOO_LIGHT -> "Too light"
    SubjectiveLoad.OK -> "OK"
    SubjectiveLoad.TOO_HEAVY -> "Too heavy"
    SubjectiveLoad.MUCH_TOO_HEAVY -> "Much too heavy"
}

private fun TechniqueRating.label(): String = name
    .lowercase()
    .replaceFirstChar { it.uppercase() }

@Preview(showBackground = true)
@Composable
private fun LogSetScreenPreview() {
    FitnessTheme {
        LogSetScreen(
            state = LogSetUiState(
                isLoading = false,
                isEditMode = false,
                exerciseId = "back-squat",
                exerciseName = "Back Squat",
                reps = "5",
                loadKg = "80",
                subjectiveLoad = SubjectiveLoad.OK,
                techniqueRating = TechniqueRating.GOOD,
            ),
            onBack = {},
            onRepsChange = {},
            onLoadKgChange = {},
            onSubjectiveLoadChange = {},
            onTechniqueRatingChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
