package com.ascend.core.domain.progression

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Configurable XP weights. Defaults follow the spec's initial formula so tuning
 * never requires touching call sites.
 */
data class XpConfig(
    val workoutBase: Long = 100,
    val perMinuteCap: Long = 90,
    val intensityMin: Long = 10,
    val intensityMax: Long = 50,
    val volumeMax: Long = 100,
    val consistencyBonus: Long = 25,
    val personalRecordBonus: Long = 50,
    // Over-completion: at most [overCompletionCapFraction] of the target counts,
    // scaled by [overCompletionRate]; keeps the bonus small and safe.
    val overCompletionCapFraction: Double = 0.5,
    val overCompletionRate: Double = 0.2,
)

data class QuestXpResult(
    val total: Long,
    val base: Long,
    val overCompletionBonus: Long,
)

/**
 * Pure XP math. No persistence, no Android. [intensity] and [volumeScore] are
 * normalised inputs in 0f..1f so the caller decides how to derive them.
 */
class XpCalculator(private val config: XpConfig = XpConfig()) {
    fun workoutXp(
        durationMinutes: Int,
        intensity: Float = 0f,
        volumeScore: Float = 0f,
        isPersonalRecord: Boolean = false,
        consistencyEligible: Boolean = false,
        expeditionMultiplier: Double = 1.0,
    ): Long {
        require(durationMinutes >= 0) { "durationMinutes must be >= 0" }
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        val clampedVolume = volumeScore.coerceIn(0f, 1f)

        val duration = min(durationMinutes.toLong(), config.perMinuteCap)
        val intensityXp =
            config.intensityMin +
                ((config.intensityMax - config.intensityMin) * clampedIntensity).roundToLong()
        val volumeXp = (config.volumeMax * clampedVolume).roundToLong()
        val consistencyXp = if (consistencyEligible) config.consistencyBonus else 0
        val prXp = if (isPersonalRecord) config.personalRecordBonus else 0

        val raw = config.workoutBase + duration + intensityXp + volumeXp + consistencyXp + prXp
        return (raw * expeditionMultiplier).roundToLong()
    }

    /**
     * Reward for a quest.
     * @param completionFraction achieved / target, expected in 0.0..1.0+ (capped for base).
     * @param overCompletionFraction extra beyond target / target (>= 0).
     */
    fun questXp(
        baseReward: Long,
        completionFraction: Double,
        overCompletionFraction: Double = 0.0,
        partialEnabled: Boolean = true,
        overCompletionEnabled: Boolean = true,
    ): QuestXpResult {
        require(baseReward >= 0) { "baseReward must be >= 0" }
        val completed = completionFraction >= 1.0

        val base: Long =
            when {
                completed -> baseReward
                partialEnabled -> (baseReward * completionFraction.coerceIn(0.0, 1.0)).roundToLong()
                else -> 0
            }

        val bonus: Long =
            if (completed && overCompletionEnabled && overCompletionFraction > 0.0) {
                val countedOver = min(overCompletionFraction, config.overCompletionCapFraction)
                max(0L, (baseReward * countedOver * config.overCompletionRate).roundToLong())
            } else {
                0
            }

        return QuestXpResult(total = base + bonus, base = base, overCompletionBonus = bonus)
    }
}
