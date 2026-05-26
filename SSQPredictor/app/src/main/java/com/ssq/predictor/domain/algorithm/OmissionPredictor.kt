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
import kotlin.random.Random

class OmissionPredictor @Inject constructor() : BasePredictor() {

    override val name: String = "遗漏分析"

    override suspend fun predict(
        history: List<DrawEntity>,
        filterConfig: FilterConfig,
        groupCount: Int
    ): PredictionSet = withContext(Dispatchers.Default) {
        val sorted = history.sortedByDescending { it.period }

        val redOmissions = calculateRedOmissions(sorted)
        val blueOmissions = calculateBlueOmissions(sorted)

        val avgOmission = redOmissions.values.average()
        val maxOmission = redOmissions.values.max()

        val ranked = redOmissions.entries
            .filter { filterConfig.isRedValid(it.key) }
            .sortedByDescending { it.value }

        val groups = (1..groupCount).map { i ->
            val selected = selectByOmission(ranked, filterConfig, i, avgOmission)
            val blue = selectBlueByOmission(blueOmissions, filterConfig)
            val score = ((selected.sumOf { redOmissions[it]!! } / maxOmission / 6) * 100)
                .toInt().coerceIn(0, 100)
            val reasons = selected.map { num ->
                val omission = redOmissions[num]!!
                BallReason(
                    number = num,
                    reason = "遗漏${omission}期",
                    type = ReasonType.OMISSION
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

    private fun calculateRedOmissions(sorted: List<DrawEntity>): Map<Int, Int> {
        val omissions = mutableMapOf<Int, Int>()
        for (n in 1..33) omissions[n] = 0
        var found = false
        for (draw in sorted) {
            val reds = setOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
            for (n in 1..33) {
                if (n in reds) {
                    omissions[n] = 0
                    found = true
                } else if (found) {
                    omissions[n] = omissions[n]!! + 1
                }
            }
        }
        for (n in 1..33) {
            if (omissions[n] == 0 && sorted.isNotEmpty()) {
                val latestReds = setOf(
                    sorted.first().red1, sorted.first().red2, sorted.first().red3,
                    sorted.first().red4, sorted.first().red5, sorted.first().red6
                )
                if (n !in latestReds) omissions[n] = 1
            }
        }
        return omissions
    }

    private fun calculateBlueOmissions(sorted: List<DrawEntity>): Map<Int, Int> {
        val omissions = mutableMapOf<Int, Int>()
        for (n in 1..16) omissions[n] = 0
        var found = false
        for (draw in sorted) {
            for (n in 1..16) {
                if (n == draw.blue) {
                    omissions[n] = 0
                    found = true
                } else if (found) {
                    omissions[n] = omissions[n]!! + 1
                }
            }
        }
        return omissions
    }

    private fun selectByOmission(
        ranked: List<Map.Entry<Int, Int>>,
        filter: FilterConfig,
        seed: Int,
        avgOmission: Double
    ): List<Int> {
        val selected = filter.mustIncludeReds.toMutableSet()
        val rand = Random(seed.toLong())
        val highOmission = ranked.filter { it.value >= avgOmission }
        val lowOmission = ranked.filter { it.value < avgOmission }

        while (selected.size < 6) {
            val pool = if (selected.size < 4 && highOmission.isNotEmpty()) {
                highOmission.filter { it.key !in selected }
            } else {
                ranked.filter { it.key !in selected }
            }
            if (pool.isEmpty()) break
            val weights = pool.map { it.value.toDouble() }
            val totalWeight = weights.sum()
            var r = rand.nextDouble() * totalWeight
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

    private fun selectBlueByOmission(omissions: Map<Int, Int>, filter: FilterConfig): Int {
        val ranked = (1..16)
            .filter { it !in filter.excludeBlues }
            .sortedByDescending { omissions[it] }
        return filter.mustIncludeBlue ?: ranked.first()
    }
}
