package com.ssq.predictor.domain.model

data class FilterConfig(
    val mustIncludeReds: Set<Int> = emptySet(),
    val excludeReds: Set<Int> = emptySet(),
    val mustIncludeBlue: Int? = null,
    val excludeBlues: Set<Int> = emptySet(),
    val minSum: Int = 70,
    val maxSum: Int = 140,
    val minSpan: Int = 18,
    val maxSpan: Int = 32,
    val oddEvenRatio: List<Pair<Int, Int>> = listOf(3 to 3, 4 to 2, 2 to 4)
) {
    fun isRedValid(number: Int): Boolean {
        return number !in excludeReds
    }

    fun isBlueValid(number: Int): Boolean {
        return number !in excludeBlues && (mustIncludeBlue == null || number == mustIncludeBlue)
    }

    fun isGroupValid(reds: List<Int>, blue: Int): Boolean {
        if (!mustIncludeReds.all { it in reds }) return false
        if (blue !in 1..16 || !isBlueValid(blue)) return false
        val sum = reds.sum()
        if (sum < minSum || sum > maxSum) return false
        val span = reds.max() - reds.min()
        if (span < minSpan || span > maxSpan) return false
        return true
    }
}
