package com.ascend.core.domain.progression

import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import kotlin.math.roundToLong

data class AttributeConfig(
    // Points per unit of primary-attribute volume (e.g. per repetition/second/metre).
    val volumeRate: Double = 0.15,
    val volumeCap: Long = 200,
    // Flat discipline granted for completing a planned quest/workout.
    val disciplineBase: Long = 10,
    val difficultyMultipliers: Map<Difficulty, Double> = mapOf(
        Difficulty.EASY to 0.8,
        Difficulty.MODERATE to 1.0,
        Difficulty.HARD to 1.3,
        Difficulty.EXTREME to 1.6,
    ),
)

/**
 * Maps completed activity to attribute deltas. Completing a planned quest always
 * grants Discipline (adherence); the exercise's primary attribute grows with
 * volume. Pure and configurable.
 */
class AttributeProgressCalculator(private val config: AttributeConfig = AttributeConfig()) {

    fun difficultyMultiplier(difficulty: Difficulty): Double =
        config.difficultyMultipliers[difficulty] ?: 1.0

    /** Primary-attribute points earned from accumulated volume (reps/seconds/metres). */
    fun volumePoints(volume: Double, difficulty: Difficulty = Difficulty.MODERATE): Long {
        require(volume >= 0) { "volume must be >= 0" }
        return (volume * config.volumeRate * difficultyMultiplier(difficulty))
            .roundToLong()
            .coerceIn(0, config.volumeCap)
    }

    /** Flat Discipline granted once for completing a planned quest/workout. */
    fun disciplinePoints(difficulty: Difficulty = Difficulty.MODERATE): Long =
        (config.disciplineBase * difficultyMultiplier(difficulty)).roundToLong()

    /**
     * @param primaryAttribute the attribute the exercise trains (e.g. push-ups -> STRENGTH).
     * @param volume total accumulated value (reps, seconds, metres, ...).
     */
    fun forQuestCompletion(
        primaryAttribute: AttributeType,
        volume: Double,
        difficulty: Difficulty = Difficulty.MODERATE,
    ): Map<AttributeType, Long> {
        require(volume >= 0) { "volume must be >= 0" }
        val mult = difficultyMultiplier(difficulty)

        val primaryPoints = (volume * config.volumeRate * mult)
            .roundToLong()
            .coerceIn(0, config.volumeCap)
        val disciplinePoints = (config.disciplineBase * mult).roundToLong()

        // Merge so a quest whose primary attribute IS discipline still accumulates once.
        val result = linkedMapOf<AttributeType, Long>()
        if (primaryPoints > 0) result[primaryAttribute] = primaryPoints
        result[AttributeType.DISCIPLINE] =
            (result[AttributeType.DISCIPLINE] ?: 0L) + disciplinePoints
        return result
    }
}
