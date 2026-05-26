package com.ssq.predictor.domain.algorithm

import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.domain.model.BallReason
import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionGroup
import com.ssq.predictor.domain.model.PredictionSet
import com.ssq.predictor.domain.model.ReasonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope

class EnsemblePredictor @Inject constructor(
    private val hotCold: HotColdPredictor,
    private val omission: OmissionPredictor,
    private val markov: MarkovPredictor,
    private val distribution: DistributionPredictor
) : BasePredictor() {

    override val name: String = "集成预测"

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val results = coroutineScope {
            listOf(
                async { hotCold.predict(history, filterConfig, groupCount) },
                async { omission.predict(history, filterConfig, groupCount) },
                async { markov.predict(history, filterConfig, groupCount) },
                async { distribution.predict(history, filterConfig, groupCount) }
            ).map { it.await() }
        }

        val redFrequency = mutableMapOf<Int, Int>()
        val blueFrequency = mutableMapOf<Int, Int>()
        val redScores = mutableMapOf<Int, MutableList<Int>>()
        val redReasons = mutableMapOf<Int, MutableList<String>>()

        results.forEach { result ->
            result.groups.forEach { group ->
                group.reds.forEach { red ->
                    redFrequency[red] = redFrequency.getOrDefault(red, 0) + 1
                    redScores.getOrPut(red) { mutableListOf() }.add(group.score)
                    group.reasons.firstOrNull { it.number == red }?.let {
                        redReasons.getOrPut(red) { mutableListOf() }.add(it.reason)
                    }
                }
                blueFrequency[group.blue] = blueFrequency.getOrDefault(group.blue, 0) + 1
            }
        }

        val rankedReds = redFrequency.entries
            .filter { filterConfig.isRedValid(it.key) }
            .sortedByDescending { it.value }

        val rankedBlues = blueFrequency.entries
            .filter { it.key !in filterConfig.excludeBlues }
            .sortedByDescending { it.value }

        val maxFreq = rankedReds.firstOrNull()?.value?.toDouble() ?: 1.0

        val groups = (1..groupCount).map { i ->
            val reds = selectEnsemble(rankedReds, redFrequency, filterConfig, i)
            val blue = filterConfig.mustIncludeBlue
                ?: rankedBlues.firstOrNull()?.key ?: (1..16).random()
            val avgScore = reds.mapNotNull { redScores[it]?.average()?.toInt() }
                .let { if (it.isEmpty()) 0 else it.sum() / it.size }
            val reasons = reds.map { num ->
                BallReason(
                    number = num,
                    reason = "综合推荐 (${redFrequency.getOrDefault(num, 0)}次)",
                    type = ReasonType.ENSEMBLE
                )
            }
            PredictionGroup(
                reds = reds.sorted(),
                blue = blue,
                score = ((redFrequency.getOrDefault(reds.sumOf { redFrequency.getOrDefault(it, 0) }, 0)) / (maxFreq * 6) * 100)
                    .toInt().coerceIn(0, 100),
                reasons = reasons
            )
        }

        PredictionSet(algorithmName = name, groups = groups)
    }

    private fun selectEnsemble(
        ranked: List<Map.Entry<Int, Int>>,
        freq: Map<Int, Int>,
        filter: FilterConfig,
        seed: Int
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = kotlin.random.Random(seed.toLong())
        val pool = ranked.filter { it.key !in selected }
        var idx = 0
        while (selected.size < 6) {
            if (idx < pool.size) {
                if (pool[idx].key !in selected) selected.add(pool[idx].key)
                idx++
            } else {
                val remaining = (1..33).filter { filter.isRedValid(it) && it !in selected }
                if (remaining.isEmpty()) break
                selected.add(remaining.random(rand))
            }
        }
        return selected.toList()
    }
}
