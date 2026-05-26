package com.ssq.predictor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val draws: List<DrawEntity> = emptyList(),
    val latestDraw: DrawEntity? = null,
    val isLoading: Boolean = true,
    val networkSynced: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DrawRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.initializeIfEmpty()
            _uiState.value = _uiState.value.copy(isLoading = true)

            val synced = repository.syncFromNetwork()
            val recentDraws = repository.getRecentDraws(20)

            _uiState.value = HomeUiState(
                draws = recentDraws,
                latestDraw = recentDraws.firstOrNull(),
                isLoading = false,
                networkSynced = synced
            )
        }
    }
}
