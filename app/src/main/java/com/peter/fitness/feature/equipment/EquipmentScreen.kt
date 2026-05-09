package com.peter.fitness.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter.fitness.core.ui.theme.FitnessTheme

@Composable
fun EquipmentRoute(
    onBack: () -> Unit,
    viewModel: EquipmentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EquipmentScreen(
        state = state,
        onBack = onBack,
        onBarKgChange = viewModel::onBarKgChange,
        onHasRackChange = viewModel::onHasRackChange,
        onHasBenchChange = viewModel::onHasBenchChange,
        onHasPullUpBarChange = viewModel::onHasPullUpBarChange,
        onPlateDenominationChange = viewModel::onPlateDenominationChange,
        onPlateCountChange = viewModel::onPlateCountChange,
        onAddPlate = viewModel::onAddPlate,
        onRemovePlate = viewModel::onRemovePlate,
        onSave = viewModel::onSave,
        onSavedAcknowledged = viewModel::onSavedAcknowledged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onBack: () -> Unit,
    onBarKgChange: (String) -> Unit,
    onHasRackChange: (Boolean) -> Unit,
    onHasBenchChange: (Boolean) -> Unit,
    onHasPullUpBarChange: (Boolean) -> Unit,
    onPlateDenominationChange: (String, String) -> Unit,
    onPlateCountChange: (String, String) -> Unit,
    onAddPlate: () -> Unit,
    onRemovePlate: (String) -> Unit,
    onSave: () -> Unit,
    onSavedAcknowledged: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.justSaved) {
        if (state.justSaved) {
            snackbarHostState.showSnackbar("Saved")
            onSavedAcknowledged()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Equipment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
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
            BarSection(
                barKg = state.barKg,
                error = state.barKgError,
                onBarKgChange = onBarKgChange,
            )
            FixturesSection(
                hasRack = state.hasRack,
                hasBench = state.hasBench,
                hasPullUpBar = state.hasPullUpBar,
                onHasRackChange = onHasRackChange,
                onHasBenchChange = onHasBenchChange,
                onHasPullUpBarChange = onHasPullUpBarChange,
            )
            PlatesSection(
                plates = state.plates,
                errors = state.plateErrors,
                onPlateDenominationChange = onPlateDenominationChange,
                onPlateCountChange = onPlateCountChange,
                onAddPlate = onAddPlate,
                onRemovePlate = onRemovePlate,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = state.isDirty && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}

@Composable
private fun BarSection(
    barKg: String,
    error: String?,
    onBarKgChange: (String) -> Unit,
) {
    Text("Barbell", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = barKg,
        onValueChange = onBarKgChange,
        label = { Text("Bar weight (kg)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun FixturesSection(
    hasRack: Boolean,
    hasBench: Boolean,
    hasPullUpBar: Boolean,
    onHasRackChange: (Boolean) -> Unit,
    onHasBenchChange: (Boolean) -> Unit,
    onHasPullUpBarChange: (Boolean) -> Unit,
) {
    Text("Fixtures", style = MaterialTheme.typography.titleMedium)
    SwitchRow(label = "Squat or power rack", checked = hasRack, onCheckedChange = onHasRackChange)
    SwitchRow(label = "Bench", checked = hasBench, onCheckedChange = onHasBenchChange)
    SwitchRow(label = "Pull-up bar", checked = hasPullUpBar, onCheckedChange = onHasPullUpBarChange)
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlatesSection(
    plates: List<PlateRowUi>,
    errors: Map<String, String>,
    onPlateDenominationChange: (String, String) -> Unit,
    onPlateCountChange: (String, String) -> Unit,
    onAddPlate: () -> Unit,
    onRemovePlate: (String) -> Unit,
) {
    Text("Plates", style = MaterialTheme.typography.titleMedium)
    if (plates.isEmpty()) {
        Text(
            "No plates configured. Tap \"Add plate\" below to add denominations.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    plates.forEach { row ->
        PlateRow(
            row = row,
            error = errors[row.key],
            onDenominationChange = { onPlateDenominationChange(row.key, it) },
            onCountChange = { onPlateCountChange(row.key, it) },
            onRemove = { onRemovePlate(row.key) },
        )
    }
    OutlinedButton(onClick = onAddPlate, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add plate")
    }
}

@Composable
private fun PlateRow(
    row: PlateRowUi,
    error: String?,
    onDenominationChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = row.denominationKg,
                onValueChange = onDenominationChange,
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                isError = error != null,
            )
            OutlinedTextField(
                value = row.pairCount,
                onValueChange = onCountChange,
                label = { Text("pairs") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                isError = error != null,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove plate")
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EquipmentScreenPreview() {
    FitnessTheme {
        EquipmentScreen(
            state = EquipmentUiState(
                isLoading = false,
                barKg = "20",
                hasRack = true,
                hasBench = true,
                hasPullUpBar = false,
                plates = listOf(
                    PlateRowUi("a", "20", "4"),
                    PlateRowUi("b", "10", "4"),
                    PlateRowUi("c", "2.5", "4"),
                ),
                isDirty = true,
            ),
            onBack = {},
            onBarKgChange = {},
            onHasRackChange = {},
            onHasBenchChange = {},
            onHasPullUpBarChange = {},
            onPlateDenominationChange = { _, _ -> },
            onPlateCountChange = { _, _ -> },
            onAddPlate = {},
            onRemovePlate = {},
            onSave = {},
            onSavedAcknowledged = {},
        )
    }
}
