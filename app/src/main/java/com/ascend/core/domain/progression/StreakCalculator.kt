package com.ascend.core.domain.progression

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakResult(
    val activeStreak: Int,
    val longestStreak: Int,
    val incremented: Boolean,
    val reset: Boolean,
)

/**
 * Consistency streak logic. A missed day resets the ACTIVE streak but never
 * removes historical XP or the longest-streak record — missing training is not
 * treated as failure (spec: "Do not permanently punish users").
 */
class StreakCalculator {

    fun onActivity(
        currentStreak: Int,
        longestStreak: Int,
        lastActiveDay: LocalDate?,
        today: LocalDate,
    ): StreakResult {
        val safeCurrent = currentStreak.coerceAtLeast(0)
        val safeLongest = longestStreak.coerceAtLeast(safeCurrent)

        if (lastActiveDay == null) {
            return StreakResult(
                activeStreak = 1,
                longestStreak = maxOf(safeLongest, 1),
                incremented = true,
                reset = false,
            )
        }

        val gap = ChronoUnit.DAYS.between(lastActiveDay, today)
        return when {
            gap <= 0L -> // same day (or clock skew): no change
                StreakResult(safeCurrent.coerceAtLeast(1), safeLongest, incremented = false, reset = false)
            gap == 1L -> {
                val next = safeCurrent + 1
                StreakResult(next, maxOf(safeLongest, next), incremented = true, reset = false)
            }
            else -> // returning after one or more missed days
                StreakResult(1, safeLongest, incremented = false, reset = true)
        }
    }
}
