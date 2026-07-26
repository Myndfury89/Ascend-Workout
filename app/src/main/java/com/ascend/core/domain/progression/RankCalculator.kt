package com.ascend.core.domain.progression

import com.ascend.core.model.Rank

/**
 * Resolves a [Rank] from a configurable blend of level, lifetime XP, and
 * consistency — deliberately NOT from weight lifted, so different goals and
 * abilities are treated fairly.
 *
 * Each threshold is the minimum "ascension score" required. The score is:
 *   level  +  (lifetimeXp / xpPerScorePoint)  +  (consistencyDays * consistencyWeight)
 */
data class RankThreshold(val minScore: Double, val rank: Rank)

class RankCalculator(
    private val xpPerScorePoint: Double = 1_000.0,
    private val consistencyWeight: Double = 0.5,
    private val thresholds: List<RankThreshold> = DEFAULT_THRESHOLDS,
) {
    init {
        require(thresholds.isNotEmpty()) { "thresholds must not be empty" }
    }

    fun ascensionScore(level: Int, lifetimeXp: Long, consistencyDays: Int = 0): Double =
        level.toDouble() +
            (lifetimeXp.coerceAtLeast(0) / xpPerScorePoint) +
            (consistencyDays.coerceAtLeast(0) * consistencyWeight)

    fun rankFor(level: Int, lifetimeXp: Long, consistencyDays: Int = 0): Rank {
        val score = ascensionScore(level, lifetimeXp, consistencyDays)
        return thresholds
            .sortedByDescending { it.minScore }
            .firstOrNull { score >= it.minScore }
            ?.rank
            ?: thresholds.minByOrNull { it.minScore }!!.rank
    }

    companion object {
        // Level-forward defaults; lifetime XP and consistency nudge borderline cases up.
        val DEFAULT_THRESHOLDS: List<RankThreshold> = listOf(
            RankThreshold(0.0, Rank.INITIATE),
            RankThreshold(5.0, Rank.IRON),
            RankThreshold(10.0, Rank.BRONZE),
            RankThreshold(18.0, Rank.SILVER),
            RankThreshold(28.0, Rank.GOLD),
            RankThreshold(42.0, Rank.VANGUARD),
            RankThreshold(60.0, Rank.ASCENDANT),
            RankThreshold(85.0, Rank.APEX),
            RankThreshold(120.0, Rank.MYTHIC),
        )
    }
}
