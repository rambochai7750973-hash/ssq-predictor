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
import kotlin.random.Random

class BigDataPredictor @Inject constructor() : BasePredictor() {

    override val name: String = "大数据预测"

    private val redRange = 1..33
    private val blueRange = 1..16

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val sorted = history.sortedByDescending { it.period }
        val total = sorted.size
        if (total == 0) return@withContext PredictionSet(name, emptyList())

        val shortWin = sorted.take(20)
        val midWin = sorted.take(50)

        val shortFreq = freqMap(shortWin)
        val midFreq = freqMap(midWin)
        val longFreq = freqMap(sorted)
        val omissionMap = calcOmission(sorted)
        val posMatrix = buildPositionMatrix(sorted)
        val pairMatrix = buildPairMatrix(sorted)
        val trendMap = calcTrend(shortFreq, longFreq, shortWin.size, total)

        val avgOmission = redRange.map { omissionMap[it]!! }.average().coerceAtLeast(1.0)

        val redScore = redRange.associateWith { n ->
            val sf = norm(shortFreq[n] ?: 0.0, shortFreq) * 3.0
            val mf = norm(midFreq[n] ?: 0.0, midFreq) * 2.0
            val lf = norm(longFreq[n] ?: 0.0, longFreq) * 0.8
            val om = (omissionMap[n]!! / avgOmission).coerceIn(0.0, 3.0) * 2.5
            val ps = calcPositionScore(n, posMatrix) * 1.5
            val pr = calcPairScore(n, pairMatrix) * 1.2
            val ts = (trendMap[n] ?: 0.0) * 1.8
            sf + mf + lf + om + ps + pr + ts
        }

        val ranked = redScore.entries
            .filter { filterConfig.isRedValid(it.key) }
            .sortedByDescending { it.value }
        val maxScore = ranked.firstOrNull()?.value ?: 1.0

