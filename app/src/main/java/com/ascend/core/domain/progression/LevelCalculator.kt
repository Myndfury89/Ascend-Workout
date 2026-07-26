package com.ascend.core.domain.progression

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure, configurable level curve. All progression formulas live in the domain
 * layer (never scattered through UI). Default curve per spec:
 *
 *   XP required to advance FROM [level] to [level] + 1  =  round(200 * level^exponent)
 *
 * A player starts at level 1 with 0 lifetime XP.
 */
class LevelCalculator(
    private val base: Double = 200.0,
    private val exponent: Double = 1.35,
    private val maxLevel: Int = 999,
) {
    /** XP needed to go from [level] to the next level. Level must be >= 1. */
    fun xpToReachNextLevel(level: Int): Long {
        require(level >= 1) { "level must be >= 1, was $level" }
        return (base * level.toDouble().pow(exponent)).roundToLong()
    }

    /** Total lifetime XP required to first reach [level] (level 1 == 0). */
    fun cumulativeXpForLevel(level: Int): Long {
        require(level >= 1) { "level must be >= 1, was $level" }
        var total = 0L
        for (l in 1 until level) total += xpToReachNextLevel(l)
        return total
    }

    /** Resolve a lifetime XP total into a concrete level + within-level progress. */
    fun resolve(lifetimeXp: Long): LevelState {
        require(lifetimeXp >= 0) { "lifetimeXp must be >= 0, was $lifetimeXp" }
        var level = 1
        var consumed = 0L
        while (level < maxLevel) {
            val needed = xpToReachNextLevel(level)
            if (consumed + needed > lifetimeXp) break
            consumed += needed
            level++
        }
        val xpIntoLevel = lifetimeXp - consumed
        val xpForNext = if (level < maxLevel) xpToReachNextLevel(level) else 0L
        return LevelState(
            level = level,
            currentLevelXp = xpIntoLevel,
            xpToNextLevel = xpForNext,
            lifetimeXp = lifetimeXp,
        )
    }
}

data class LevelState(
    val level: Int,
    val currentLevelXp: Long,
    val xpToNextLevel: Long,
    val lifetimeXp: Long,
) {
    /** Progress toward the next level in the range 0f..1f (1f at max level). */
    val progressFraction: Float
        get() = if (xpToNextLevel <= 0L) 1f else (currentLevelXp.toFloat() / xpToNextLevel.toFloat())
}
