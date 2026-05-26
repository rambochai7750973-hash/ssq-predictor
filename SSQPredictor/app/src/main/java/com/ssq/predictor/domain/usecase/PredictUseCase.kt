package com.ssq.predictor.domain.usecase

import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.data.repository.DrawRepository
import com.ssq.predictor.domain.algorithm.BasePredictor
import com.ssq.predictor.domain.algorithm.BigDataPredictor
import com.ssq.predictor.domain.algorithm.DistributionPredictor
import com.ssq.predictor.domain.algorithm.EnsemblePredictor
import com.ssq.predictor.domain.algorithm.HotColdPredictor
import com.ssq.predictor.domain.algorithm.MarkovPredictor
import com.ssq.predictor.domain.algorithm.OmissionPredictor
import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictUseCase @Inject constructor(
    private val repository: DrawRepository,
    private val hotColdPredictor: HotColdPredictor,
    private val omissionPredictor: OmissionPredictor,
    private val markovPredictor: MarkovPredictor,
    private val distributionPredictor: DistributionPredictor,
    private val ensemblePredictor: EnsemblePredictor,
    private val bigDataPredictor: BigDataPredictor
) {
    private val predictors: Map<String, BasePredictor> = mapOf(
        hotColdPredictor.name to hotColdPredictor,
        omissionPredictor.name to omissionPredictor,
        markovPredictor.name to markovPredictor,
        distributionPredictor.name to distributionPredictor,
        ensemblePredictor.name to ensemblePredictor,
        bigDataPredictor.name to bigDataPredictor
    )

    fun getAlgorithmNames(): List<String> = predictors.keys.toList()

    suspend fun predict(
        algorithmName: String,
        filterConfig: FilterConfig = FilterConfig(),
        groupCount: Int = 5
    ): PredictionSet {
        val predictor = predictors[algorithmName]
            ?: throw IllegalArgumentException("未知算法: $algorithmName")
        val history = repository.getAllDrawsOnce()
        return predictor.predict(history, filterConfig, groupCount)
    }
}
