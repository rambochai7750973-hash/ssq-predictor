package com.ssq.predictor.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssq.predictor.data.local.entity.PredictionRecordEntity
import com.ssq.predictor.data.repository.PredictionRepository
import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionSet
import com.ssq.predictor.domain.usecase.PredictUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PredictionUiState(
    val algorithms: List<String> = emptyList(),
    val selectedAlgorithm: String = "",
    val result: PredictionSet? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterConfig: FilterConfig = FilterConfig(),
    val historyRecords: List<PredictionRecordEntity> = emptyList()
)

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val predictUseCase: PredictUseCase,
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            algorithms = predictUseCase.getAlgorithmNames(),
            selectedAlgorithm = predictUseCase.getAlgorithmNames().firstOrNull() ?: ""
        )
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            predictionRepository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(historyRecords = records)
            }
        }
    }

    fun selectAlgorithm(name: String) {
        _uiState.value = _uiState.value.copy(selectedAlgorithm = name)
    }

    fun predict() {
        val state = _uiState.value
        if (state.selectedAlgorithm.isEmpty() || state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, result = null)
            try {
                val result = predictUseCase.predict(
                    algorithmName = state.selectedAlgorithm,
                    filterConfig = state.filterConfig
                )
                _uiState.value = _uiState.value.copy(result = result, isLoading = false)
                savePredictionResult(result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "预测失败"
                )
            }
        }
    }

    private suspend fun savePredictionResult(result: PredictionSet) {
        val batchId = System.currentTimeMillis()
        val records = result.groups.map { group ->
            val sortedReds = group.reds.sorted()
            PredictionRecordEntity(
                batchId = batchId,
                algorithmName = result.algorithmName,
                red1 = sortedReds[0],
                red2 = sortedReds[1],
                red3 = sortedReds[2],
                red4 = sortedReds[3],
                red5 = sortedReds[4],
                red6 = sortedReds[5],
                blue = group.blue,
                score = group.score,
                timestamp = batchId
            )
        }
        predictionRepository.savePredictionBatch(records)
    }

    fun updateFilter(filter: FilterConfig) {
        _uiState.value = _uiState.value.copy(filterConfig = filter)
    }
}
