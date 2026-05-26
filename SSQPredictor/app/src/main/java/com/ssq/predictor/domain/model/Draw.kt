package com.ssq.predictor.domain.model

data class Draw(
    val period: String,
    val date: String,
    val reds: List<Int>,
    val blue: Int
) {
    init {
        require(reds.size == 6) { "必须恰好6个红球" }
        require(reds.distinct().size == 6) { "红球不能重复" }
        require(blue in 1..16) { "蓝球必须在1~16之间" }
    }

    val redSet: Set<Int> get() = reds.toSet()

    val sum: Int get() = reds.sum() + blue

    val oddCount: Int get() = reds.count { it % 2 == 1 }

    val evenCount: Int get() = 6 - oddCount

    val span: Int get() = reds.max() - reds.min()

    val region1: Int get() = reds.count { it in 1..11 }
    val region2: Int get() = reds.count { it in 12..22 }
    val region3: Int get() = reds.count { it in 23..33 }
}
