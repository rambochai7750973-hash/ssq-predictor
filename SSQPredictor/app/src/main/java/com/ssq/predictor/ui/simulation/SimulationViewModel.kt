package com.ssq.predictor.ui.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssq.predictor.domain.model.SimulationResult
import com.ssq.predictor.domain.usecase.SimulatePurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SimulationUiState(
    val result: SimulationResult? = null,
    val isLoading: Boolean = false,
    val hasRecords: Boolean = true
)

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val simulatePurchaseUseCase: SimulatePurchaseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    init {
        runSimulation()
    }

    fun runSimulation() {
        viewModelScope.launch {
            _uiState.value = SimulationUiState(isLoading = true)
            try {
                val result = simulatePurchaseUseCase()
                _uiState.value = SimulationUiState(
                    result = result,
                    hasRecords = result.totalGroups > 0
                )
            } catch (e: Exception) {
                _uiState.value = SimulationUiState(hasRecords = false)
            }
        }
    }
}
