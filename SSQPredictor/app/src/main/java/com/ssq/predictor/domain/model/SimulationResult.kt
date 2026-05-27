package com.ssq.predictor.domain.model

import com.ssq.predictor.domain.usecase.PrizeLevel

data class SimulationResult(
    val totalBatches: Int,
    val totalGroups: Int,
    val totalCost: Int,
    val totalPrize: Int,
    val winCount: Int,
    val batchResults: List<SimulationBatchResult>
) {
    val netProfit: Int get() = totalPrize - totalCost
    val winRate: Float get() = if (totalGroups > 0) winCount.toFloat() / totalGroups else 0f
}

data class SimulationBatchResult(
    val algorithmName: String,
    val timestamp: Long,
    val groups: List<SimulationGroupResult>
) {
    val batchCost: Int get() = groups.size * 2
    val batchPrize: Int get() = groups.sumOf { it.bestPrizeAmount }
    val batchWinCount: Int get() = groups.count { it.bestPrize != null }
}

data class SimulationGroupResult(
    val reds: List<Int>,
    val blue: Int,
    val matches: List<DrawMatchResult>
) {
    val bestPrize: PrizeLevel? get() = matches.minOfOrNull { it.prizeLevel?.ordinal ?: Int.MAX_VALUE }?.let { ordinal ->
        PrizeLevel.entries.find { it.ordinal == ordinal }
    }
    val bestPrizeAmount: Int get() = bestPrize?.amount ?: 0
}

data class DrawMatchResult(
    val period: String,
    val date: String,
    val redMatches: Int,
    val blueMatch: Boolean,
    val prizeLevel: PrizeLevel?
)
