package com.ssq.predictor.domain.usecase

import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.data.local.entity.PredictionRecordEntity
import com.ssq.predictor.data.repository.DrawRepository
import com.ssq.predictor.data.repository.PredictionRepository
import com.ssq.predictor.domain.model.DrawMatchResult
import com.ssq.predictor.domain.model.SimulationBatchResult
import com.ssq.predictor.domain.model.SimulationGroupResult
import com.ssq.predictor.domain.model.SimulationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatePurchaseUseCase @Inject constructor(
    private val predictionRepository: PredictionRepository,
    private val drawRepository: DrawRepository
) {
    suspend operator fun invoke(): SimulationResult {
        val records = predictionRepository.getAllRecordsOnce()
        val draws = drawRepository.getAllDrawsOnce()

        if (records.isEmpty() || draws.isEmpty()) {
            return SimulationResult(0, 0, 0, 0, 0, emptyList())
        }

        val groupedByBatch = records.groupBy { it.batchId }
            .toSortedMap(compareByDescending { it })

        val batchResults = groupedByBatch.map { (_, batchRecords) ->
            val groups = batchRecords.map { record ->
                val reds = listOf(record.red1, record.red2, record.red3, record.red4, record.red5, record.red6)
                val userReds = reds.toSet()
                val userBlue = record.blue

                val matches = draws.mapNotNull { draw ->
                    val winReds = setOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
                    val redMatchCount = userReds.intersect(winReds).size
                    val blueMatch = userBlue == draw.blue

                    val level = when {
                        redMatchCount == 6 && blueMatch -> PrizeLevel.FIRST
                        redMatchCount == 6 -> PrizeLevel.SECOND
                        redMatchCount == 5 && blueMatch -> PrizeLevel.THIRD
                        redMatchCount == 5 || (redMatchCount == 4 && blueMatch) -> PrizeLevel.FOURTH
                        redMatchCount == 4 || (redMatchCount == 3 && blueMatch) -> PrizeLevel.FIFTH
                        blueMatch -> PrizeLevel.SIXTH
                        else -> null
                    }

                    if (level != null) {
                        DrawMatchResult(
                            period = draw.period,
                            date = draw.date,
                            redMatches = redMatchCount,
                            blueMatch = blueMatch,
                            prizeLevel = level
                        )
                    } else {
                        null
                    }
                }

                SimulationGroupResult(reds = reds, blue = userBlue, matches = matches)
            }

            SimulationBatchResult(
                algorithmName = batchRecords.first().algorithmName,
                timestamp = batchRecords.first().timestamp,
                groups = groups
            )
        }

        val totalGroups = batchResults.sumOf { it.groups.size }
        val totalCost = batchResults.sumOf { it.batchCost }
        val totalPrize = batchResults.sumOf { it.batchPrize }
        val winCount = batchResults.sumOf { it.batchWinCount }

        return SimulationResult(
            totalBatches = batchResults.size,
            totalGroups = totalGroups,
            totalCost = totalCost,
            totalPrize = totalPrize,
            winCount = winCount,
            batchResults = batchResults
        )
    }
}
