package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestInterval
import com.ascend.core.model.QuestIntervalStatus
import javax.inject.Inject

/**
 * Derives interval + daily progress. The daily objective is authoritative: daily
 * progress is the sum of interval progress, and the objective can complete even
 * before every interval's window has passed.
 */
class QuestIntervalProgressCalculator
    @Inject
    constructor() {
        fun statusFor(
            current: Double,
            target: Double,
            now: Long,
            scheduledEnd: Long,
        ): QuestIntervalStatus =
            when {
                current >= target && target > 0 -> QuestIntervalStatus.COMPLETED
                now > scheduledEnd && current > 0 -> QuestIntervalStatus.PARTIAL
                now > scheduledEnd -> QuestIntervalStatus.MISSED
                current > 0 -> QuestIntervalStatus.IN_PROGRESS
                else -> QuestIntervalStatus.PENDING
            }

        fun dailyProgress(intervals: List<QuestInterval>): Double = intervals.sumOf { it.currentValue }

        fun dailyRemaining(
            dailyTarget: Double,
            intervals: List<QuestInterval>,
        ): Double = (dailyTarget - dailyProgress(intervals)).coerceAtLeast(0.0)

        /** Remaining in the current interval, and whether the daily target is already met. */
        fun dailyMet(
            dailyTarget: Double,
            intervals: List<QuestInterval>,
        ): Boolean = dailyProgress(intervals) >= dailyTarget
    }
