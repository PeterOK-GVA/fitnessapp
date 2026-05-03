package com.peter.fitness.feature.hello

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HelloUiState(
    val greeting: String = "Phase 0 wired up",
    val tapCount: Int = 0,
)

@HiltViewModel
class HelloViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HelloUiState())
    val uiState: StateFlow<HelloUiState> = _uiState.asStateFlow()

    fun onTap() {
        _uiState.update { it.copy(tapCount = it.tapCount + 1) }
    }
}
