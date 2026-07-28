package com.ascend.core.domain.training

import com.ascend.core.model.QuestOutcome
import com.ascend.core.model.RecentBaseline
import javax.inject.Inject

/**
 * Builds a rolling [RecentBaseline] from recent Daily Quest outcomes. Uses the median
 * target so a single outlier can't dominate, and reports consistency + a trend so the
 * quest progression engine can decide to raise, hold, or reduce — never from one
 * result alone. This finally powers the `recentBaseline` hook in `QuestTargetValidator`.
 */
class RecentBaselineCalculator
    @Inject
    constructor() {
        fun calculate(outcomes: List<QuestOutcome>): RecentBaseline? {
            if (outcomes.isEmpty()) return null

            val representativeTarget = median(outcomes.map { it.target })
            val avgCompleted = outcomes.map { it.completed }.average()
            val avgPercentage = outcomes.map { it.completionPercentage }.average()
            val consistency = outcomes.count { it.completionPercentage >= 1.0 }.toDouble() / outcomes.size

            // Newest-first: trend compares the most recent outcome to the oldest in the window.
            val newest = outcomes.first().completed
            val oldest = outcomes.last().completed
            val trend = if (oldest <= 0) 0.0 else (newest - oldest).toDouble() / oldest

            val confidence = (outcomes.size.toDouble() / CONFIDENT_SAMPLE).coerceIn(0.0, 1.0)

            return RecentBaseline(
                representativeTarget = representativeTarget,
                averageCompletedAmount = avgCompleted,
                averageCompletionPercentage = avgPercentage,
                completionConsistency = consistency,
                averagePerceivedEffort = null,
                recentTrend = trend,
                confidence = confidence,
                sampleCount = outcomes.size,
            )
        }

        private fun median(values: List<Int>): Int {
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        }

        private companion object {
            const val CONFIDENT_SAMPLE = 3.0
        }
    }
