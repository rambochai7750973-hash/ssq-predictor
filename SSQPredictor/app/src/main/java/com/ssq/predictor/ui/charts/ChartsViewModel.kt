package com.ssq.predictor.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssq.predictor.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartsUiState(
    val redOmissions: Map<Int, Int> = emptyMap(),
    val blueOmissions: Map<Int, Int> = emptyMap(),
    val redFrequency: Map<Int, Int> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val repository: DrawRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val draws = repository.getAllDrawsOnce()
            if (draws.isEmpty()) return@launch

            val redFreq = mutableMapOf<Int, Int>()
            val blueFreq = mutableMapOf<Int, Int>()
            val redOmission = mutableMapOf<Int, Int>()
            val blueOmission = mutableMapOf<Int, Int>()

            for (n in 1..33) { redFreq[n] = 0; redOmission[n] = 0 }
            for (n in 1..16) { blueFreq[n] = 0; blueOmission[n] = 0 }

            val sorted = draws.sortedByDescending { it.period }
            var redFound = false
            var blueFound = false

            for (draw in sorted) {
                listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).forEach {
                    redFreq[it] = redFreq.getOrDefault(it, 0) + 1
                }
                blueFreq[draw.blue] = blueFreq.getOrDefault(draw.blue, 0) + 1

                val redsSet = setOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
                for (n in 1..33) {
                    if (n in redsSet) {
                        redOmission[n] = 0
                        redFound = true
                    } else if (redFound) {
                        redOmission[n] = redOmission[n]!! + 1
                    }
                }
                for (n in 1..16) {
                    if (n == draw.blue) {
                        blueOmission[n] = 0
                        blueFound = true
                    } else if (blueFound) {
                        blueOmission[n] = blueOmission[n]!! + 1
                    }
                }
            }

            _uiState.value = ChartsUiState(
                redOmissions = redOmission,
                blueOmissions = blueOmission,
                redFrequency = redFreq,
                isLoading = false
            )
        }
    }
}
