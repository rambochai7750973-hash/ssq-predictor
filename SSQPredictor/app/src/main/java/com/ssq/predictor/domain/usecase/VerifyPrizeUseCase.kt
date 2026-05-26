package com.ssq.predictor.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

data class PrizeResult(
    val redMatches: Int,
    val blueMatch: Boolean,
    val prizeLevel: PrizeLevel?
)

enum class PrizeLevel(val label: String) {
    FIRST("一等奖 (6+1)"),
    SECOND("二等奖 (6+0)"),
    THIRD("三等奖 (5+1)"),
    FOURTH("四等奖 (5+0 或 4+1)"),
    FIFTH("五等奖 (4+0 或 3+1)"),
    SIXTH("六等奖 (2+1 或 1+1 或 0+1)")
}

@Singleton
class VerifyPrizeUseCase @Inject constructor() {

    operator fun invoke(
        userReds: Set<Int>,
        userBlue: Int,
        winningReds: Set<Int>,
        winningBlue: Int
    ): PrizeResult {
        val redMatches = userReds.intersect(winningReds).size
        val blueMatch = userBlue == winningBlue

        val level = when {
            redMatches == 6 && blueMatch -> PrizeLevel.FIRST
            redMatches == 6 -> PrizeLevel.SECOND
            redMatches == 5 && blueMatch -> PrizeLevel.THIRD
            redMatches == 5 || (redMatches == 4 && blueMatch) -> PrizeLevel.FOURTH
            redMatches == 4 || (redMatches == 3 && blueMatch) -> PrizeLevel.FIFTH
            blueMatch -> PrizeLevel.SIXTH
            else -> null
        }

        return PrizeResult(
            redMatches = redMatches,
            blueMatch = blueMatch,
            prizeLevel = level
        )
    }
}