        val groups = (1..groupCount).map { i ->
            val selected = selectGroup(ranked, redScore, filterConfig, i)
            val blue = selectBlue(sorted, filterConfig)
            val score = ((selected.sumOf { redScore[it]!! } / maxScore / 6) * 100)
                .toInt().coerceIn(0, 100)
            val reasons = selected.map { num ->
                val s = redScore[num]!!
                val posScore = calcPositionScore(num, posMatrix)
                val label = when {
                    s > maxScore * 0.7 -> "多维强势"
                    omissionMap[num]!! > avgOmission * 1.5 -> "遗漏回补"
                    (trendMap[num] ?: 0.0) > 0.05 -> "趋势上升"
                    (shortFreq[num] ?: 0) > (midFreq[num] ?: 0) -> "近期活跃"
                    posScore > 0.4 -> "位置偏好"
                    else -> "综合评分"
                }
                BallReason(number = num, reason = label, type = ReasonType.BIG_DATA)
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

    private fun freqMap(draws: List<DrawEntity>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        draws.forEach { d ->
            listOf(d.red1, d.red2, d.red3, d.red4, d.red5, d.red6).forEach { map[it] = map.getOrDefault(it, 0) + 1 }
        }
        return map
    }

    private fun norm(value: Double, map: Map<Int, Int>): Double {
        val max = map.values.maxOrNull()?.toDouble() ?: return 0.0
        return if (max > 0) value / max else 0.0
    }

    private fun calcOmission(sorted: List<DrawEntity>): Map<Int, Int> {
        val om = redRange.associateWith { sorted.size }.toMutableMap()
        for ((idx, draw) in sorted.withIndex()) {
            val reds = setOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
            redRange.forEach { n ->
                if (n in reds) om[n] = idx
            }
        }
        return om
    }

    private fun buildPositionMatrix(sorted: List<DrawEntity>): Array<IntArray> {
        val mat = Array(7) { IntArray(34) }
        for (draw in sorted) {
            val reds = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).sorted()
            reds.forEachIndexed { pos, num -> mat[pos + 1][num]++ }
        }
        return mat
    }

    private fun buildPairMatrix(sorted: List<DrawEntity>): Array<IntArray> {
        val mat = Array(34) { IntArray(34) }
        for (draw in sorted) {
            val reds = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
            for (i in reds.indices) {
                for (j in i + 1 until reds.size) {
                    mat[reds[i]][reds[j]]++
                    mat[reds[j]][reds[i]]++
                }
            }
        }
        return mat
    }

    private fun calcTrend(
        shortFreq: Map<Int, Int>,
        longFreq: Map<Int, Int>,
        shortTotal: Int,
        longTotal: Int
    ): Map<Int, Double> {
        val baseRate = longTotal.toDouble() / 6.0
        return redRange.associateWith { n ->
            val shortRate = shortFreq[n]?.toDouble() ?: 0.0
            val longRate = (longFreq[n]?.toDouble() ?: 0.0) / baseRate
            if (longRate > 0) (shortRate - longRate) / longRate else 0.0
        }
    }

    private fun calcPositionScore(n: Int, mat: Array<IntArray>): Double {
        var score = 0.0
        for (pos in 1..6) {
            val colSum = (1..33).sumOf { mat[pos][it] }
            if (colSum > 0) score += mat[pos][n].toDouble() / colSum
        }
        return score
    }

    private fun calcPairScore(n: Int, mat: Array<IntArray>): Double {
        val total = (1..33).sumOf { mat[n][it] }
        val distinct = (1..33).count { mat[n][it] > 0 }
        return if (total > 0) (total.toDouble() / distinct.coerceAtLeast(1)) * 0.1 else 0.0
    }

    private fun selectGroup(
        ranked: List<Map.Entry<Int, Double>>,
        scores: Map<Int, Double>,
        filter: FilterConfig,
        seed: Int
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = Random(seed.toLong())
        val pool = ranked.filter { it.key !in selected }.toMutableList()

        while (selected.size < 6 && pool.isNotEmpty()) {
            val needZones = calcZoneNeeds(selected)
            val needOdd = 3 - selected.count { it % 2 == 1 }
            val needEven = (6 - selected.size) - needOdd

            val candidates = pool.filter { (num) ->
                val z = when (num) { in 1..11 -> 0; in 12..22 -> 1; else -> 2 }
                val zoneOk = needZones[z] > 0 || selected.size >= 5
                val oeOk = if (needOdd > 0 && needEven <= 0) num % 2 == 1
                    else if (needEven > 0 && needOdd <= 0) num % 2 == 0
                    else true
                zoneOk && oeOk
            }

            val chosen = if (candidates.isNotEmpty()) {
                val weights = candidates.map { (_, score) -> score }
                val total = weights.sum()
                var r = rand.nextDouble() * total
                var idx = candidates.size - 1
                for ((i, w) in weights.withIndex()) {
                    r -= w
                    if (r <= 0) { idx = i; break }
                }
                candidates[idx].key
            } else {
                pool[rand.nextInt(pool.size)].key
            }

            selected.add(chosen)
            pool.removeAll { it.key == chosen }
        }

        return selected.toList()
    }

    private fun calcZoneNeeds(selected: Set<Int>): IntArray {
        val zones = intArrayOf(0, 0, 0)
        selected.forEach { n ->
            when (n) { in 1..11 -> zones[0]++; in 12..22 -> zones[1]++; else -> zones[2]++ }
        }
        val target = intArrayOf(2, 2, 2)
        return IntArray(3) { (target[it] - zones[it]).coerceAtLeast(0) }
    }

    private fun selectBlue(sorted: List<DrawEntity>, filter: FilterConfig): Int {
        if (filter.mustIncludeBlue != null) return filter.mustIncludeBlue
        val candidates = blueRange.filter { it !in filter.excludeBlues }
        if (candidates.isEmpty()) return 1

        val short = sorted.take(20)
        val shortFreq = mutableMapOf<Int, Int>()
        val longFreq = mutableMapOf<Int, Int>()
        short.forEach { shortFreq[it.blue] = shortFreq.getOrDefault(it.blue, 0) + 1 }
        sorted.forEach { longFreq[it.blue] = longFreq.getOrDefault(it.blue, 0) + 1 }

        val avgLong = sorted.size.toDouble() / 16
        val scored = candidates.map { n ->
            val sf = shortFreq.getOrDefault(n, 0).toDouble() / short.size.coerceAtLeast(1) * 3
            val lf = longFreq.getOrDefault(n, 0).toDouble() / avgLong * 0.5
            val om = sorted.indexOfFirst { it.blue == n }.let { if (it < 0) sorted.size else it }.toDouble() / sorted.size * 2
            n to (sf + lf + om)
        }
        return scored.maxByOrNull { it.second }?.first ?: candidates.first()
    }
}
