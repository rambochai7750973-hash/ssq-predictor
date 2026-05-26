package com.ssq.predictor.domain.algorithm

import com.ssq.predictor.data.local.entity.DrawEntity
import com.ssq.predictor.domain.model.BallReason
import com.ssq.predictor.domain.model.FilterConfig
import com.ssq.predictor.domain.model.PredictionGroup
import com.ssq.predictor.domain.model.PredictionSet
import com.ssq.predictor.domain.model.ReasonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow
import kotlin.random.Random

class MarkovPredictor @Inject constructor() : BasePredictor() {

    override val name: String = "马尔可夫链"

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val sorted = history.sortedBy { it.period }
        val transitionMatrix = buildTransitionMatrix(sorted)
        val lastDraw = sorted.lastOrNull()

        val redProbs = calculateRedProbabilities(transitionMatrix, lastDraw)
        val blueProbs = calculateBlueProbabilities(sorted)

        val ranked = redProbs.entries
            .filter { filterConfig.isRedValid(it.key) }
            .sortedByDescending { it.value }

        val maxProb = ranked.firstOrNull()?.value ?: 1.0

        val groups = (1..groupCount).map { i ->
            val selected = selectByProbability(ranked, filterConfig, i)
            val blue = selectBlueByProbability(blueProbs, filterConfig)
            val score = ((selected.sumOf { redProbs[it]!! } / maxProb / 6) * 100)
                .toInt().coerceIn(0, 100)
            val reasons = selected.map { num ->
                BallReason(
                    number = num,
                    reason = "转移概率 ${"%.1f".format(redProbs[num]!! * 100)}%",
                    type = ReasonType.MARKOV
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

    private fun buildTransitionMatrix(sorted: List<DrawEntity>): Array<DoubleArray> {
        val size = 34
        val matrix = Array(size) { DoubleArray(size) }
        val counts = Array(size) { IntArray(size) }

        for (i in 0 until sorted.size - 1) {
            val currentReds = getRedSet(sorted[i])
            val nextReds = getRedSet(sorted[i + 1])
            for (from in currentReds) {
                for (to in nextReds) {
                    counts[from][to]++
                }
            }
        }

        for (i in 1..33) {
            val total = counts[i].sum()
            if (total > 0) {
                for (j in 1..33) {
                    matrix[i][j] = counts[i][j].toDouble() / total
                }
            }
        }
        return matrix
    }

    private fun calculateRedProbabilities(
        matrix: Array<DoubleArray>,
        lastDraw: DrawEntity?
    ): Map<Int, Double> {
        val probs = mutableMapOf<Int, Double>()
        if (lastDraw == null) {
            (1..33).forEach { probs[it] = 1.0 / 33 }
            return probs
        }
        val lastReds = getRedSet(lastDraw)
        for (n in 1..33) {
            var prob = 0.0
            for (from in lastReds) {
                prob += matrix[from][n]
            }
            probs[n] = prob / lastReds.size
        }
        val maxProb = probs.values.maxOrNull() ?: 1.0
        if (maxProb > 0) {
            probs.forEach { (k, v) -> probs[k] = v / maxProb }
        }
        (1..33).forEach { n ->
            if (probs[n] == 0.0) probs[n] = 0.01
        }
        return probs
    }

    private fun calculateBlueProbabilities(sorted: List<DrawEntity>): Map<Int, Double> {
        val counts = mutableMapOf<Int, Int>()
        sorted.forEach { counts[it.blue] = counts.getOrDefault(it.blue, 0) + 1 }
        val total = sorted.size.toDouble()
        return (1..16).associateWith { counts.getOrDefault(it, 0).toDouble() / total }
    }

    private fun selectByProbability(
        ranked: List<Map.Entry<Int, Double>>,
        filter: FilterConfig,
        seed: Int
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = Random(seed.toLong())
        while (selected.size < 6) {
            val pool = ranked.filter { it.key !in selected }
            if (pool.isEmpty()) break
            val weights = pool.map { it.value }
            val total = weights.sum()
            var r = rand.nextDouble() * total
            var chosen = pool.last().key
            for ((index, entry) in pool.withIndex()) {
                r -= weights[index]
                if (r <= 0) {
                    chosen = entry.key
                    break
                }
            }
            if (chosen !in selected) selected.add(chosen)
        }
        return selected.toList()
    }

    private fun selectBlueByProbability(probs: Map<Int, Double>, filter: FilterConfig): Int {
        val candidates = probs.filterKeys { it !in filter.excludeBlues }
        return filter.mustIncludeBlue ?: candidates.maxByOrNull { it.value }?.key ?: 1
    }

    private fun getRedSet(draw: DrawEntity): Set<Int> =
        setOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
}
