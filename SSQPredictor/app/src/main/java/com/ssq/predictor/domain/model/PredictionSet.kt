package com.ssq.predictor.domain.model

data class PredictionSet(
    val algorithmName: String,
    val groups: List<PredictionGroup>
)

data class PredictionGroup(
    val reds: List<Int>,
    val blue: Int,
    val score: Int,
    val reasons: List<BallReason>
)

data class BallReason(
    val number: Int,
    val reason: String,
    val type: ReasonType
)

enum class ReasonType(val label: String) {
    HOT("热号"),
    COLD("冷号"),
    OMISSION("遗漏回补"),
    MARKOV("马尔可夫"),
    DISTRIBUTION("分布均衡"),
    RANDOM("随机"),
    ENSEMBLE("综合"),
    BIG_DATA("大数据")
}
