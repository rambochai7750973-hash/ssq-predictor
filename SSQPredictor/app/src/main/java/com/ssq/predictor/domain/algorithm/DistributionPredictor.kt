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

class DistributionPredictor @Inject constructor() : BasePredictor() {

    override val name: String = "区间分布"

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val sorted = history.sortedByDescending { it.period }
        val recent = sorted.take(50)

        val targetRatio = calculateTargetOddEvenRatio(recent)
        val targetRegion = calculateTargetRegionDistribution(recent)

        val groups = (1..groupCount).map { i ->
            val selected = generateByDistribution(
                targetRatio, targetRegion, filterConfig, i
            )
            val blue = pickBalancedBlue(recent, filterConfig)
            val quality = evaluateDistribution(selected, targetRatio, targetRegion)
            val reasons = selected.map { num ->
                BallReason(
                    number = num,
                    reason = when {
                        num % 2 == 1 -> "奇数均衡"
                        else -> "偶数均衡"
                    },
                    type = ReasonType.DISTRIBUTION
                )
            }
            PredictionGroup(
                reds = selected.sorted(),
                blue = blue,
                score = quality.coerceIn(0, 100),
                reasons = reasons
            )
        }

        PredictionSet(algorithmName = name, groups = groups)
    }

    private fun calculateTargetOddEvenRatio(recent: List<DrawEntity>): Double {
        val totalOdd = recent.sumOf { draw ->
            listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
                .count { it % 2 == 1 }
        }
        return totalOdd.toDouble() / (recent.size * 6)
    }

    private fun calculateTargetRegionDistribution(recent: List<DrawEntity>): Triple<Double, Double, Double> {
        var r1 = 0; var r2 = 0; var r3 = 0
        recent.forEach { draw ->
            listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).forEach { n ->
                when {
                    n in 1..11 -> r1++
                    n in 12..22 -> r2++
                    else -> r3++
                }
            }
        }
        val total = recent.size * 6.0
        return Triple(r1 / total, r2 / total, r3 / total)
    }

    private fun generateByDistribution(
        targetRatio: Double,
        targetRegion: Triple<Double, Double, Double>,
        filter: FilterConfig,
        seed: Int
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = Random(seed.toLong())

        val targetOdd = (targetRatio * 6).toInt().coerceIn(0, 6)
        val currentOdd = selected.count { it % 2 == 1 }

        val targetR1 = (targetRegion.first * 6).toInt().coerceIn(0, 6)
        val targetR2 = (targetRegion.second * 6).toInt().coerceIn(0, 6)
        val currentR1 = selected.count { it in 1..11 }
        val currentR2 = selected.count { it in 12..22 }

        val pool = (1..33).toMutableList()
        pool.removeAll(selected)
        pool.removeAll(filter.excludeReds.toList())

        while (selected.size < 6 && pool.isNotEmpty()) {
            val neededOdd = targetOdd - currentOdd
            val neededEven = 6 - targetOdd - (selected.size - currentOdd)
            val neededR1 = targetR1 - currentR1
            val neededR2 = targetR2 - currentR2
            val neededR3 = 6 - targetR1 - targetR2 - (selected.size - currentR1 - currentR2)

            val candidates = pool.filter { num ->
                val oddOk = if (neededOdd > 0 && neededEven <= 0) num % 2 == 1
                    else if (neededEven > 0 && neededOdd <= 0) num % 2 == 0
                    else true
                val regionOk = if (neededR1 > 0 && neededR2 <= 0 && neededR3 <= 0) num in 1..11
                    else if (neededR2 > 0 && neededR1 <= 0 && neededR3 <= 0) num in 12..22
                    else if (neededR3 > 0 && neededR1 <= 0 && neededR2 <= 0) num in 23..33
                    else true
                oddOk && regionOk
            }

            if (candidates.isEmpty()) {
                if (pool.isEmpty()) break
                val pick = pool[rand.nextInt(pool.size)]
                selected.add(pick)
                pool.remove(pick)
            } else {
                val pick = candidates[rand.nextInt(candidates.size)]
                selected.add(pick)
                pool.remove(pick)
            }
        }
        return selected.toList()
    }

    private fun pickBalancedBlue(recent: List<DrawEntity>, filter: FilterConfig): Int {
        val freq = mutableMapOf<Int, Int>()
        recent.forEach { freq[it.blue] = freq.getOrDefault(it.blue, 0) + 1 }
        return filter.mustIncludeBlue ?: (1..16)
            .filter { it !in filter.excludeBlues }
            .minByOrNull { freq.getOrDefault(it, 0) } ?: 1
    }

    private fun evaluateDistribution(
        reds: List<Int>,
        targetRatio: Double,
        targetRegion: Triple<Double, Double, Double>
    ): Int {
        val oddCount = reds.count { it % 2 == 1 }
        val oddDiff = Math.abs(oddCount / 6.0 - targetRatio)
        val r1 = reds.count { it in 1..11 }
        val r2 = reds.count { it in 12..22 }
        val r3 = reds.count { it in 23..33 }
        val r1Diff = Math.abs(r1 / 6.0 - targetRegion.first)
        val r2Diff = Math.abs(r2 / 6.0 - targetRegion.second)
        val r3Diff = Math.abs(r3 / 6.0 - targetRegion.third)

        val totalDiff = oddDiff + r1Diff + r2Diff + r3Diff
        return (100 - totalDiff * 50).toInt().coerceIn(0, 100)
    }
}
