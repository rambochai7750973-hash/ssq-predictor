package com.ssq.predictor.domain.algorithm

import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.domain.model.BallReason
import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionGroup
import com.ssq.predictor.domain.model.PredictionSet
import com.ssq.predictor.domain.model.ReasonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TreeSet
import javax.inject.Inject
import kotlin.math.pow
import kotlin.random.Random

class HotColdPredictor @Inject constructor() : BasePredictor() {

    override val name: String = "热冷分析"

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val sorted = history.sortedByDescending { it.period }
        val recentWindow = sorted.take(50)
        val midWindow = sorted.drop(50).take(100)
        val allWindow = sorted

        val hotScore = scoreNumbers(recentWindow, weight = 3.0)
        val warmScore = scoreNumbers(midWindow, weight = 1.5)
        val totalScore = scoreNumbers(allWindow, weight = 0.5)

        val combined = (1..33).associateWith { n ->
            hotScore.getOrDefault(n, 0.0) +
            warmScore.getOrDefault(n, 0.0) +
            totalScore.getOrDefault(n, 0.0)
        }

        val ranked = combined.entries
            .filter { filterConfig.isRedValid(it.key) }
            .sortedByDescending { it.value }

        val groups = (1..groupCount).map { i ->
            val selected = selectRedGroup(ranked, filterConfig, i)
            val blue = selectHotBlue(sorted, filterConfig)
            val score = ((selected.sumOf { ranked[it]!! } / ranked.first().value / 6) * 100)
                .toInt().coerceIn(0, 100)
            val reasons = selected.map { num ->
                BallReason(
                    number = num,
                    reason = if (ranked[num]!! > combined.values.average()) "热号" else "温号",
                    type = ReasonType.HOT
                )
            }
            PredictionGroup(
                reds = selected.sorted(),
                blue = blue,
                score = score,
                reasons = reasons
            )
        }

        PredictionSet(algorithmName = name, groups = groups)
    }

    private fun scoreNumbers(draws: List<DrawEntity>, weight: Double): Map<Int, Double> {
        val freq = mutableMapOf<Int, Int>()
        draws.forEach { draw ->
            listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).forEach {
                freq[it] = freq.getOrDefault(it, 0) + 1
            }
        }
        val total = draws.size * 6
        return (1..33).associateWith { n ->
            (freq.getOrDefault(n, 0).toDouble() / total * 100) * weight
        }
    }

    private fun selectRedGroup(
        ranked: List<Map.Entry<Int, Double>>,
        filter: FilterConfig,
        seed: Int
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = Random(seed.toLong())
        val candidates = ranked.filter { it.key !in selected }
        while (selected.size < 6) {
            val idx = (rand.nextDouble().pow(2) * candidates.size).toInt().coerceIn(0, candidates.size - 1)
            val num = candidates[idx].key
            if (num !in selected) selected.add(num)
        }
        return selected.toList()
    }

    private fun selectHotBlue(draws: List<DrawEntity>, filter: FilterConfig): Int {
        val freq = mutableMapOf<Int, Int>()
        draws.forEach { freq[it.blue] = freq.getOrDefault(it.blue, 0) + 1 }
        val ranked = (1..16)
            .filter { it !in filter.excludeBlues }
            .sortedByDescending { freq.getOrDefault(it, 0) }
        return filter.mustIncludeBlue ?: ranked.first()
    }
}
