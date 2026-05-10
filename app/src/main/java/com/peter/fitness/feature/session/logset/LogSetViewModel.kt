package com.peter.fitness.feature.session.logset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peter.fitness.core.ids.IdFactory
import com.peter.fitness.domain.model.EquipmentInventory
import com.peter.fitness.domain.model.ExerciseId
import com.peter.fitness.domain.model.SessionId
import com.peter.fitness.domain.model.SetEntry
import com.peter.fitness.domain.model.SetEntryId
import com.peter.fitness.domain.model.SubjectiveLoad
import com.peter.fitness.domain.model.TechniqueRating
import com.peter.fitness.domain.plates.PlateCalculator
import com.peter.fitness.domain.repository.EquipmentInventoryRepository
import com.peter.fitness.domain.repository.ExerciseRepository
import com.peter.fitness.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class LogSetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val equipmentRepository: EquipmentInventoryRepository,
    private val idFactory: IdFactory,
    private val clock: Clock,
) : ViewModel() {

    private val sessionId: SessionId = SessionId(
        requireNotNull(savedStateHandle.get<String>(KEY_SESSION_ID)) {
            "Missing $KEY_SESSION_ID in SavedStateHandle"
        },
    )
    private val exerciseIdArg: ExerciseId? =
        savedStateHandle.get<String>(KEY_EXERCISE_ID)?.let(::ExerciseId)
    private val setEntryIdArg: SetEntryId? =
        savedStateHandle.get<String>(KEY_SET_ENTRY_ID)?.let(::SetEntryId)

    private var currentInventory: EquipmentInventory? = null

    private val _uiState = MutableStateFlow(
        LogSetUiState(isEditMode = setEntryIdArg != null),
    )
    val uiState: StateFlow<LogSetUiState> = _uiState.asStateFlow()

    private val _events = Channel<LogSetEvent>(capacity = Channel.BUFFERED)
    val events: Flow<LogSetEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadInitialState() }
    }

    private suspend fun loadInitialState() {
        currentInventory = equipmentRepository.current()
        if (setEntryIdArg != null) {
            val existing = sessionRepository.observeSetEntries(sessionId).first()
                .firstOrNull { it.id == setEntryIdArg }
            if (existing == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return
            }
            val name = exerciseRepository.findById(existing.exerciseId)?.name ?: "Exercise"
            val initialLoad = (existing.performedLoadKg ?: existing.targetLoadKg).toCleanString()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    exerciseId = existing.exerciseId.value,
                    exerciseName = name,
                    reps = (existing.completedReps ?: existing.targetReps).toString(),
                    loadKg = initialLoad,
                    subjectiveLoad = existing.subjectiveLoad,
                    techniqueRating = existing.techniqueRating,
                    plateHint = computePlateHint(initialLoad, currentInventory),
                )
            }
        } else {
            val exId = requireNotNull(exerciseIdArg) {
                "exerciseId is required when setEntryId is absent"
            }
            val name = exerciseRepository.findById(exId)?.name ?: "Exercise"
            _uiState.update {
                it.copy(
                    isLoading = false,
                    exerciseId = exId.value,
                    exerciseName = name,
                )
            }
        }
    }

    fun onRepsChange(value: String) {
        _uiState.update { it.copy(reps = value, repsError = null) }
    }

    fun onLoadKgChange(value: String) {
        _uiState.update {
            it.copy(
                loadKg = value,
                loadKgError = null,
                plateHint = computePlateHint(value, currentInventory),
            )
        }
    }

    fun onSubjectiveLoadChange(value: SubjectiveLoad?) {
        _uiState.update { it.copy(subjectiveLoad = value) }
    }

    fun onTechniqueRatingChange(value: TechniqueRating?) {
        _uiState.update { it.copy(techniqueRating = value) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val parsed = state.parse() ?: run {
            _uiState.update {
                it.copy(repsError = state.repsErrorMessage(), loadKgError = state.loadKgErrorMessage())
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, repsError = null, loadKgError = null) }
            if (setEntryIdArg == null) {
                addNewSet(parsed.reps, parsed.loadKg, state.subjectiveLoad, state.techniqueRating)
            } else {
                updateExistingSet(parsed.reps, parsed.loadKg, state.subjectiveLoad, state.techniqueRating)
            }
            _events.send(LogSetEvent.Saved)
        }
    }

    fun onDelete() {
        val targetId = setEntryIdArg ?: return
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            sessionRepository.deleteSetEntry(targetId)
            _events.send(LogSetEvent.Deleted)
        }
    }

    private suspend fun addNewSet(
        reps: Int,
        loadKg: Double,
        subjective: SubjectiveLoad?,
        technique: TechniqueRating?,
    ) {
        val ordinal = sessionRepository.observeSetEntries(sessionId).first().size
        val entry = SetEntry(
            id = idFactory.newSetEntryId(),
            sessionId = sessionId,
            exerciseId = ExerciseId(_uiState.value.exerciseId),
            ordinal = ordinal,
            targetReps = reps,
            targetLoadKg = loadKg,
            completedReps = reps,
            performedLoadKg = loadKg,
            subjectiveLoad = subjective,
            techniqueRating = technique,
            createdAt = clock.instant(),
        )
        sessionRepository.addSetEntry(entry)
    }

    private suspend fun updateExistingSet(
        reps: Int,
        loadKg: Double,
        subjective: SubjectiveLoad?,
        technique: TechniqueRating?,
    ) {
        val targetId = setEntryIdArg ?: return
        val existing = sessionRepository.observeSetEntries(sessionId).first()
            .firstOrNull { it.id == targetId } ?: return
        sessionRepository.updateSetEntry(
            existing.copy(
                targetReps = reps,
                targetLoadKg = loadKg,
                completedReps = reps,
                performedLoadKg = loadKg,
                subjectiveLoad = subjective,
                techniqueRating = technique,
            ),
        )
    }

    private fun computePlateHint(loadStr: String, inv: EquipmentInventory?): PlateHintUi? {
        if (inv == null) return null
        val target = loadStr.trim().toDoubleOrNull() ?: return null
        if (target < 0.0) return null
        val result = PlateCalculator.snap(target, inv)
        val onTarget = abs(result.deviationKg) < ON_TARGET_TOLERANCE_KG
        val achievable = formatKg(result.achievableKg)
        val achievableText = if (onTarget) {
            "Snaps to $achievable"
        } else {
            "Snaps to $achievable (${formatSignedKg(result.deviationKg)})"
        }
        val perSideText = if (result.perSide.isEmpty()) {
            "Bar only"
        } else {
            "Per side: " + result.perSide.joinToString(" + ") {
                "${it.countPerSide} × ${formatKg(it.denominationKg)}"
            }
        }
        return PlateHintUi(
            achievableText = achievableText,
            perSideText = perSideText,
            isOnTarget = onTarget,
        )
    }

    private data class ParsedInput(val reps: Int, val loadKg: Double)

    private fun LogSetUiState.parse(): ParsedInput? {
        val reps = reps.trim().toIntOrNull()?.takeIf { it > 0 }
        val load = loadKg.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
        return if (reps != null && load != null) ParsedInput(reps, load) else null
    }

    private fun LogSetUiState.repsErrorMessage(): String? =
        if (reps.trim().toIntOrNull()?.takeIf { it > 0 } == null) "Enter a positive number" else null

    private fun LogSetUiState.loadKgErrorMessage(): String? =
        if (loadKg.trim().toDoubleOrNull()?.takeIf { it >= 0.0 } == null) {
            "Enter a non-negative number"
        } else {
            null
        }

    private companion object {
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_EXERCISE_ID = "exerciseId"
        const val KEY_SET_ENTRY_ID = "setEntryId"
        const val ON_TARGET_TOLERANCE_KG = 0.01
    }
}

private fun Double.toCleanString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private fun formatKg(kg: Double): String =
    if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "$kg kg"

private fun formatSignedKg(kg: Double): String {
    val sign = if (kg >= 0.0) "+" else ""
    return "$sign${formatKg(kg)}"
}
