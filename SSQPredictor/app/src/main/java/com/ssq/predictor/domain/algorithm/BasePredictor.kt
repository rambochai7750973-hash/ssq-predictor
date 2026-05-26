package com.ssq.predictor.domain.algorithm

import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionSet

abstract class BasePredictor {
    abstract val name: String

    protected val redPool = (1..33).toList()
    protected val bluePool = (1..16).toList()

    abstract suspend fun predict(
        history: List<com.ssq.predictor.data.local.entity.DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int = 5
    ): PredictionSet

    protected fun pickRandomBlue(filter: FilterConfig): Int {
        val candidates = bluePool.filter { it !in filter.excludeBlues }
            .let { list ->
                filter.mustIncludeBlue?.let { if (it in list) listOf(it) else list }
                    ?: list
            }
        return candidates.random()
    }
}
